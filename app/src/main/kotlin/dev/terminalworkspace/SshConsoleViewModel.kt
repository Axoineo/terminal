package dev.terminalworkspace

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.terminalworkspace.core.data.remote.HostKeyPrompt
import dev.terminalworkspace.core.data.remote.SshClient
import dev.terminalworkspace.core.data.remote.SshConnectionConfig
import dev.terminalworkspace.core.data.remote.SshConnectionState
import dev.terminalworkspace.core.domain.ReceivedOutputBuffer
import dev.terminalworkspace.core.domain.SessionHistoryRepository
import dev.terminalworkspace.core.model.SavedSessionOutput
import dev.terminalworkspace.core.model.SavedSessionState
import dev.terminalworkspace.terminal.TerminalSessionState
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Owns the live session across rotation. No credentials or live terminal are saved into a Bundle. */
class SshConsoleViewModel(
    private val history: SessionHistoryRepository,
    private val knownHostsFile: File,
) : ViewModel() {
    var host by mutableStateOf("")
    var port by mutableStateOf("22")
    var username by mutableStateOf("")
    val password = TextFieldState()
    var recordOutput by mutableStateOf(false)
    var localNetwork by mutableStateOf(false)
    var busy by mutableStateOf(false)
        private set
    var connected by mutableStateOf(false)
        private set
    var message by mutableStateOf("Ready to connect")
        private set
    var trustPrompt by mutableStateOf<HostKeyPrompt?>(null)
        private set
    var selectedHistory by mutableStateOf<SavedSessionOutput?>(null)
        private set
    var recordingMessage by mutableStateOf("")
        private set

    val savedSessions = history.observeRecent().catch {
        message = "Session history could not be read."
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private data class Attempt(val id: String, val client: SshClient, var historyId: String?,
        val capture: ReceivedOutputBuffer = ReceivedOutputBuffer(), var savedRevision: Long = -1)
    private data class Input(val id: String, val bytes: ByteArray)
    private data class Size(val cols: Int, val rows: Int, val width: Int, val height: Int)
    private val active = AtomicReference<Attempt?>()
    private val inputs = Channel<Input>(32)
    private val sizes = Channel<Size>(Channel.CONFLATED)
    private val latestSize = AtomicReference(Size(80, 24, 0, 0))
    private val saves = Mutex()
    private var connectionJob: Job? = null
    private var outputJob: Job? = null
    private var stateJob: Job? = null
    private var flushJob: Job? = null
    private var approval: CompletableDeferred<Boolean>? = null

    val terminal = TerminalSessionState(
        onInput = { bytes ->
            val attempt = active.get()
            if (attempt != null && attempt.client.state.value == SshConnectionState.Connected) {
                if (bytes.size <= 65_536 && inputs.trySend(Input(attempt.id, bytes)).isFailure) {
                    viewModelScope.launch { message = "Input queue is full. Check the connection before typing again." }
                }
            }
        },
        onResize = { cols, rows, width, height ->
            if (cols in 1..1000 && rows in 1..1000) {
                val size = Size(cols, rows, width.coerceAtLeast(0), height.coerceAtLeast(0))
                latestSize.set(size)
                sizes.trySend(size)
            }
        },
    )

    init {
        viewModelScope.launch {
            for (input in inputs) {
                val attempt = active.get()
                try {
                    if (attempt?.id == input.id) attempt.client.send(input.bytes)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    message = "Input could not be sent. Remote command outcomes remain unknown."
                } finally { input.bytes.fill(0) }
            }
        }
        viewModelScope.launch {
            for (size in sizes) {
                val attempt = active.get() ?: continue
                if (attempt.client.state.value == SshConnectionState.Connected) {
                    runCatching { attempt.client.resize(size.cols, size.rows, size.width, size.height) }
                }
            }
        }
    }

    fun connect() {
        if (busy) return
        val secret = password.text.toString().toByteArray(Charsets.UTF_8)
        password.clearText()
        val config = try { SshConnectionConfig(host.trim(), port.toInt(), username.trim(), secret) }
        catch (_: IllegalArgumentException) {
            secret.fill(0)
            message = "Enter a hostname (without URL or brackets), port 1–65535 and username."
            return
        }
        busy = true
        connected = false
        message = "Connecting…"
        connectionJob = viewModelScope.launch {
            try {
                stopPrevious()
                terminal.reset()
                val id = UUID.randomUUID().toString()
                val attempt = Attempt(id, SshClient(knownHostsFile), if (recordOutput) id else null)
                active.set(attempt)
                if (attempt.historyId != null) {
                    history.create(id, config.host, config.port, config.username, now())
                    recordingMessage = "Recording received output locally. The latest second may be unsaved."
                } else recordingMessage = "History recording is off."
                outputJob = viewModelScope.launch {
                    attempt.client.output.collect { bytes ->
                        if (active.get() === attempt) {
                            if (attempt.historyId != null) attempt.capture.append(bytes)
                            terminal.writeReceived(bytes)
                        }
                    }
                }
                stateJob = viewModelScope.launch {
                    attempt.client.state.collect { state ->
                        if (active.get() !== attempt) return@collect
                        connected = state == SshConnectionState.Connected
                        when (state) {
                            SshConnectionState.Connecting -> message = "Connecting…"
                            SshConnectionState.Connected -> {
                                message = "Connected · ${config.username}@${config.host}:${config.port}"
                                sizes.trySend(latestSize.get())
                                setHistoryState(attempt, SavedSessionState.CONNECTED)
                            }
                            is SshConnectionState.Failed -> {
                                message = state.message
                                busy = false
                                flush(attempt)
                                setHistoryState(attempt, SavedSessionState.INTERRUPTED)
                            }
                            SshConnectionState.Disconnected -> if (!attempt.clientConnecting()) {
                                // Initial Disconnected is ignored; final status is handled below/on disconnect.
                                if (message.startsWith("Connected")) {
                                    busy = false
                                    message = "Disconnected. Remote command outcomes remain unknown."
                                    flush(attempt)
                                    setHistoryState(attempt, SavedSessionState.DISCONNECTED)
                                }
                            }
                        }
                    }
                }
                flushJob = viewModelScope.launch {
                    while (true) { delay(1_000); flush(attempt) }
                }
                attempt.client.connect(config) { prompt ->
                    withContext(Dispatchers.Main.immediate) {
                        val response = CompletableDeferred<Boolean>()
                        approval = response
                        trustPrompt = prompt
                        try { response.await() }
                        finally { if (approval === response) { approval = null; trustPrompt = null } }
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                busy = false
                connected = false
                message = if (error is dev.terminalworkspace.core.data.remote.SshConnectionException)
                    error.message ?: "Connection failed." else "Connection could not be started or saved."
                active.get()?.let { attempt ->
                    attempt.client.close()
                    flush(attempt)
                    setHistoryState(attempt, SavedSessionState.FAILED_TO_CONNECT)
                }
            } finally { secret.fill(0) }
        }
    }

    // Called from a state collector; the live client's state itself is the authority.
    private fun Attempt.clientConnecting() = client.state.value == SshConnectionState.Connecting

    fun approveHost(accept: Boolean) { approval?.complete(accept) }

    fun disconnect() {
        if (!busy && active.get() == null) return
        viewModelScope.launch {
            connectionJob?.cancelAndJoin()
            stopPrevious()
            busy = false
            connected = false
            message = "Disconnected. Remote command outcomes remain unknown."
        }
    }

    private suspend fun stopPrevious() {
        approval?.complete(false)
        trustPrompt = null
        val previous = active.getAndSet(null)
        withContext(Dispatchers.IO) { previous?.client?.close() }
        outputJob?.cancelAndJoin()
        stateJob?.cancelAndJoin()
        flushJob?.cancelAndJoin()
        if (previous != null) {
            flush(previous)
            setHistoryState(previous, SavedSessionState.DISCONNECTED)
        }
    }

    private suspend fun flush(attempt: Attempt) = saves.withLock {
        val id = attempt.historyId ?: return@withLock
        if (attempt.capture.revision == attempt.savedRevision) return@withLock
        val revision = attempt.capture.revision
        val snapshot = attempt.capture.snapshot()
        try {
            history.saveReceived(id, snapshot, attempt.capture.truncated, now())
            attempt.savedRevision = revision
            if (active.get() === attempt && attempt.capture.truncated)
                recordingMessage = "History reached 256 KiB. Only the beginning of this session is saved."
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            recordingMessage = "History could not be saved. Live output may not be recoverable."
        }
    }

    private suspend fun setHistoryState(attempt: Attempt, state: SavedSessionState) {
        val id = attempt.historyId ?: return
        try { history.markState(id, state, now()) }
        catch (error: Exception) {
            if (error is CancellationException) throw error
            recordingMessage = "Session status could not be saved."
        }
    }

    fun openHistory(id: String) { viewModelScope.launch {
        try { selectedHistory = history.read(id) }
        catch (_: Exception) { message = "Saved output could not be read." }
    } }
    fun closeHistory() { selectedHistory = null }
    fun deleteHistory(id: String) { viewModelScope.launch {
        saves.withLock {
            active.get()?.takeIf { it.historyId == id }?.let {
                it.historyId = null
                recordingMessage = "Recording stopped and saved output deleted."
            }
            try { history.delete(id); if (selectedHistory?.session?.id == id) selectedHistory = null }
            catch (_: Exception) { message = "Saved output could not be deleted." }
        }
    } }
    fun permissionDenied() { password.clearText(); message = "Local network permission was denied." }

    override fun onCleared() {
        approval?.complete(false)
        active.getAndSet(null)?.client?.close()
        inputs.close()
        sizes.close()
        password.clearText()
        terminal.close()
    }

    private fun now() = System.currentTimeMillis().coerceAtLeast(0)
}

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
    var connectionAttemptId by mutableStateOf(0L)
        private set

    val savedSessions = history.observeRecent().catch {
        recordingMessage = "Session history could not be read."
        emit(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private data class Attempt(val id: String, val generation: Long, val client: SshClient, var historyId: String?,
        val capture: ReceivedOutputBuffer = ReceivedOutputBuffer(), var savedRevision: Long = -1,
        var everConnected: Boolean = false, var connectFinished: Boolean = false,
        var terminalState: SavedSessionState? = null)
    private data class Input(val id: String, val bytes: ByteArray)
    private data class Size(val cols: Int, val rows: Int, val width: Int, val height: Int)
    private val active = AtomicReference<Attempt?>()
    private val inputs = Channel<Input>(32)
    private val sizes = Channel<Size>(Channel.CONFLATED)
    private val latestSize = AtomicReference(Size(80, 24, 0, 0))
    private val saves = Mutex()
    private var connectionJob: Job? = null
    private var disconnectJob: Job? = null
    private var outputJob: Job? = null
    private var stateJob: Job? = null
    private var flushJob: Job? = null
    private var approval: CompletableDeferred<Boolean>? = null
    private var historySelectionRequest = 0L
    @Volatile private var lifecycleGeneration = 0L

    val terminal = TerminalSessionState(
        onInput = { bytes ->
            val attempt = active.get()
            if (attempt != null && isCurrent(attempt) && attempt.client.state.value == SshConnectionState.Connected) {
                if (bytes.size > 65_536 || inputs.trySend(Input(attempt.id, bytes)).isFailure) {
                    bytes.fill(0)
                    viewModelScope.launch {
                        if (isCurrent(attempt)) message = "Input could not be queued. Check the connection before typing again."
                    }
                }
            } else bytes.fill(0)
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
                    if (attempt != null && attempt.id == input.id && isCurrent(attempt)) attempt.client.send(input.bytes)
                } catch (error: Exception) {
                    // A canceled/timed-out send must not permanently kill the reusable input loop.
                    if (error is CancellationException) currentCoroutineContext().ensureActive()
                    if (attempt != null && isCurrent(attempt))
                        message = "Input could not be sent. Remote command outcomes remain unknown."
                } finally { input.bytes.fill(0) }
            }
        }
        viewModelScope.launch {
            for (size in sizes) {
                val attempt = active.get() ?: continue
                if (isCurrent(attempt) && attempt.client.state.value == SshConnectionState.Connected) {
                    try { attempt.client.resize(size.cols, size.rows, size.width, size.height) }
                    catch (error: Exception) {
                        if (error is CancellationException) currentCoroutineContext().ensureActive()
                    }
                }
            }
        }
    }

    fun connect() {
        if (busy) return
        val secret = password.text.toString().toByteArray(Charsets.UTF_8)
        password.clearText()
        val config = try {
            // Match the persisted session metadata limit regardless of the recording toggle.
            require(username.trim().length <= 128)
            SshConnectionConfig(host.trim(), port.toInt(), username.trim(), secret)
        }
        catch (_: IllegalArgumentException) {
            secret.fill(0)
            message = "Enter a hostname (without URL or brackets), port 1–65535 and username."
            return
        }
        val generation = ++lifecycleGeneration
        connectionAttemptId++
        val shouldRecord = recordOutput
        val previousConnectionJob = connectionJob
        busy = true
        connected = false
        message = "Connecting…"
        connectionJob = viewModelScope.launch {
            var ownedAttempt: Attempt? = null
            try {
                previousConnectionJob?.cancelAndJoin()
                stopPrevious()
                if (lifecycleGeneration != generation) return@launch
                terminal.reset()
                val id = UUID.randomUUID().toString()
                val attempt = Attempt(id, generation, SshClient(knownHostsFile), if (shouldRecord) id else null)
                ownedAttempt = attempt
                active.set(attempt)
                if (attempt.historyId != null) {
                    history.create(id, config.host, config.port, config.username, now())
                    recordingMessage = "Recording received output locally. The latest second may be unsaved."
                } else recordingMessage = "History recording is off."
                outputJob = viewModelScope.launch {
                    try {
                        attempt.client.output.collect { bytes ->
                            if (active.get() === attempt) {
                                if (attempt.historyId != null) attempt.capture.append(bytes)
                                terminal.writeReceived(bytes)
                            }
                        }
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        if (isCurrent(attempt)) {
                            connected = false
                            message = "Terminal output could not be displayed. Remote command outcomes remain unknown."
                            setHistoryState(attempt, SavedSessionState.INTERRUPTED)
                            withContext(Dispatchers.IO) { attempt.client.close() }
                            flush(attempt)
                            if (isCurrent(attempt)) busy = false
                        }
                    }
                }
                stateJob = viewModelScope.launch {
                    attempt.client.state.collect { state ->
                        if (!isCurrent(attempt)) return@collect
                        connected = state == SshConnectionState.Connected
                        when (state) {
                            SshConnectionState.Connecting -> message = "Connecting…"
                            SshConnectionState.Connected -> {
                                attempt.everConnected = true
                                message = "Connected · ${config.username}@${config.host}:${config.port}"
                                sizes.trySend(latestSize.get())
                                setHistoryState(attempt, SavedSessionState.CONNECTED)
                            }
                            is SshConnectionState.Failed -> {
                                message = state.message
                                flush(attempt)
                                setHistoryState(attempt, if (attempt.everConnected) SavedSessionState.INTERRUPTED
                                    else SavedSessionState.FAILED_TO_CONNECT)
                                if (isCurrent(attempt)) busy = false
                            }
                            SshConnectionState.Disconnected -> if (attempt.connectFinished && attempt.terminalState == null) {
                                message = "Disconnected. Remote command outcomes remain unknown."
                                flush(attempt)
                                setHistoryState(attempt, SavedSessionState.DISCONNECTED)
                                if (isCurrent(attempt)) busy = false
                            }
                        }
                    }
                }
                flushJob = viewModelScope.launch {
                    while (true) { delay(1_000); flush(attempt) }
                }
                attempt.client.connect(config) { prompt ->
                    withContext(Dispatchers.Main.immediate) {
                        if (!isCurrent(attempt)) return@withContext false
                        val response = CompletableDeferred<Boolean>()
                        approval = response
                        trustPrompt = prompt
                        try { response.await() }
                        finally { if (approval === response) { approval = null; trustPrompt = null } }
                    }
                }
                attempt.everConnected = true
                attempt.connectFinished = true
                // StateFlow may conflate a short Connected -> EOF transition before its collector runs.
                if (isCurrent(attempt) && attempt.client.state.value == SshConnectionState.Disconnected &&
                    attempt.terminalState == null
                ) {
                    connected = false
                    message = "Disconnected. Remote command outcomes remain unknown."
                    flush(attempt)
                    setHistoryState(attempt, SavedSessionState.DISCONNECTED)
                    if (isCurrent(attempt)) busy = false
                }
            } catch (error: Exception) {
                // Internal transport/approval timeouts leave this owner active; explicit lifecycle
                // cancellation does not. Only the latter skips the ordinary failure cleanup.
                if (error is CancellationException) currentCoroutineContext().ensureActive()
                val attempt = ownedAttempt
                if (lifecycleGeneration == generation) {
                    connected = false
                    message = when (error) {
                        is dev.terminalworkspace.core.data.remote.SshConnectionException -> error.message ?: "Connection failed."
                        is CancellationException -> "The connection timed out or was canceled. Try again."
                        else -> "Connection could not be started or saved."
                    }
                }
                attempt?.let {
                    withContext(Dispatchers.IO) { attempt.client.close() }
                    flush(attempt)
                    setHistoryState(attempt, if (attempt.everConnected) SavedSessionState.INTERRUPTED
                        else SavedSessionState.FAILED_TO_CONNECT)
                }
                if (lifecycleGeneration == generation) busy = false
            } finally { secret.fill(0) }
        }
    }

    private fun isCurrent(attempt: Attempt) = active.get() === attempt && attempt.generation == lifecycleGeneration

    fun approveHost(accept: Boolean) { approval?.complete(accept) }

    fun disconnect() {
        if (disconnectJob?.isActive == true) return
        if (!busy && active.get() == null) return
        val generation = ++lifecycleGeneration
        val previousConnectionJob = connectionJob
        busy = true
        connected = false
        message = "Disconnecting…"
        disconnectJob = viewModelScope.launch {
            previousConnectionJob?.cancelAndJoin()
            stopPrevious()
            if (lifecycleGeneration == generation) {
                busy = false
                message = "Disconnected. Remote command outcomes remain unknown."
            }
        }
    }

    private suspend fun stopPrevious() {
        approval?.complete(false)
        trustPrompt = null
        val previous = active.get()
        stateJob?.cancelAndJoin()
        flushJob?.cancelAndJoin()
        withContext(Dispatchers.IO) { previous?.client?.close() }
        // Closing a one-shot client completes its channel after the bounded received-byte backlog.
        // Let the old collector finish before resetting/relabeling the terminal and capture.
        if (withTimeoutOrNull(2_000) { outputJob?.join(); true } != true) outputJob?.cancelAndJoin()
        active.compareAndSet(previous, null)
        if (previous != null) {
            flush(previous)
            setHistoryState(previous, if (previous.everConnected) SavedSessionState.DISCONNECTED
                else SavedSessionState.FAILED_TO_CONNECT)
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
            if (isCurrent(attempt) && attempt.capture.truncated)
                recordingMessage = "History reached 256 KiB. Only the beginning of this session is saved."
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            if (isCurrent(attempt)) recordingMessage = "History could not be saved. Live output may not be recoverable."
        }
    }

    private suspend fun setHistoryState(attempt: Attempt, state: SavedSessionState) = saves.withLock {
        val resolved = if (state == SavedSessionState.CONNECTED) {
            if (attempt.terminalState != null) return@withLock
            state
        } else {
            attempt.terminalState ?: state.also { attempt.terminalState = it }
        }
        val id = attempt.historyId ?: return@withLock
        try { history.markState(id, resolved, now()) }
        catch (error: Exception) {
            if (error is CancellationException) throw error
            if (isCurrent(attempt)) recordingMessage = "Session status could not be saved."
        }
    }

    fun openHistory(id: String) {
        val request = ++historySelectionRequest
        viewModelScope.launch {
            try {
                val output = history.read(id)
                if (request == historySelectionRequest) selectedHistory = output
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (request == historySelectionRequest) message = "Saved output could not be read."
            }
        }
    }
    fun closeHistory() { historySelectionRequest++; selectedHistory = null }
    fun deleteHistory(id: String) {
        historySelectionRequest++
        viewModelScope.launch {
            saves.withLock {
                val recordingAttempt = active.get()?.takeIf { it.historyId == id }
                recordingAttempt?.let {
                    it.historyId = null
                    recordingMessage = "Recording stopped. Deleting saved output…"
                }
                try {
                    history.delete(id)
                    if (selectedHistory?.session?.id == id) selectedHistory = null
                    if (recordingAttempt != null && isCurrent(recordingAttempt))
                        recordingMessage = "Recording stopped and saved output deleted."
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    if (recordingAttempt != null && isCurrent(recordingAttempt))
                        recordingMessage = "Recording stopped, but saved output could not be deleted."
                    message = "Saved output could not be deleted."
                }
            }
        }
    }
    fun permissionDenied() { password.clearText(); message = "Local network permission was denied." }

    override fun onCleared() {
        lifecycleGeneration++
        approval?.complete(false)
        val previous = active.getAndSet(null)
        val cleanup = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        cleanup.launch { try { previous?.client?.close() } finally { cleanup.cancel() } }
        inputs.close()
        while (true) (inputs.tryReceive().getOrNull() ?: break).bytes.fill(0)
        sizes.close()
        password.clearText()
        terminal.close()
    }

    private fun now() = System.currentTimeMillis().coerceAtLeast(0)
}

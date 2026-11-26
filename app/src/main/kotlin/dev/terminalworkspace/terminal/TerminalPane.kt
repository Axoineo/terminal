package dev.terminalworkspace.terminal

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import org.connectbot.terminal.InlineImages
import org.connectbot.terminal.Terminal
import org.connectbot.terminal.TerminalEmulator
import org.connectbot.terminal.TerminalEmulatorFactory
import org.connectbot.terminal.VTermKey

/**
 * Own one of these per SSH session in a ViewModel (or a longer-lived session owner), rather than in
 * remember(). Collect incoming bytes in that owner's scope so temporary UI removal does not lose
 * terminal output. Stop/join its producer before resetting or permanently closing this holder.
 * Native screen/history state survives configuration changes through the owner, not process death.
 *
 * Callbacks hand bytes and dimensions to the transport; they must not synchronously feed the
 * emulator again. No network, credentials, parser, or terminal rendering is implemented here.
 */
class TerminalSessionState(
    private val onInput: (ByteArray) -> Unit,
    private val onResize: (columns: Int, rows: Int, widthPixels: Int, heightPixels: Int) -> Unit,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    private val activeGeneration = AtomicLong(0)
    private val lifetimeLock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val retired = mutableSetOf<EngineGeneration>()
    private var displayedEngine by mutableStateOf<EngineGeneration?>(createEngine(0))

    internal val engine: EngineGeneration? get() = displayedEngine

    private fun createEngine(generation: Long, rows: Int = 24, columns: Int = 80): EngineGeneration =
        EngineGeneration(generation, TerminalEmulatorFactory.create(
        initialRows = rows,
        initialCols = columns,
        onKeyboardInput = { bytes ->
            if (!closed.get() && activeGeneration.get() == generation) onInput(bytes.copyOf())
        },
        onResize = { size ->
            if (!closed.get() && activeGeneration.get() == generation) {
                onResize(size.columns, size.rows, size.widthPixels, size.heightPixels)
            }
        },
        // Untrusted remote output cannot silently alter/read the Android clipboard or decode images.
        onClipboardCopy = null,
        inlineImages = InlineImages.Off,
        autoDetectUrls = false,
    ))

    /**
     * Starts a genuinely new upstream engine, including parser, modes, decoder, and history. The
     * old producer must already be stopped/joined. No reset escape sequences are synthesized.
     * Compose detaches the previous generation before its native resources are released.
     */
    suspend fun reset() = withContext(Dispatchers.Main.immediate) {
        synchronized(lifetimeLock) {
            check(!closed.get()) { "Terminal session is closed" }
            val previous = checkNotNull(displayedEngine)
            val dimensions = previous.emulator.dimensions
            val nextId = activeGeneration.get() + 1
            val next = createEngine(nextId, dimensions.rows, dimensions.columns)
            activeGeneration.set(nextId)
            displayedEngine = next
            retire(previous)
        }
    }

    /** Sequential collection preserves transport order and avoids a UI-lifecycle subscription. */
    suspend fun collectReceived(received: Flow<ByteArray>) {
        received.collect { writeReceived(it) }
    }

    /** Feed unchanged transport bytes; the upstream engine owns UTF-8 and escape-sequence state. */
    suspend fun writeReceived(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        withContext(Dispatchers.Default) {
            synchronized(lifetimeLock) {
                if (!closed.get()) displayedEngine?.emulator?.writeInput(bytes)
            }
        }
    }

    /** Only for deliberate user input. The caller reviews multiline clipboard content before paste. */
    fun paste(text: String) {
        synchronized(lifetimeLock) {
            if (!closed.get()) displayedEngine?.emulator?.pasteText(text)
        }
    }

    /** A key event, not a bracketed paste payload. */
    fun sendControlC() {
        synchronized(lifetimeLock) {
            if (!closed.get()) displayedEngine?.emulator?.dispatchCharacter(0, 3)
        }
    }

    /** Let the engine encode Tab as keyboard input instead of bracketed pasted text. */
    fun sendTab() {
        synchronized(lifetimeLock) {
            if (!closed.get()) displayedEngine?.emulator?.dispatchKey(0, VTermKey.TAB)
        }
    }

    override fun close() {
        synchronized(lifetimeLock) {
            if (!closed.compareAndSet(false, true)) return
            activeGeneration.incrementAndGet()
            displayedEngine?.let(::retire)
            displayedEngine = null
            retired.toList().forEach(::scheduleCloseWhenDetached)
        }
    }

    internal fun attach(generation: EngineGeneration) = synchronized(lifetimeLock) {
        check(!generation.disposed) { "Terminal engine is already disposed" }
        generation.attachments++
    }

    internal fun detach(generation: EngineGeneration) = synchronized(lifetimeLock) {
        check(generation.attachments > 0)
        generation.attachments--
        scheduleCloseWhenDetached(generation)
    }

    private fun retire(generation: EngineGeneration) {
        generation.retired = true
        retired.add(generation)
        scheduleCloseWhenDetached(generation)
    }

    private fun scheduleCloseWhenDetached(generation: EngineGeneration) {
        if (!generation.retired || generation.attachments != 0 || generation.closeScheduled || generation.disposed) return
        generation.closeScheduled = true
        // Run after the composition disposal pass, including upstream terminal effects, has finished.
        mainHandler.post {
            synchronized(lifetimeLock) {
                generation.closeScheduled = false
                if (generation.retired && generation.attachments == 0 && !generation.disposed) {
                    generation.emulator.close()
                    generation.disposed = true
                    retired.remove(generation)
                }
            }
        }
    }

    internal class EngineGeneration(val id: Long, val emulator: TerminalEmulator) {
        var attachments = 0
        var retired = false
        var closeScheduled = false
        var disposed = false
    }
}

/**
 * Borrows an owner-retained emulator. Removing this composable does not end the SSH session.
 * Paste is exposed only when the host supplies an explicit user-request handler; remote OSC 52
 * operations are disabled independently. Hyperlinks never launch an external intent in this v1.
 * Upstream supplies keyboard/IME handling, selection, scrolling, resizing, and accessibility.
 */
@Composable
fun TerminalPane(
    state: TerminalSessionState,
    modifier: Modifier = Modifier,
    keyboardEnabled: Boolean = true,
    showSoftKeyboard: Boolean = true,
    onPasteRequest: (() -> Unit)? = null,
) {
    val generation = state.engine ?: return
    key(state, generation.id) {
        DisposableEffect(state, generation) {
            state.attach(generation)
            onDispose { state.detach(generation) }
        }
        Terminal(
            terminalEmulator = generation.emulator,
            modifier = modifier,
            keyboardEnabled = keyboardEnabled,
            showSoftKeyboard = showSoftKeyboard,
            onHyperlinkClick = {},
            onPasteRequest = if (keyboardEnabled) onPasteRequest else null,
        )
    }
}

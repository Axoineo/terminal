package dev.terminalworkspace.terminal

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the packaged JNI engine on Android, including the patched oversized CSI input class. */
@RunWith(AndroidJUnit4::class)
class TerminalEngineSmokeTest {
    @Test fun nativeEngineHandlesSplitUtf8OversizedCsiAndFreshSessionReplacement() = runBlocking {
        val state = withContext(Dispatchers.Main) { TerminalSessionState({}, { _, _, _, _ -> }) }
        try {
            val sequences = listOf(
                "\u001b[${(1..64).joinToString(";")}mtext",
                "\u001b[38:${(1..64).joinToString(":")}m",
                "\u001b[999999999999999999999999999999999999C",
                "\u001b]52;c;c2VjcmV0\u0007",
            )
            sequences.forEach { state.writeReceived(it.toByteArray()) }
            "€🙂".toByteArray().forEach { state.writeReceived(byteArrayOf(it)) }
            state.writeReceived("\u001b]unfinished".toByteArray())
            state.reset()
            state.writeReceived("A fresh connection\r\n".toByteArray())
        } finally { withContext(Dispatchers.Main) { state.close() } }
    }
}

package dev.terminalworkspace.core.data.remote

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.apache.sshd.server.Environment
import org.apache.sshd.server.ExitCallback
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.channel.ChannelSession
import org.apache.sshd.server.command.Command
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.shell.ShellFactory
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Real loopback SSH peers exercise authentication ordering, persistent pinning and cleanup. */
class SshClientSecurityTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test(timeout = 30_000) fun rejectedUnknownKeyNeverSendsPasswordAuthentication() = runBlocking {
        val fixture = server()
        val knownHosts = File(temporary.root, "known_hosts")
        val client = SshClient(knownHosts)
        val password = "secret".toByteArray()
        try {
            var prompt: HostKeyPrompt? = null
            val error = connectionFailure {
                client.connect(config(fixture.server.port, password)) { prompt = it; false }
            }
            assertTrue(error.message!!.contains("not approved"))
            assertTrue(prompt!!.fingerprintSha256.startsWith("SHA256:"))
            assertEquals("ssh-rsa", prompt!!.algorithm)
            assertEquals(0, fixture.authenticationCalls.get())
            assertFalse(knownHosts.exists())
            assertTrue(password.all { it == 0.toByte() })
        } finally {
            client.close()
            fixture.server.stop(true)
        }
    }

    @Test(timeout = 30_000) fun approvedKeyPersistsBeforeAuthenticationAndEchoIsRaw() = runBlocking {
        val knownHosts = File(temporary.root, "known_hosts")
        val fixture = server(beforeAuthentication = { assertTrue(knownHosts.isFile) })
        val client = SshClient(knownHosts)
        try {
            val expected = "echo bytes \u001b[31mred\u001b[0m\n".toByteArray()
            val captured = CompletableDeferred<ByteArray>()
            val collector = launch {
                val buffer = ByteArrayOutputStream()
                client.output.collect { chunk ->
                    buffer.write(chunk)
                    if (buffer.size() >= expected.size && !captured.isCompleted) captured.complete(buffer.toByteArray())
                }
            }
            try {
                client.connect(config(fixture.server.port)) { true }
                client.resize(100, 30)
                client.send(expected)
                assertArrayEquals(expected, withTimeout(10_000) { captured.await() })
                assertEquals(SshConnectionState.Connected, client.state.value)
            } finally {
                client.disconnect()
                collector.cancelAndJoin()
            }
            // Fresh client, same private pins: no TOFU prompt and a successful second login.
            val second = SshClient(knownHosts)
            try {
                second.connect(config(fixture.server.port)) { throw AssertionError("Known key prompted again") }
                assertEquals(SshConnectionState.Connected, second.state.value)
            } finally { second.close() }
        } finally {
            client.close()
            fixture.server.stop(true)
        }
    }

    @Test(timeout = 30_000) fun changedHostKeyFailsBeforeAuthenticationWithoutApprovalOverride() = runBlocking {
        val knownHosts = File(temporary.root, "known_hosts")
        val first = server()
        val port = first.server.port
        val client = SshClient(knownHosts)
        try { client.connect(config(port)) { true } } finally {
            client.close()
            first.server.stop(true)
        }
        val originalBytes = knownHosts.readBytes()
        val replacement = server(port)
        val second = SshClient(knownHosts)
        try {
            val error = connectionFailure {
                second.connect(config(port)) { throw AssertionError("Changed keys must never request approval") }
            }
            assertTrue(error.message!!.contains("changed"))
            assertEquals(0, replacement.authenticationCalls.get())
            assertArrayEquals(originalBytes, knownHosts.readBytes())
        } finally {
            second.close()
            replacement.server.stop(true)
        }
    }

    @Test(timeout = 30_000) fun hostKeyTrustIsScopedToPort() = runBlocking {
        val knownHosts = File(temporary.root, "known_hosts")
        val first = server()
        val second = server()
        val firstClient = SshClient(knownHosts)
        val secondClient = SshClient(knownHosts)
        try {
            firstClient.connect(config(first.server.port)) { true }
            var prompted = false
            connectionFailure { secondClient.connect(config(second.server.port)) { prompted = true; false } }
            assertTrue(prompted)
            assertEquals(0, second.authenticationCalls.get())
        } finally {
            firstClient.close()
            secondClient.close()
            first.server.stop(true)
            second.server.stop(true)
        }
    }

    @Test(timeout = 30_000) fun failureToPersistAnApprovedKeyStillPreventsAuthentication() = runBlocking {
        val parentIsFile = temporary.newFile("not-a-directory")
        val fixture = server()
        val client = SshClient(File(parentIsFile, "known_hosts"))
        try {
            var approved = false
            connectionFailure { client.connect(config(fixture.server.port)) { approved = true; true } }
            assertTrue(approved)
            assertEquals(0, fixture.authenticationCalls.get())
        } finally {
            client.close()
            fixture.server.stop(true)
        }
    }

    @Test(timeout = 30_000) fun malformedPrivatePinStoreFailsClosedInsteadOfForgettingTrust() = runBlocking {
        val knownHosts = temporary.newFile("known_hosts").apply { writeText("damaged pin row\n") }
        val fixture = server()
        val client = SshClient(knownHosts)
        try {
            connectionFailure {
                client.connect(config(fixture.server.port)) { throw AssertionError("Damaged store prompted for trust") }
            }
            assertEquals(0, fixture.authenticationCalls.get())
            assertEquals("damaged pin row\n", knownHosts.readText())
        } finally {
            client.close()
            fixture.server.stop(true)
        }
    }

    @Test(timeout = 15_000) fun cancellationClosesTheSocketDuringIncompleteHandshake() = runBlocking {
        val listener = ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))
        val peer = CompletableFuture<java.net.Socket>()
        thread(isDaemon = true) { runCatching { peer.complete(listener.accept()) } }
        val client = SshClient(File(temporary.root, "known_hosts"))
        val password = "secret".toByteArray()
        try {
            coroutineScope {
                val attempt = launch { client.connect(config(listener.localPort, password)) { true } }
                val socket = withContext(Dispatchers.IO) { peer.get(5, TimeUnit.SECONDS) }
                socket.use {
                    socket.soTimeout = 5_000
                    withTimeout(5_000) { attempt.cancelAndJoin() }
                    withContext(Dispatchers.IO) {
                        // The client's identification may precede EOF; consume it without a fake SSH server.
                        val stream = socket.getInputStream()
                        var bytes = 0
                        while (stream.read() != -1) check(++bytes <= 1_024)
                    }
                }
            }
            assertEquals(SshConnectionState.Disconnected, client.state.value)
            assertTrue(password.all { it == 0.toByte() })
        } finally {
            client.close()
            listener.close()
        }
    }

    private data class Fixture(val server: SshServer, val authenticationCalls: AtomicInteger)

    private fun server(port: Int = 0, beforeAuthentication: () -> Unit = {}): Fixture {
        val calls = AtomicInteger()
        val key = File(temporary.root, "server-key-${System.nanoTime()}")
        val server = SshServer.setUpDefaultServer().apply {
            host = "127.0.0.1"
            this.port = port
            keyPairProvider = SimpleGeneratorHostKeyProvider(key.toPath()).apply {
                algorithm = "RSA"
                keySize = 2_048
            }
            passwordAuthenticator = PasswordAuthenticator { username, password, _ ->
                calls.incrementAndGet()
                beforeAuthentication()
                username == "tester" && password == "secret"
            }
            shellFactory = ShellFactory { EchoCommand() }
            start()
        }
        return Fixture(server, calls)
    }

    private fun config(port: Int, password: ByteArray = "secret".toByteArray()) =
        SshConnectionConfig("127.0.0.1", port, "tester", password)

    private suspend fun connectionFailure(block: suspend () -> Unit): SshConnectionException {
        try { block() } catch (error: SshConnectionException) { return error }
        throw AssertionError("Expected a refused SSH connection")
    }

    private class EchoCommand : Command {
        private lateinit var input: InputStream
        private lateinit var output: OutputStream
        private lateinit var exit: ExitCallback
        private var worker: Thread? = null
        override fun setInputStream(input: InputStream) { this.input = input }
        override fun setOutputStream(output: OutputStream) { this.output = output }
        override fun setErrorStream(error: OutputStream) = Unit
        override fun setExitCallback(callback: ExitCallback) { exit = callback }
        override fun start(channel: ChannelSession, environment: Environment) {
            worker = thread(isDaemon = true) {
                try {
                    val buffer = ByteArray(8_192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        output.flush()
                    }
                } catch (_: Exception) {
                    // Client cancellation is expected in these fixtures.
                } finally { exit.onExit(0) }
            }
        }
        override fun destroy(channel: ChannelSession) {
            runCatching { input.close() }
            worker?.interrupt()
        }
    }
}

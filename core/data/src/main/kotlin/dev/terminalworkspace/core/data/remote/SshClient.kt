package dev.terminalworkspace.core.data.remote

import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.SocketFactory
import com.jcraft.jsch.UserInfo
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Password-only SSH PTY adapter around pinned mwiede/JSch. No command parsing, credentials store,
 * forwarding, host bootstrap, input recording, or automatic reconnect/replay is implemented here.
 * Collect output once, before connect, and fan it out to the recorder and terminal at the caller.
 */
class SshClient(knownHostsFile: File) : Closeable {
    private val hosts = KnownHostsStore(knownHostsFile)
    private val lifetime = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val active = AtomicReference<Connection?>()
    private val closed = AtomicBoolean(false)
    private val attempted = AtomicBoolean(false)
    private val connectMutex = Mutex()
    private val writeMutex = Mutex()
    private val received = Channel<ByteArray>(capacity = 32)
    private val mutableState = MutableStateFlow<SshConnectionState>(SshConnectionState.Disconnected)

    val state: StateFlow<SshConnectionState> = mutableState.asStateFlow()
    /** Bounded, back-pressured, single-consumer stream. Byte arrays are owned by the consumer. */
    val output: Flow<ByteArray> = received.receiveAsFlow()

    suspend fun connect(
        config: SshConnectionConfig,
        approveUnknownHost: suspend (HostKeyPrompt) -> Boolean,
    ) {
        try {
            connectMutex.withLock {
                check(!closed.get()) { "This SSH client is closed" }
                check(attempted.compareAndSet(false, true)) { "Create a new SSH client for each connection attempt" }
                check(active.get() == null) { "Disconnect the existing connection first" }
                val connection = Connection()
                active.set(connection)
                mutableState.value = SshConnectionState.Connecting
                try {
                    withTimeout(CONNECT_TOTAL_MS) {
                        blocking(connection) { context ->
                            val host = config.host.lowercase(Locale.ROOT)
                            val binding = "[$host]:${config.port}"
                            val session = JSch().getSession(config.username, host, config.port)
                            connection.session = session
                            session.setHostKeyAlias(binding)
                            session.setHostKeyRepository(object : HostKeyRepository {
                                override fun check(hostname: String, key: ByteArray): Int = hosts.verify(
                                    binding, host, config.port, key,
                                ) { prompt ->
                                    runBlocking(context) {
                                        withTimeout(HOST_APPROVAL_MS) { approveUnknownHost(prompt) }
                                    }
                                }
                                override fun getHostKey(host: String?, type: String?): Array<HostKey> = hosts.keys(host, type)
                                override fun getHostKey(): Array<HostKey> = hosts.keys(null, null)
                                override fun getKnownHostsRepositoryID(): String = "Private known hosts"
                                override fun add(hostkey: HostKey?, userinfo: UserInfo?) = error("Implicit host-key writes are disabled")
                                override fun remove(host: String?, type: String?) = error("Implicit host-key removal is disabled")
                                override fun remove(host: String?, type: String?, key: ByteArray?) = error("Implicit host-key removal is disabled")
                            })
                            configure(session)
                            session.setSocketFactory(object : SocketFactory {
                                override fun createSocket(host: String, port: Int): Socket {
                                    val socket = Socket()
                                    connection.socket.set(socket)
                                    if (connection.stopped.get()) {
                                        socket.close()
                                        throw CancellationException("Connection stopped")
                                    }
                                    socket.connect(InetSocketAddress(host, port), NETWORK_TIMEOUT_MS)
                                    return socket
                                }
                                override fun getInputStream(socket: Socket): InputStream = socket.getInputStream()
                                override fun getOutputStream(socket: Socket): OutputStream = socket.getOutputStream()
                            })
                            session.setPassword(config.password)
                            session.connect(NETWORK_TIMEOUT_MS)
                            context.ensureActive()
                            if (connection.stopped.get()) throw CancellationException("Connection stopped")
                            val shell = session.openChannel("shell") as ChannelShell
                            connection.shell = shell
                            shell.setAgentForwarding(false)
                            shell.setXForwarding(false)
                            shell.setPtyType("xterm-256color", 80, 24, 0, 0)
                            shell.setInputStream(null)
                            connection.input = shell.inputStream
                            shell.connect(NETWORK_TIMEOUT_MS)
                            connection.writer = shell.outputStream
                            context.ensureActive()
                        }
                    }
                    if (active.get() !== connection || connection.stopped.get()) throw CancellationException("Connection stopped")
                    mutableState.value = SshConnectionState.Connected
                    connection.reader = lifetime.launch { readOutput(connection) }
                } catch (error: Exception) {
                    connection.stop()
                    if (active.compareAndSet(connection, null)) {
                        mutableState.value = if (error is CancellationException) SshConnectionState.Disconnected
                            else SshConnectionState.Failed(safeMessage(error))
                    }
                    if (error is CancellationException) throw error
                    throw SshConnectionException(safeMessage(error), error)
                }
            }
        } finally {
            config.password.fill(0)
        }
    }

    suspend fun send(bytes: ByteArray) {
        require(bytes.size <= 65_536) { "Send input in bounded chunks" }
        writeMutex.withLock {
            val connection = connected()
            withTimeout(NETWORK_TIMEOUT_MS.toLong()) {
                blocking(connection) { _ ->
                    val writer = connection.writer ?: throw SshConnectionException("The shell is not connected.")
                    writer.write(bytes)
                    writer.flush()
                }
            }
        }
    }

    suspend fun resize(cols: Int, rows: Int, widthPixels: Int = 0, heightPixels: Int = 0) {
        require(cols in 1..1_000 && rows in 1..1_000 && widthPixels >= 0 && heightPixels >= 0)
        writeMutex.withLock {
            val connection = connected()
            withTimeout(NETWORK_TIMEOUT_MS.toLong()) {
                blocking(connection) { _ -> connection.shell?.setPtySize(cols, rows, widthPixels, heightPixels) }
            }
        }
    }

    suspend fun disconnect() {
        val connection = active.getAndSet(null)
        withContext(NonCancellable + Dispatchers.IO) { connection?.stop() }
        mutableState.value = SshConnectionState.Disconnected
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            active.getAndSet(null)?.stop()
            lifetime.cancel()
            received.close()
            mutableState.value = SshConnectionState.Disconnected
        }
    }

    private fun connected(): Connection = active.get()?.takeIf {
        !it.stopped.get() && mutableState.value == SshConnectionState.Connected
    } ?: throw SshConnectionException("The shell is not connected.")

    private suspend fun readOutput(connection: Connection) {
        var failure: Exception? = null
        try {
            val stream = connection.input ?: return
            val buffer = ByteArray(16_384)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = stream.read(buffer)
                if (count < 0) break
                if (count > 0) received.send(buffer.copyOf(count))
            }
        } catch (error: Exception) {
            failure = error
        } finally {
            connection.stop()
            if (active.compareAndSet(connection, null)) {
                mutableState.value = if (failure != null && failure !is CancellationException)
                    SshConnectionState.Failed("The SSH connection ended. Remote command outcomes remain unknown.")
                    else SshConnectionState.Disconnected
            }
        }
    }

    private suspend fun <T> blocking(connection: Connection, work: (CoroutineContext) -> T): T =
        suspendCancellableCoroutine { continuation ->
            val worker = lifetime.launch {
                try {
                    val result = operationSlots.withPermit { work(currentCoroutineContext()) }
                    if (continuation.isActive) continuation.resume(result)
                } catch (error: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            }
            val cancel = { continuation.cancel(); Unit }
            connection.pending.set(cancel)
            continuation.invokeOnCancellation {
                connection.stop()
                worker.cancel()
                connection.pending.compareAndSet(cancel, null)
            }
            worker.invokeOnCompletion { connection.pending.compareAndSet(cancel, null) }
        }

    private fun configure(session: Session) {
        session.setConfig("StrictHostKeyChecking", "yes")
        session.setConfig("PreferredAuthentications", "password")
        session.setConfig("NumberOfPasswordPrompts", "1")
        session.setConfig("host_certificate_to_key_fallback", "no")
        session.setConfig("server_host_key", "ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,rsa-sha2-512,rsa-sha2-256")
        session.setConfig("kex", "ecdh-sha2-nistp256,ecdh-sha2-nistp384,ecdh-sha2-nistp521,diffie-hellman-group16-sha512,diffie-hellman-group14-sha256")
        session.setConfig("cipher.c2s", "aes256-gcm@openssh.com,aes128-gcm@openssh.com,aes256-ctr,aes128-ctr")
        session.setConfig("cipher.s2c", "aes256-gcm@openssh.com,aes128-gcm@openssh.com,aes256-ctr,aes128-ctr")
        session.setConfig("mac.c2s", "hmac-sha2-512-etm@openssh.com,hmac-sha2-256-etm@openssh.com,hmac-sha2-512,hmac-sha2-256")
        session.setConfig("mac.s2c", "hmac-sha2-512-etm@openssh.com,hmac-sha2-256-etm@openssh.com,hmac-sha2-512,hmac-sha2-256")
        session.setConfig("enable_strict_kex", "yes")
        session.setConfig("require_strict_kex", "yes")
        session.setConfig("max_input_buffer_size", "65536")
        session.setDaemonThread(true)
        session.setServerAliveInterval(15_000)
        session.setServerAliveCountMax(2)
    }

    private fun safeMessage(error: Throwable): String {
        generateSequence(error) { it.cause }.filterIsInstance<HostTrustException>().firstOrNull()?.let { return it.message!! }
        return if (error.message?.contains("Auth fail", ignoreCase = true) == true)
            "Password authentication failed. Keyboard-interactive and key authentication are not supported yet."
        else "SSH could not connect. Check the address, password and network. This version requires strict key exchange and an ECDSA or RSA-SHA2 host key."
    }

    private class Connection {
        val stopped = AtomicBoolean(false)
        val socket = AtomicReference<Socket?>()
        val pending = AtomicReference<(() -> Unit)?>()
        @Volatile var session: Session? = null
        @Volatile var shell: ChannelShell? = null
        @Volatile var input: InputStream? = null
        @Volatile var writer: OutputStream? = null
        @Volatile var reader: Job? = null

        fun stop() {
            if (stopped.compareAndSet(false, true)) {
                // Close the actual socket first: channel/session cleanup can otherwise wait on I/O.
                runCatching { socket.getAndSet(null)?.close() }
                pending.getAndSet(null)?.invoke()
                reader?.cancel()
                runCatching { input?.close() }
                runCatching { writer?.close() }
                runCatching { shell?.disconnect() }
                runCatching { session?.disconnect() }
            }
        }
    }

    private companion object {
        // A platform DNS resolver may ignore interruption. Bound outstanding blocking calls
        // across all client instances, while canceled callers return without waiting for DNS.
        val operationSlots = Semaphore(2)
        const val NETWORK_TIMEOUT_MS = 15_000
        const val HOST_APPROVAL_MS = 45_000L
        const val CONNECT_TOTAL_MS = 75_000L
    }
}

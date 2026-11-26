package dev.terminalworkspace.core.data.remote

import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.JSch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Base64

/**
 * App-owned OpenSSH-format pins. JSch parses/checks keys; this wrapper adds explicit TOFU,
 * rejects changed algorithms as well as changed bytes, and atomically persists before auth.
 * There is deliberately no automatic key replacement/import or changed-key approval path.
 */
internal class KnownHostsStore(private val file: File) {
    fun verify(
        binding: String,
        host: String,
        port: Int,
        key: ByteArray,
        approve: (HostKeyPrompt) -> Boolean,
    ): Int {
        val candidate = HostKey(binding, key)
        if (candidate.type !in SUPPORTED_KEY_TYPES) {
            throw HostTrustException("This host-key type is not supported in this version.")
        }
        synchronized(FILE_LOCK) {
            if (checkedRepository(binding, candidate, key) != null) return HostKeyRepository.OK
        }
        val fingerprint = "SHA256:" + Base64.getEncoder().withoutPadding()
            .encodeToString(MessageDigest.getInstance("SHA-256").digest(key))
        if (!approve(HostKeyPrompt(host, port, candidate.type, fingerprint))) {
            throw HostTrustException("The unknown host key was not approved. No authentication was sent.")
        }
        synchronized(FILE_LOCK) {
            // Another connection may have committed a different pin while approval was open.
            if (checkedRepository(binding, candidate, key) == null) appendVerified(candidate, key)
        }
        return HostKeyRepository.OK
    }

    fun keys(host: String?, type: String?): Array<HostKey> = synchronized(FILE_LOCK) {
        load(readBytes()).getHostKey(host, type)
    }

    private fun checkedRepository(binding: String, candidate: HostKey, key: ByteArray): HostKeyRepository? {
        val repository = load(readBytes())
        val entries = repository.getHostKey(binding, null)
        if (entries.any { it.marker == "@revoked" && it.key == candidate.key }) {
            throw HostTrustException("This host key has been revoked. Connection refused.")
        }
        return when (repository.check(binding, key)) {
            HostKeyRepository.OK -> repository
            HostKeyRepository.CHANGED -> throw HostTrustException(
                "The saved host key has changed. Connection refused; verify the server independently.",
            )
            else -> {
                if (entries.isNotEmpty()) throw HostTrustException(
                    "The server presented a different host-key type. Connection refused.",
                )
                null
            }
        }
    }

    private fun appendVerified(candidate: HostKey, key: ByteArray) {
        val previous = readBytes()
        val line = "${candidate.host} ${candidate.type} ${candidate.key}\n".toByteArray(Charsets.US_ASCII)
        if (previous.size + line.size + 1 > MAX_FILE_BYTES) throw IOException("The known-host store is full.")
        val parent = file.absoluteFile.parentFile ?: throw IOException("The host-key store has no directory.")
        if (!parent.isDirectory && !parent.mkdirs()) throw IOException("Cannot create the private host-key store.")
        val staged = File.createTempFile(".known-hosts-", ".tmp", parent)
        try {
            FileOutputStream(staged).use { stream ->
                stream.write(previous)
                if (previous.isNotEmpty() && previous.last() != '\n'.code.toByte()) stream.write('\n'.code)
                stream.write(line)
                stream.fd.sync()
            }
            if (load(staged.readBytes()).check(candidate.host, key) != HostKeyRepository.OK) {
                throw IOException("The host-key store could not be verified.")
            }
            Files.move(staged.toPath(), file.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            staged.delete()
        }
    }

    private fun readBytes(): ByteArray {
        if (!file.exists()) return byteArrayOf()
        if (Files.isSymbolicLink(file.toPath()) || !file.isFile) throw IOException("Invalid private host-key store.")
        return file.inputStream().use { input ->
            val bytes = ByteArrayOutputStream()
            val buffer = ByteArray(8_192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (bytes.size() + count > MAX_FILE_BYTES) throw IOException("The host-key store is too large.")
                bytes.write(buffer, 0, count)
            }
            bytes.toByteArray()
        }
    }

    private fun load(bytes: ByteArray): HostKeyRepository {
        // This private store has no general-purpose import path. Reject damaged rows rather
        // than letting a permissive parser silently forget a previously trusted host.
        bytes.toString(Charsets.US_ASCII).lineSequence().forEach { line ->
            if (line.isBlank() || line.startsWith('#')) return@forEach
            val fields = line.trim().split(Regex("\\s+"))
            val offset = if (fields.first() == "@revoked") 1 else 0
            try {
                if (fields.size != offset + 3) throw IllegalArgumentException()
                val parsed = HostKey(fields[offset], Base64.getDecoder().decode(fields[offset + 2]))
                if (parsed.type != fields[offset + 1] || parsed.type !in SUPPORTED_KEY_TYPES) {
                    throw IllegalArgumentException()
                }
            } catch (error: Exception) {
                throw IOException("The private host-key store is damaged; connection refused.", error)
            }
        }
        return JSch().apply { setKnownHosts(ByteArrayInputStream(bytes)) }.hostKeyRepository
    }

    private companion object {
        val FILE_LOCK = Any()
        const val MAX_FILE_BYTES = 1_048_576
        val SUPPORTED_KEY_TYPES = setOf("ssh-rsa", "ecdsa-sha2-nistp256", "ecdsa-sha2-nistp384", "ecdsa-sha2-nistp521")
    }
}

package dev.terminalworkspace.core.data.remote

import java.io.IOException
import java.net.InetAddress

/** One-use credentials. connect consumes and clears password, including on failure/cancellation. */
class SshConnectionConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: ByteArray,
) {
    init {
        try {
            require(validHost(host)) {
                "Use an ASCII DNS name, IPv4 address, or unbracketed IPv6 literal. Use punycode for international names."
            }
            require(port in 1..65535)
            require(username.isNotBlank() && username.length <= 256 && username.none { it.isISOControl() })
            require(password.size <= 16_384)
        } catch (error: IllegalArgumentException) {
            password.fill(0)
            throw error
        }
    }

    override fun toString(): String = "SshConnectionConfig(credentials redacted)"

    private companion object {
        val DNS_LABEL = Regex("[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?")
        val IPV6_ZONE = Regex("[A-Za-z0-9_][A-Za-z0-9_.-]{0,63}")

        fun validHost(host: String): Boolean {
            if (host.isEmpty() || host.length > 253 || host.any { it.code !in 33..126 }) return false
            if (':' in host) {
                if (host.count { it == '%' } > 1) return false
                val address = host.substringBefore('%')
                if ('%' in host && !IPV6_ZONE.matches(host.substringAfter('%'))) return false
                if (address.count { it == ':' } < 2 ||
                    address.any { it !in "0123456789abcdefABCDEF:." }
                ) return false
                // The input is restricted to IPv6-literal characters and contains colons;
                // InetAddress validates the literal without DNS lookup. Parse the unscoped
                // part so a legitimate interface name need not exist on the build/test host.
                return runCatching { InetAddress.getByName(address) }.isSuccess
            }
            if (host.all { it in "0123456789." }) {
                val octets = host.split('.')
                return octets.size == 4 && octets.all { octet ->
                    val number = octet.toIntOrNull()
                    octet.isNotEmpty() && octet.length <= 3 &&
                        (octet.length == 1 || !octet.startsWith('0')) &&
                        number != null && number in 0..255
                }
            }
            // Do not allow known_hosts wildcard/list/negation/marker syntax. These exact
            // ASCII labels are later placed inside one [host]:port pin, never a pattern.
            val name = host.removeSuffix(".")
            return name.isNotEmpty() && name.split('.').all(DNS_LABEL::matches)
        }
    }
}

data class HostKeyPrompt(
    val host: String,
    val port: Int,
    val algorithm: String,
    val fingerprintSha256: String,
)

sealed interface SshConnectionState {
    data object Disconnected : SshConnectionState
    data object Connecting : SshConnectionState
    data object Connected : SshConnectionState
    data class Failed(val message: String) : SshConnectionState
}

class SshConnectionException(message: String, cause: Throwable? = null) : IOException(message, cause)

internal class HostTrustException(message: String) : IOException(message)

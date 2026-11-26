package dev.terminalworkspace.core.data.remote

import org.junit.Assert.assertTrue
import org.junit.Test

class SshConnectionConfigTest {
    @Test fun rejectsKnownHostsPatternsListsAndMarkers() {
        listOf(
            "*", "*.example.com", "host?name", "!host", "host,other", "@revoked", "user@host",
            "host#comment", "host|other", "host;other", "host=other",
        ).forEach(::assertRejected)
    }

    @Test fun rejectsUrisPathsControlsWhitespaceAndAmbiguousAddresses() {
        listOf(
            "", " host", "host ", "host\tname", "host\nname", "host\u0000name", "host\u007fname",
            "ssh://host", "host/path", "host\\path", "host:22", "[::1]", "[::1]:22",
            "127.1", "2130706433", "127.0.0.256", "127.00.0.1", "1.2.3.4.",
            "bad..name", "-host.example", "host-.example", "under_score.example", ".host",
            "fe80::1%", "fe80::1%wlan*", "fe80::1%wlan0%other", ":::1", "1:2:3", "gg::1",
            "::ffff:256.1.1.1", "bücher.example", "ｈｏｓｔ.example",
            "a".repeat(64) + ".example", "a".repeat(254),
        ).forEach(::assertRejected)
    }

    @Test fun acceptsAsciiDnsPunycodeAndIpLiterals() {
        listOf(
            "localhost", "my-host.local", "EXAMPLE.com", "example.com.", "xn--bcher-kva.example",
            "127.0.0.1", "192.0.2.255", "0.0.0.0", "::1", "::", "2001:db8::1",
            "2001:db8:0:0:0:0:0:1", "::ffff:192.0.2.1", "fe80::1%wlan0", "fe80::1%3",
        ).forEach { host ->
            val password = "secret".toByteArray()
            SshConnectionConfig(host, 22, "tester", password)
            password.fill(0)
        }
    }

    private fun assertRejected(host: String) {
        val password = "secret".toByteArray()
        try {
            SshConnectionConfig(host, 22, "tester", password)
        } catch (_: IllegalArgumentException) {
            assertTrue("Rejected configurations must clear credentials", password.all { it == 0.toByte() })
            return
        } finally {
            password.fill(0)
        }
        throw AssertionError("Accepted an invalid host")
    }
}

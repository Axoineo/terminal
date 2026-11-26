package dev.terminalworkspace.core.domain

import java.io.ByteArrayOutputStream

/** A bounded prefix of received bytes, not keystrokes, terminal scrollback, or unseen host output. */
class ReceivedOutputBuffer(private val limit: Int = SessionHistoryRepository.MAX_OUTPUT_BYTES) {
    init { require(limit > 0) }
    private val bytes = ByteArrayOutputStream(minOf(limit, 8_192))
    var truncated: Boolean = false
        private set
    var revision: Long = 0
        private set

    fun append(received: ByteArray) {
        if (received.isEmpty()) return
        val remaining = limit - bytes.size()
        val count = minOf(remaining, received.size)
        if (count > 0) bytes.write(received, 0, count)
        val newlyTruncated = received.size > remaining && !truncated
        if (received.size > remaining) truncated = true
        if (count > 0 || newlyTruncated) revision++
    }

    fun snapshot(): ByteArray = bytes.toByteArray()
}

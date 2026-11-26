package dev.terminalworkspace.core.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceivedOutputBufferTest {
    @Test fun `split UTF8 and terminal bytes remain unchanged`() {
        val raw = "\u001b[31m日本語\u001b[0m".toByteArray()
        val buffer = ReceivedOutputBuffer(128)
        raw.forEach { buffer.append(byteArrayOf(it)) }
        assertArrayEquals(raw, buffer.snapshot())
        assertFalse(buffer.truncated)
    }

    @Test fun `overflow keeps bounded prefix and announces truncation`() {
        val buffer = ReceivedOutputBuffer(4)
        buffer.append(byteArrayOf(1, 2, 3))
        buffer.append(byteArrayOf(4, 5, 6))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), buffer.snapshot())
        assertTrue(buffer.truncated)
        val revision = buffer.revision
        buffer.append(ByteArray(100))
        assertEquals(revision, buffer.revision)
    }

    @Test fun `snapshots do not share mutable transport storage`() {
        val buffer = ReceivedOutputBuffer(8)
        val input = byteArrayOf(1, 2)
        buffer.append(input)
        input[0] = 9
        val snapshot = buffer.snapshot()
        snapshot[1] = 9
        assertArrayEquals(byteArrayOf(1, 2), buffer.snapshot())
    }

    @Test fun `exact limit is complete until another byte arrives`() {
        val buffer = ReceivedOutputBuffer(2)
        buffer.append(byteArrayOf(1, 2))
        assertFalse(buffer.truncated)
        buffer.append(byteArrayOf(3))
        assertTrue(buffer.truncated)
    }
}

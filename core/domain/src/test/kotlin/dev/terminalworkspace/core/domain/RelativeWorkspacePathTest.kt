package dev.terminalworkspace.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RelativeWorkspacePathTest {
    @Test fun `relative paths preserve spaces and international filenames`() {
        assertEquals("src/日本語 file.kt", RelativeWorkspacePath.parse("src/日本語 file.kt").value)
    }

    @Test fun `absolute paths traversal URI paths and control characters are rejected`() {
        listOf("", "/etc/passwd", "../secret", "src/../secret", "./src", "src//file",
            "src/", "C:\\secret", "content://provider/file", "src/line\nbreak", "src/\u0000file")
            .forEach { invalid ->
                assertThrows(invalid, IllegalArgumentException::class.java) {
                    RelativeWorkspacePath.parse(invalid)
                }
            }
    }
}

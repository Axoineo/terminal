package dev.terminalworkspace.core.domain

import dev.terminalworkspace.core.model.ContentDigest
import dev.terminalworkspace.core.model.FileSyncState
import dev.terminalworkspace.core.model.SyncBaseline
import dev.terminalworkspace.core.model.SyncDecision
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncPolicyTest {
    private val original = ContentDigest("a".repeat(64))
    private val localEdit = ContentDigest("b".repeat(64))
    private val remoteEdit = ContentDigest("c".repeat(64))

    @Test fun `concurrent distinct edits require user resolution`() {
        assertEquals(SyncDecision.CONFLICT,
            SyncPolicy.decide(FileSyncState(SyncBaseline.Known(original), localEdit, remoteEdit)))
    }

    @Test fun `unknown baseline never authorizes overwrite`() {
        assertEquals(SyncDecision.CONFLICT,
            SyncPolicy.decide(FileSyncState(SyncBaseline.Unknown, localEdit, original)))
    }

    @Test fun `same content is synchronized regardless of history`() {
        assertEquals(SyncDecision.IN_SYNC,
            SyncPolicy.decide(FileSyncState(SyncBaseline.Unknown, localEdit, localEdit)))
    }

    @Test fun `one sided edit selects direction`() {
        assertEquals(SyncDecision.UPLOAD,
            SyncPolicy.decide(FileSyncState(SyncBaseline.Known(original), localEdit, original)))
        assertEquals(SyncDecision.DOWNLOAD,
            SyncPolicy.decide(FileSyncState(SyncBaseline.Known(original), original, remoteEdit)))
    }

    @Test fun `delete versus edit is a conflict`() {
        assertEquals(SyncDecision.CONFLICT,
            SyncPolicy.decide(FileSyncState(SyncBaseline.Known(original), null, remoteEdit)))
    }

    @Test fun `confirmed absence differs from missing baseline`() {
        assertEquals(SyncDecision.UPLOAD,
            SyncPolicy.decide(FileSyncState(SyncBaseline.Known(null), localEdit, null)))
        assertEquals(SyncDecision.CONFLICT,
            SyncPolicy.decide(FileSyncState(SyncBaseline.Unknown, localEdit, null)))
    }
}

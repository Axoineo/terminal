package dev.terminalworkspace.core.domain

import dev.terminalworkspace.core.model.FileSyncState
import dev.terminalworkspace.core.model.SyncBaseline
import dev.terminalworkspace.core.model.SyncDecision

/** Plans a change from trusted hashes. I/O errors must not be represented as missing files. */
object SyncPolicy {
    fun decide(state: FileSyncState): SyncDecision {
        if (state.local == state.remote) return SyncDecision.IN_SYNC
        val baseline = state.baseline as? SyncBaseline.Known ?: return SyncDecision.CONFLICT
        return when {
            state.local == baseline.digest -> SyncDecision.DOWNLOAD
            state.remote == baseline.digest -> SyncDecision.UPLOAD
            else -> SyncDecision.CONFLICT
        }
    }
}

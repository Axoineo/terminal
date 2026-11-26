package dev.terminalworkspace.core.domain

import dev.terminalworkspace.core.model.SavedSessionOutput
import dev.terminalworkspace.core.model.SavedSessionState
import dev.terminalworkspace.core.model.SavedSessionSummary
import kotlinx.coroutines.flow.Flow

interface SessionHistoryRepository {
    fun observeRecent(): Flow<List<SavedSessionSummary>>
    suspend fun create(id: String, host: String, port: Int, username: String, nowMillis: Long)
    suspend fun saveReceived(id: String, bytes: ByteArray, truncated: Boolean, nowMillis: Long)
    suspend fun markState(id: String, state: SavedSessionState, nowMillis: Long)
    suspend fun read(id: String): SavedSessionOutput?
    suspend fun delete(id: String)

    companion object {
        const val MAX_OUTPUT_BYTES = 256 * 1024
        const val MAX_SAVED_SESSIONS = 20
    }
}

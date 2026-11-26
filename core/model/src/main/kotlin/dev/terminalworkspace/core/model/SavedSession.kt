package dev.terminalworkspace.core.model

enum class SavedSessionState { CONNECTING, CONNECTED, DISCONNECTED, INTERRUPTED, FAILED_TO_CONNECT }

data class SavedSessionSummary(
    val id: String,
    val host: String,
    val port: Int,
    val username: String,
    val startedAtMillis: Long,
    val updatedAtMillis: Long,
    val state: SavedSessionState,
    val outputBytes: Int,
    val truncated: Boolean,
)

data class SavedSessionOutput(val session: SavedSessionSummary, val receivedBytes: ByteArray)

package dev.terminalworkspace.core.model

@JvmInline
value class ContentDigest(val sha256: String) {
    init { require(sha256.matches(Regex("[a-f0-9]{64}"))) }
}

/** Known(null) means confirmed absence; Unknown means there is no trusted baseline. */
sealed interface SyncBaseline {
    data object Unknown : SyncBaseline
    data class Known(val digest: ContentDigest?) : SyncBaseline
}

data class FileSyncState(
    val baseline: SyncBaseline,
    val local: ContentDigest?,
    val remote: ContentDigest?,
)

enum class SyncDecision { IN_SYNC, UPLOAD, DOWNLOAD, CONFLICT }

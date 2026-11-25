package dev.terminalworkspace.core.model

@JvmInline
value class TaskId(val value: String) {
    init { require(value.matches(Regex("[A-Za-z0-9_-]{1,64}"))) }
}

enum class ExecutionStatus { DRAFT, RUNNING, COMPLETION_UNKNOWN, SUCCEEDED, FAILED }

/** Execution evidence; transport connectivity is a separate concern. */
data class JournalTask(
    val id: TaskId,
    val workspaceId: WorkspaceId,
    val commandPreview: String,
    val status: ExecutionStatus,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val exitCode: Int? = null,
) {
    init {
        require(createdAtMillis >= 0 && updatedAtMillis >= createdAtMillis)
        when (status) {
            ExecutionStatus.SUCCEEDED -> require(exitCode == 0)
            ExecutionStatus.FAILED -> require(exitCode != null && exitCode != 0)
            else -> require(exitCode == null)
        }
    }
}

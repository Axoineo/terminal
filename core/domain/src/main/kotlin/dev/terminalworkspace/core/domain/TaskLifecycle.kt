package dev.terminalworkspace.core.domain

import dev.terminalworkspace.core.model.ExecutionStatus
import dev.terminalworkspace.core.model.JournalTask

sealed interface TaskEvent {
    data object Started : TaskEvent
    data object ConnectionLost : TaskEvent
    data class Completed(val exitCode: Int) : TaskEvent
}

object TaskLifecycle {
    fun apply(task: JournalTask, event: TaskEvent, nowMillis: Long): JournalTask {
        require(nowMillis >= 0)
        // Wall-clock skew must not discard confirmed execution evidence.
        val observedAt = maxOf(nowMillis, task.updatedAtMillis)
        return when (event) {
            TaskEvent.Started -> {
                require(task.status == ExecutionStatus.DRAFT) { "Only a new draft may start" }
                task.copy(status = ExecutionStatus.RUNNING, updatedAtMillis = observedAt)
            }
            TaskEvent.ConnectionLost -> {
                require(task.status == ExecutionStatus.RUNNING) { "Only running work can lose contact" }
                task.copy(status = ExecutionStatus.COMPLETION_UNKNOWN, updatedAtMillis = observedAt)
            }
            is TaskEvent.Completed -> {
                require(task.status in setOf(ExecutionStatus.RUNNING, ExecutionStatus.COMPLETION_UNKNOWN))
                task.copy(
                    status = if (event.exitCode == 0) ExecutionStatus.SUCCEEDED else ExecutionStatus.FAILED,
                    exitCode = event.exitCode,
                    updatedAtMillis = observedAt,
                )
            }
        }
    }

    /** Restoring UI state never executes or replays the command. */
    fun recoverAfterRestart(task: JournalTask, nowMillis: Long): JournalTask =
        if (task.status == ExecutionStatus.RUNNING) {
            apply(task, TaskEvent.ConnectionLost, maxOf(nowMillis, task.updatedAtMillis))
        } else {
            task
        }
}

package dev.terminalworkspace.core.domain

import dev.terminalworkspace.core.model.ExecutionStatus
import dev.terminalworkspace.core.model.JournalTask
import dev.terminalworkspace.core.model.TaskId
import dev.terminalworkspace.core.model.WorkspaceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class TaskLifecycleTest {
    private val draft = JournalTask(TaskId("task"), WorkspaceId("workspace"), "make test",
        ExecutionStatus.DRAFT, createdAtMillis = 10, updatedAtMillis = 10)

    @Test fun `loss of contact never claims success or failure`() {
        val running = TaskLifecycle.apply(draft, TaskEvent.Started, 11)
        val lost = TaskLifecycle.apply(running, TaskEvent.ConnectionLost, 12)
        assertEquals(ExecutionStatus.COMPLETION_UNKNOWN, lost.status)
        assertNull(lost.exitCode)
    }

    @Test fun `late confirmed exit can resolve uncertain work`() {
        val running = TaskLifecycle.apply(draft, TaskEvent.Started, 11)
        val lost = TaskLifecycle.recoverAfterRestart(running, 12)
        val finished = TaskLifecycle.apply(lost, TaskEvent.Completed(0), 13)
        assertEquals(ExecutionStatus.SUCCEEDED, finished.status)
        assertEquals(0, finished.exitCode)
    }

    @Test fun `nonzero exit is recorded as failure`() {
        val running = TaskLifecycle.apply(draft, TaskEvent.Started, 11)
        val failed = TaskLifecycle.apply(running, TaskEvent.Completed(2), 12)
        assertEquals(ExecutionStatus.FAILED, failed.status)
        assertEquals(2, failed.exitCode)
    }

    @Test fun `uncertain task cannot be started again`() {
        val running = TaskLifecycle.apply(draft, TaskEvent.Started, 11)
        val lost = TaskLifecycle.recoverAfterRestart(running, 12)
        assertThrows(IllegalArgumentException::class.java) {
            TaskLifecycle.apply(lost, TaskEvent.Started, 13)
        }
    }

    @Test fun `restart preserves confirmed terminal results`() {
        val running = TaskLifecycle.apply(draft, TaskEvent.Started, 11)
        val finished = TaskLifecycle.apply(running, TaskEvent.Completed(0), 12)
        assertEquals(finished, TaskLifecycle.recoverAfterRestart(finished, 20))
    }

    @Test fun `restart recovery is idempotent even after wall clock change`() {
        val running = TaskLifecycle.apply(draft, TaskEvent.Started, 11)
        val recovered = TaskLifecycle.recoverAfterRestart(running, 5)
        assertEquals(11L, recovered.updatedAtMillis)
        assertEquals(recovered, TaskLifecycle.recoverAfterRestart(recovered, 30))
    }

    @Test fun `clock change does not discard confirmed completion`() {
        val running = TaskLifecycle.apply(draft, TaskEvent.Started, 11)
        val completed = TaskLifecycle.apply(running, TaskEvent.Completed(0), 5)
        assertEquals(ExecutionStatus.SUCCEEDED, completed.status)
        assertEquals(11L, completed.updatedAtMillis)
    }

    @Test fun `negative time and impossible completion are rejected`() {
        val running = TaskLifecycle.apply(draft, TaskEvent.Started, 11)
        assertThrows(IllegalArgumentException::class.java) {
            TaskLifecycle.apply(running, TaskEvent.Completed(0), -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            TaskLifecycle.apply(draft, TaskEvent.Completed(0), 11)
        }
    }
}

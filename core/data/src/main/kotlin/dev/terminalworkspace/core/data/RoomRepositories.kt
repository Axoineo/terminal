package dev.terminalworkspace.core.data

import androidx.room.withTransaction
import dev.terminalworkspace.core.domain.JournalRepository
import dev.terminalworkspace.core.domain.TaskEvent
import dev.terminalworkspace.core.domain.TaskLifecycle
import dev.terminalworkspace.core.domain.WorkspaceRepository
import dev.terminalworkspace.core.model.ExecutionStatus
import dev.terminalworkspace.core.model.JournalTask
import dev.terminalworkspace.core.model.TaskId
import dev.terminalworkspace.core.model.Workspace
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomWorkspaceRepository(private val database: WorkspaceDatabase) : WorkspaceRepository {
    override fun observeWorkspaces() = database.workspaceDao().observe().map { rows -> rows.map { it.toModel() } }

    override suspend fun create(workspace: Workspace) = database.workspaceDao().insert(workspace.toEntity())
}

/** One application-scoped instance owns startup reconciliation before all journal I/O. */
class RoomJournalRepository(
    private val database: WorkspaceDatabase,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : JournalRepository {
    private val initialization = Mutex()
    private var initialized = false

    override fun observeRecentTasks() = flow {
        initializeOnce()
        emitAll(database.taskDao().observeRecent(limit = 100)
            .map { rows -> rows.map { it.toModel() } })
    }

    override suspend fun createDraft(task: JournalTask) {
        initializeOnce()
        require(task.status == ExecutionStatus.DRAFT) { "New tasks must begin as drafts" }
        database.taskDao().insert(task.toEntity())
    }

    override suspend fun applyEvent(id: TaskId, event: TaskEvent, nowMillis: Long) {
        initializeOnce()
        database.withTransaction {
            val current = requireNotNull(database.taskDao().get(id.value)) { "Unknown task" }.toModel()
            val updated = TaskLifecycle.apply(current, event, nowMillis)
            database.taskDao().update(updated.toEntity())
        }
    }

    private suspend fun initializeOnce() {
        initialization.withLock {
            if (!initialized) {
                database.taskDao().markInterrupted(nowMillis().coerceAtLeast(0))
                initialized = true
            }
        }
    }
}

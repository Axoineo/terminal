package dev.terminalworkspace.core.domain

import dev.terminalworkspace.core.model.JournalTask
import dev.terminalworkspace.core.model.TaskId
import dev.terminalworkspace.core.model.Workspace
import kotlinx.coroutines.flow.Flow

interface WorkspaceRepository {
    fun observeWorkspaces(): Flow<List<Workspace>>
    suspend fun create(workspace: Workspace)
}

interface JournalRepository {
    fun observeRecentTasks(): Flow<List<JournalTask>>
    suspend fun createDraft(task: JournalTask)
    suspend fun applyEvent(id: TaskId, event: TaskEvent, nowMillis: Long)
}

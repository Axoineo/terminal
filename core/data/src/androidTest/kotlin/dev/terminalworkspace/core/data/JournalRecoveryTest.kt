package dev.terminalworkspace.core.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.terminalworkspace.core.domain.TaskEvent
import dev.terminalworkspace.core.model.BackendKind
import dev.terminalworkspace.core.model.ExecutionStatus
import dev.terminalworkspace.core.model.JournalTask
import dev.terminalworkspace.core.model.TaskId
import dev.terminalworkspace.core.model.Workspace
import dev.terminalworkspace.core.model.WorkspaceId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JournalRecoveryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "journal-recovery-test.db"
    private var database: WorkspaceDatabase? = null

    @After fun cleanup() {
        database?.close()
        context.deleteDatabase(databaseName)
    }

    @Test fun reopenPreservesWorkAndMarksUnconfirmedExecutionUnknown() = runBlocking {
        context.deleteDatabase(databaseName)
        var opened = WorkspaceDatabase.open(context, databaseName).also { database = it }
        val workspace = Workspace(WorkspaceId("project"), "Project", BackendKind.REMOTE_SSH)
        RoomWorkspaceRepository(opened).create(workspace)
        var journal = RoomJournalRepository(opened)
        val draft = JournalTask(TaskId("build"), workspace.id, "make test",
            ExecutionStatus.DRAFT, 10, 10)
        journal.createDraft(draft)
        journal.applyEvent(draft.id, TaskEvent.Started, 11)
        opened.close()

        opened = WorkspaceDatabase.open(context, databaseName).also { database = it }
        journal = RoomJournalRepository(opened, nowMillis = { 12 })
        val restored = journal.observeRecentTasks().first().single()
        assertEquals(ExecutionStatus.COMPLETION_UNKNOWN, restored.status)
        assertEquals(workspace, RoomWorkspaceRepository(opened).observeWorkspaces().first().single())
        journal.applyEvent(draft.id, TaskEvent.Completed(0), 13)
        assertEquals(ExecutionStatus.SUCCEEDED, journal.observeRecentTasks().first().single().status)
    }

    @Test fun anotherObserverDoesNotReclassifyWorkStartedInThisProcess() = runBlocking {
        context.deleteDatabase(databaseName)
        val opened = WorkspaceDatabase.open(context, databaseName).also { database = it }
        val workspace = Workspace(WorkspaceId("project"), "Project", BackendKind.REMOTE_SSH)
        RoomWorkspaceRepository(opened).create(workspace)
        val journal = RoomJournalRepository(opened, nowMillis = { 12 })
        val draft = JournalTask(TaskId("build"), workspace.id, "make test",
            ExecutionStatus.DRAFT, 10, 10)
        journal.createDraft(draft)
        journal.applyEvent(draft.id, TaskEvent.Started, 13)
        repeat(2) {
            assertEquals(ExecutionStatus.RUNNING, journal.observeRecentTasks().first().single().status)
        }
    }
}

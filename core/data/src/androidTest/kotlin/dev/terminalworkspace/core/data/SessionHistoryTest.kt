package dev.terminalworkspace.core.data

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.terminalworkspace.core.domain.SessionHistoryRepository
import dev.terminalworkspace.core.model.SavedSessionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionHistoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "session-history-test.db"
    private var database: WorkspaceDatabase? = null

    private fun open() = WorkspaceDatabase.open(context, name).also { database = it }

    @After fun cleanup() { database?.close(); context.deleteDatabase(name) }

    @Test fun reopeningPreservesRawBytesAndMarksLiveSessionInterrupted() = runBlocking {
        context.deleteDatabase(name)
        var db = open()
        var history = RoomSessionHistoryRepository(db)
        val bytes = byteArrayOf(0x1b, 0x5b, 0x32, 0x4a, 0xe2.toByte(), 0x82.toByte())
        history.create("one", "example.org", 22, "developer", 10)
        history.saveReceived("one", bytes, true, 11)
        history.markState("one", SavedSessionState.CONNECTED, 12)
        db.close()
        db = open()
        history = RoomSessionHistoryRepository(db)
        val recovered = requireNotNull(history.read("one"))
        assertArrayEquals(bytes, recovered.receivedBytes)
        assertTrue(recovered.session.truncated)
        assertEquals(SavedSessionState.INTERRUPTED, recovered.session.state)
        history.delete("one")
        history.saveReceived("one", bytes, false, 13)
        assertNull(history.read("one")) // A late flush cannot resurrect deleted output.
    }

    @Test fun boundedRetentionUsesInsertionOrderWhenClockMovesBackward() = runBlocking {
        context.deleteDatabase(name)
        val history = RoomSessionHistoryRepository(open())
        repeat(21) { i -> history.create("session_$i", "example.org", 22, "developer", (100 - i).toLong()) }
        val rows = history.observeRecent().first()
        assertEquals(SessionHistoryRepository.MAX_SAVED_SESSIONS, rows.size)
        assertEquals("session_20", rows.first().id)
        assertNull(history.read("session_0"))
    }

    @Test fun versionOneMigrationPreservesExistingWorkspaceAndAddsHistory() = runBlocking {
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL("CREATE TABLE workspaces (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, backend TEXT NOT NULL)")
            old.execSQL("""CREATE TABLE tasks (id TEXT NOT NULL PRIMARY KEY, workspaceId TEXT NOT NULL,
                commandPreview TEXT NOT NULL, status TEXT NOT NULL, createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL, exitCode INTEGER,
                FOREIGN KEY(workspaceId) REFERENCES workspaces(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
            old.execSQL("CREATE INDEX index_tasks_workspaceId ON tasks(workspaceId)")
            old.execSQL("INSERT INTO workspaces VALUES ('existing', 'Existing project', 'REMOTE_SSH')")
            old.version = 1
        }
        val db = open() // Room validates the migrated schema when the first query opens it.
        assertEquals("Existing project", db.workspaceDao().observe().first().single().name)
        val history = RoomSessionHistoryRepository(db)
        history.create("new", "example.org", 22, "developer", 1)
        assertNotNull(history.read("new"))
    }
}

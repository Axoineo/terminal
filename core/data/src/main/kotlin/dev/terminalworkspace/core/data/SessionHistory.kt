package dev.terminalworkspace.core.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.withTransaction
import dev.terminalworkspace.core.domain.SessionHistoryRepository
import dev.terminalworkspace.core.model.SavedSessionOutput
import dev.terminalworkspace.core.model.SavedSessionState
import dev.terminalworkspace.core.model.SavedSessionSummary
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Entity(tableName = "saved_sessions")
data class SavedSessionEntity(
    @PrimaryKey val id: String,
    val host: String,
    val port: Int,
    val username: String,
    val startedAtMillis: Long,
    val updatedAtMillis: Long,
    val state: String,
    val output: ByteArray,
    val truncated: Boolean,
) {
    fun summary() = SavedSessionSummary(id, host, port, username, startedAtMillis,
        updatedAtMillis, SavedSessionState.valueOf(state), output.size, truncated)
}

@Dao
interface SessionHistoryDao {
    @Query("""SELECT id, host, port, username, startedAtMillis, updatedAtMillis, state,
        length(output) AS outputBytes, truncated FROM saved_sessions
        ORDER BY rowid DESC LIMIT 20""")
    fun observeRecent(): kotlinx.coroutines.flow.Flow<List<SavedSessionSummary>>

    @Insert
    suspend fun insert(row: SavedSessionEntity)

    @Query("SELECT * FROM saved_sessions WHERE id = :id")
    suspend fun get(id: String): SavedSessionEntity?

    @Query("""UPDATE saved_sessions SET output = :bytes, truncated = :truncated,
        updatedAtMillis = MAX(updatedAtMillis, :nowMillis) WHERE id = :id""")
    suspend fun saveReceived(id: String, bytes: ByteArray, truncated: Boolean, nowMillis: Long)

    @Query("""UPDATE saved_sessions SET state = :state,
        updatedAtMillis = MAX(updatedAtMillis, :nowMillis) WHERE id = :id""")
    suspend fun markState(id: String, state: String, nowMillis: Long)

    @Query("""UPDATE saved_sessions SET state = 'INTERRUPTED',
        updatedAtMillis = MAX(updatedAtMillis, :nowMillis)
        WHERE state IN ('CONNECTING', 'CONNECTED')""")
    suspend fun markInterrupted(nowMillis: Long)

    @Query("""DELETE FROM saved_sessions WHERE id NOT IN
        (SELECT id FROM saved_sessions ORDER BY rowid DESC LIMIT 20)""")
    suspend fun trimOldSessions()

    @Query("DELETE FROM saved_sessions WHERE id = :id")
    suspend fun delete(id: String)
}

/** One application-scoped instance; recorded output is opt-in and bounded, never typed input. */
class RoomSessionHistoryRepository(private val database: WorkspaceDatabase) : SessionHistoryRepository {
    private val startup = Mutex()
    private var initialized = false

    private suspend fun initialize() = startup.withLock {
        if (!initialized) {
            database.sessionHistoryDao().markInterrupted(System.currentTimeMillis().coerceAtLeast(0))
            initialized = true
        }
    }

    override fun observeRecent() = flow {
        initialize()
        emitAll(database.sessionHistoryDao().observeRecent())
    }

    override suspend fun create(id: String, host: String, port: Int, username: String, nowMillis: Long) {
        initialize()
        require(id.matches(Regex("[A-Za-z0-9_-]{1,64}")))
        require(host.isNotBlank() && host.length <= 253 && port in 1..65535)
        require(username.isNotBlank() && username.length <= 128 && nowMillis >= 0)
        database.withTransaction {
            database.sessionHistoryDao().insert(SavedSessionEntity(id, host, port, username,
                nowMillis, nowMillis, SavedSessionState.CONNECTING.name, byteArrayOf(), false))
            database.sessionHistoryDao().trimOldSessions()
        }
    }

    override suspend fun saveReceived(id: String, bytes: ByteArray, truncated: Boolean, nowMillis: Long) {
        initialize()
        require(bytes.size <= SessionHistoryRepository.MAX_OUTPUT_BYTES && nowMillis >= 0)
        database.sessionHistoryDao().saveReceived(id, bytes, truncated, nowMillis)
    }

    override suspend fun markState(id: String, state: SavedSessionState, nowMillis: Long) {
        initialize()
        require(nowMillis >= 0)
        database.sessionHistoryDao().markState(id, state.name, nowMillis)
    }

    override suspend fun read(id: String): SavedSessionOutput? {
        initialize()
        return database.sessionHistoryDao().get(id)?.let { SavedSessionOutput(it.summary(), it.output) }
    }

    override suspend fun delete(id: String) {
        initialize()
        database.sessionHistoryDao().delete(id)
    }
}

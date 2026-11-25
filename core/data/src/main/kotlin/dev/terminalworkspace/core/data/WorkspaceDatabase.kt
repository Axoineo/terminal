package dev.terminalworkspace.core.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import dev.terminalworkspace.core.model.BackendKind
import dev.terminalworkspace.core.model.ExecutionStatus
import dev.terminalworkspace.core.model.JournalTask
import dev.terminalworkspace.core.model.TaskId
import dev.terminalworkspace.core.model.Workspace
import dev.terminalworkspace.core.model.WorkspaceId
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val backend: String,
) {
    fun toModel() = Workspace(WorkspaceId(id), name, BackendKind.valueOf(backend))
}

@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(
        entity = WorkspaceEntity::class,
        parentColumns = ["id"],
        childColumns = ["workspaceId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index("workspaceId")],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val commandPreview: String,
    val status: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val exitCode: Int?,
) {
    fun toModel() = JournalTask(
        TaskId(id), WorkspaceId(workspaceId), commandPreview,
        ExecutionStatus.valueOf(status), createdAtMillis, updatedAtMillis, exitCode,
    )
}

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspaces ORDER BY name COLLATE NOCASE, id")
    fun observe(): Flow<List<WorkspaceEntity>>

    @Insert
    suspend fun insert(workspace: WorkspaceEntity)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY updatedAtMillis DESC, id LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: String): TaskEntity?

    @Insert
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("""
        UPDATE tasks
        SET status = 'COMPLETION_UNKNOWN', updatedAtMillis = MAX(updatedAtMillis, :nowMillis)
        WHERE status = 'RUNNING'
    """)
    suspend fun markInterrupted(nowMillis: Long)
}

@Database(entities = [WorkspaceEntity::class, TaskEntity::class], version = 1, exportSchema = true)
abstract class WorkspaceDatabase : RoomDatabase() {
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun taskDao(): TaskDao

    companion object {
        fun open(context: Context, name: String = "terminal-workspace.db"): WorkspaceDatabase =
            Room.databaseBuilder(context.applicationContext, WorkspaceDatabase::class.java, name).build()
    }
}

internal fun Workspace.toEntity() = WorkspaceEntity(id.value, name, backend.name)

internal fun JournalTask.toEntity() = TaskEntity(
    id.value, workspaceId.value, commandPreview, status.name,
    createdAtMillis, updatedAtMillis, exitCode,
)

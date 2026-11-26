package dev.terminalworkspace

import android.app.Application
import dev.terminalworkspace.core.data.RoomJournalRepository
import dev.terminalworkspace.core.data.RoomWorkspaceRepository
import dev.terminalworkspace.core.data.RoomSessionHistoryRepository
import dev.terminalworkspace.core.data.WorkspaceDatabase

class TerminalWorkspaceApplication : Application() {
    val container by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    private val database = WorkspaceDatabase.open(application)
    val workspaces = RoomWorkspaceRepository(database)
    val journal = RoomJournalRepository(database)
    val sessionHistory = RoomSessionHistoryRepository(database)
    val knownHostsFile = application.filesDir.resolve("ssh/known_hosts")
}

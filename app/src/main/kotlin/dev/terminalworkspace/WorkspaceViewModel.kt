package dev.terminalworkspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.terminalworkspace.core.domain.JournalRepository
import dev.terminalworkspace.core.domain.WorkspaceRepository
import dev.terminalworkspace.core.model.BackendKind
import dev.terminalworkspace.core.model.JournalTask
import dev.terminalworkspace.core.model.Workspace
import dev.terminalworkspace.core.model.WorkspaceId
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkspaceViewModel(
    private val workspaceRepository: WorkspaceRepository,
    private val journalRepository: JournalRepository,
) : ViewModel() {
    private val _hasStorageError = MutableStateFlow(false)
    val hasStorageError = _hasStorageError.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val workspaces = workspaceRepository.observeWorkspaces()
        .catch { _hasStorageError.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Workspace>())

    val tasks = journalRepository.observeRecentTasks()
        .catch { _hasStorageError.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<JournalTask>())

    fun createWorkspace(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 120) return
        viewModelScope.launch {
            reportStorageFailure {
                workspaceRepository.create(Workspace(
                    WorkspaceId(UUID.randomUUID().toString()), trimmed, BackendKind.REMOTE_SSH,
                ))
            }
        }
    }

    private suspend fun reportStorageFailure(action: suspend () -> Unit) {
        try {
            action()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            _hasStorageError.value = true
        }
    }
}

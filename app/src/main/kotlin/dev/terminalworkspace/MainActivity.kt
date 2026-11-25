package dev.terminalworkspace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.terminalworkspace.core.model.ExecutionStatus
import dev.terminalworkspace.core.model.JournalTask
import dev.terminalworkspace.core.model.Workspace

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as TerminalWorkspaceApplication).container
        setContent {
            MaterialTheme {
                val model: WorkspaceViewModel = viewModel(factory = viewModelFactory {
                    initializer { WorkspaceViewModel(container.workspaces, container.journal) }
                })
                val workspaces by model.workspaces.collectAsStateWithLifecycle()
                val tasks by model.tasks.collectAsStateWithLifecycle()
                val storageError by model.hasStorageError.collectAsStateWithLifecycle()
                WorkspaceScreen(workspaces, tasks, storageError, model::createWorkspace)
            }
        }
    }
}

@Composable
private fun WorkspaceScreen(
    workspaces: List<Workspace>,
    tasks: List<JournalTask>,
    storageError: Boolean,
    onCreate: (String) -> Unit,
) {
    var showCreate by rememberSaveable { mutableStateOf(false) }
    Scaffold(modifier = Modifier.fillMaxSize()) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() })
                Text(stringResource(R.string.introduction))
            }
            if (storageError) {
                item { Text(stringResource(R.string.storage_error), color = MaterialTheme.colorScheme.error) }
            }
            item {
                Text(stringResource(R.string.workspaces_title), style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() })
                Button(onClick = { showCreate = true }) { Text(stringResource(R.string.create_workspace)) }
            }
            if (workspaces.isEmpty()) {
                item { Text(stringResource(R.string.no_workspaces)) }
            }
            items(workspaces, key = { it.id.value }) { workspace ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(workspace.name, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.remote_not_connected))
                    }
                }
            }
            item {
                Text(stringResource(R.string.journal_title), style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() })
            }
            if (tasks.isEmpty()) {
                item { Text(stringResource(R.string.no_tasks)) }
            }
            items(tasks, key = { it.id.value }) { task ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(task.commandPreview)
                        Text(stringResource(when (task.status) {
                            ExecutionStatus.DRAFT -> R.string.status_draft
                            ExecutionStatus.RUNNING -> R.string.status_running
                            ExecutionStatus.COMPLETION_UNKNOWN -> R.string.status_unknown
                            ExecutionStatus.SUCCEEDED -> R.string.status_succeeded
                            ExecutionStatus.FAILED -> R.string.status_failed
                        }))
                    }
                }
            }
        }
    }
    if (showCreate) {
        CreateWorkspaceDialog(
            onDismiss = { showCreate = false },
            onCreate = { name -> onCreate(name); showCreate = false },
        )
    }
}

@Composable
private fun CreateWorkspaceDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_workspace)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 120) name = it },
                label = { Text(stringResource(R.string.workspace_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

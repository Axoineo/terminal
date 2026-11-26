package dev.terminalworkspace

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.terminalworkspace.core.model.SavedSessionOutput
import dev.terminalworkspace.terminal.TerminalPane
import dev.terminalworkspace.terminal.TerminalSessionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as TerminalWorkspaceApplication).container
        setContent {
            MaterialTheme {
                val model: SshConsoleViewModel = viewModel(factory = viewModelFactory {
                    initializer { SshConsoleViewModel(container.sessionHistory, container.knownHostsFile) }
                })
                ConsoleScreen(model)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConsoleScreen(model: SshConsoleViewModel) {
    val context = LocalContext.current
    val sessions by model.savedSessions.collectAsStateWithLifecycle()
    var showHistory by remember { mutableStateOf(false) }
    var pastePreview by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) model.connect() else model.permissionDenied()
    }
    val requestPaste = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        // Access only after a deliberate tap; no HTML, intents, automatic reads or remote OSC 52.
        val item = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)
        pastePreview = item?.text?.take(16_384)?.toString()
    }
    Scaffold(Modifier.fillMaxSize().imePadding()) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Terminal Workspace", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).padding(top = 12.dp))
                TextButton(onClick = { showHistory = true }) { Text("History") }
            }
            Text(model.message, style = MaterialTheme.typography.bodySmall)
            if (!model.busy) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connect to your server", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(model.host, { if (it.length <= 253) model.host = it },
                        label = { Text("Hostname or IP address") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(model.port, { if (it.length <= 5) model.port = it },
                            label = { Text("Port") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(model.username, { if (it.length <= 128) model.username = it },
                            label = { Text("Username") }, singleLine = true, modifier = Modifier.weight(2f))
                    }
                    OutlinedSecureTextField(state = model.password, label = { Text("Password") },
                        textObfuscationMode = TextObfuscationMode.Hidden,
                        modifier = Modifier.fillMaxWidth())
                    Text("Password authentication only. Requires a modern SSH server with strict key exchange and an ECDSA or RSA-SHA2 host key.",
                        style = MaterialTheme.typography.bodySmall)
                    Row {
                        Checkbox(model.localNetwork, { model.localNetwork = it })
                        Text("Local network or VPN server", modifier = Modifier.padding(top = 12.dp))
                    }
                    Row {
                        Checkbox(model.recordOutput, { model.recordOutput = it })
                        Text("Save received output on this device", modifier = Modifier.padding(top = 12.dp))
                    }
                    Text("Output may contain secrets, including text echoed by the server. Recording is optional: first 256 KiB per session, latest 20 sessions. Passwords and typed input are not recorded separately. Backups are disabled.",
                        style = MaterialTheme.typography.bodySmall)
                    Button(onClick = {
                        val needsLocal = model.localNetwork || looksLocal(model.host)
                        if (Build.VERSION.SDK_INT >= 37 && needsLocal &&
                            context.checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED)
                            permission.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                        else model.connect()
                    }, enabled = model.host.isNotBlank() && model.username.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()) { Text("Connect") }
                    Text("A mobile connection can end when Android stops the app. This version does not reconnect or replay commands automatically.",
                        style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = model::disconnect) { Text("Disconnect") }
                    TextButton(onClick = requestPaste, enabled = model.connected) { Text("Paste…") }
                    TextButton(onClick = { model.terminal.paste("\u0003") }, enabled = model.connected) { Text("Ctrl-C") }
                    TextButton(onClick = { model.terminal.paste("\t") }, enabled = model.connected) { Text("Tab") }
                }
                Text(model.recordingMessage, style = MaterialTheme.typography.bodySmall)
            }
            // Retain the live terminal composition even when a history dialog is opened.
            TerminalPane(model.terminal, Modifier.fillMaxWidth().weight(if (model.busy) 1f else 0.01f),
                keyboardEnabled = model.connected, showSoftKeyboard = model.connected,
                onPasteRequest = requestPaste)
        }
    }
    model.trustPrompt?.let { prompt ->
        AlertDialog(onDismissRequest = { model.approveHost(false) },
            title = { Text("Verify this server") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${prompt.host}:${prompt.port}\nKey type: ${prompt.algorithm}")
                Text(prompt.fingerprintSha256)
                Text("Compare this fingerprint with a trusted source before approving. Approval pins this key on this device. A changed key will be blocked.")
            } },
            confirmButton = { TextButton(onClick = { model.approveHost(true) }) { Text("Trust and connect") } },
            dismissButton = { TextButton(onClick = { model.approveHost(false) }) { Text("Cancel") } })
    }
    pastePreview?.let { text ->
        AlertDialog(onDismissRequest = { pastePreview = null }, title = { Text("Review paste") },
            text = { Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                Text("Pasted newlines may execute commands. Up to 16,384 characters are shown and sent.")
                Text(text)
            } },
            confirmButton = { TextButton(onClick = {
                if (model.connected) model.terminal.paste(text)
                pastePreview = null
            }) { Text("Paste to server") } },
            dismissButton = { TextButton(onClick = { pastePreview = null }) { Text("Cancel") } })
    }
    if (showHistory) {
        AlertDialog(onDismissRequest = { showHistory = false }, title = { Text("Saved sessions") },
            text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sessions.isEmpty()) Text("No saved output. Enable recording before connecting.")
                sessions.forEach { session ->
                    Text("${session.username}@${session.host}:${session.port}")
                    Text("${session.state} · ${session.outputBytes} bytes${if (session.truncated) " · truncated" else ""}",
                        style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(onClick = { model.openHistory(session.id); showHistory = false }) { Text("View") }
                        TextButton(onClick = { model.deleteHistory(session.id) }) { Text("Delete") }
                    }
                }
            } }, confirmButton = { TextButton(onClick = { showHistory = false }) { Text("Close") } })
    }
    model.selectedHistory?.let { HistoryViewer(it, model::closeHistory) }
}

@Composable
private fun HistoryViewer(output: SavedSessionOutput, onClose: () -> Unit) {
    val terminal = remember(output.session.id) { TerminalSessionState({}, { _, _, _, _ -> }) }
    LaunchedEffect(terminal) { terminal.writeReceived(output.receivedBytes) }
    DisposableEffect(terminal) { onDispose { terminal.close() } }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        MaterialTheme {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text("Saved received output · read only", style = MaterialTheme.typography.titleMedium)
                Text("${output.session.username}@${output.session.host}:${output.session.port}")
                Text("${output.session.state}. Command outcomes are not recorded.${if (output.session.truncated) " Only the first 256 KiB is available." else ""}")
                TextButton(onClick = onClose) { Text("Close") }
                TerminalPane(terminal, Modifier.fillMaxWidth().weight(1f),
                    keyboardEnabled = false, showSoftKeyboard = false)
            }
        }
    }
}

/** Permission hint only; no DNS lookup or scanning. Users can mark private DNS/VPN addresses. */
private fun looksLocal(host: String): Boolean {
    val value = host.lowercase().trim()
    val ipv4 = value.split('.').mapNotNull { it.toIntOrNull() }
    return !value.contains('.') && !value.contains(':') || value.endsWith(".local") ||
        value.startsWith("fc") && value.contains(':') || value.startsWith("fd") && value.contains(':') ||
        value.startsWith("fe80:") || ipv4.size == 4 && (ipv4[0] == 10 ||
        ipv4[0] == 192 && ipv4[1] == 168 || ipv4[0] == 172 && ipv4[1] in 16..31 ||
        ipv4[0] == 169 && ipv4[1] == 254)
}

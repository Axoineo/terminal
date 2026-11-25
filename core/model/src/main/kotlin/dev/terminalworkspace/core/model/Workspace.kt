package dev.terminalworkspace.core.model

@JvmInline
value class WorkspaceId(val value: String) {
    init { require(value.matches(Regex("[A-Za-z0-9_-]{1,64}"))) }
}

enum class BackendKind { REMOTE_SSH, LOCAL_MANAGED }

data class Workspace(
    val id: WorkspaceId,
    val name: String,
    val backend: BackendKind,
) {
    init { require(name.isNotBlank() && name.length <= 120) }
}

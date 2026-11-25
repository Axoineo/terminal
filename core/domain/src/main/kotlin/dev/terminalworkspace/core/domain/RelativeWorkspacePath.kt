package dev.terminalworkspace.core.domain

/** Logical path validation only: filesystem adapters must also prevent symlink escape and races. */
@JvmInline
value class RelativeWorkspacePath private constructor(val value: String) {
    companion object {
        fun parse(raw: String): RelativeWorkspacePath {
            require(raw.isNotEmpty() && raw.length <= 4096)
            require(!raw.startsWith('/') && '\\' !in raw && ':' !in raw)
            require(raw.none { it.code < 32 || it.code == 127 })
            require(raw.split('/').none { it.isEmpty() || it == "." || it == ".." })
            return RelativeWorkspacePath(raw)
        }
    }
}

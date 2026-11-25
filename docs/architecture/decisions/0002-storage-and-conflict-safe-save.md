# ADR 0002: Private working copies and explicit save reconciliation

Date: 25 November 2026. Status: **Accepted design constraint**; adapters are future implementation.

## Context

SAF URIs, app-private paths and remote paths have different capabilities. A mobile user needs their edit preserved when a provider disconnects or a host file changes. SFTP transfer support does not itself solve draft retention or lost updates.

## Decision

Use an app-private persistent working copy. Track external/remote bindings and their base revision separately. Draft/local save precedes synchronization. SAF uses URI APIs and capability discovery, never invented filesystem paths. Preserve grants when offered, recover from revocation, and provide user-selected export because uninstall removes private files. [SAF](https://developer.android.com/training/data-storage/shared/documents-files), [private-storage lifecycle](https://developer.android.com/training/data-storage/app-specific).

A remote save compares the current destination with the base, uploads to a unique sibling temporary file, rechecks, and prefers negotiated atomic replacement. It never deletes the destination before replacement. Conflicts retain three versions and require a merge/save-copy/explicit-overwrite action. Unknown upload/rename outcomes are inspected before retry. File metadata and symlink behavior have a declared limited contract.

## Guarantee boundary

Atomic rename prevents partial content being visible at the destination; it is not compare-and-swap. The check/rename race remains with uncooperative writers. A strict revision precondition needs a cooperating server that performs it atomically. OpenSSH's `posix-rename` and `fsync` are capability extensions, not universal conflict prevention. [OpenSSH extensions](https://github.com/openssh/openssh-portable/blob/master/PROTOCOL).

## Consequences

More storage and revision bookkeeping are required. The app can preserve drafts and surface conflicts without claiming absolute safety it cannot enforce. Provider-only destinations may require save-copy. Stronger remote transactions are a later optional helper contract, not silently emulated by shell scripts or lock files.

Details and recovery states: [system design](../system-design.md).

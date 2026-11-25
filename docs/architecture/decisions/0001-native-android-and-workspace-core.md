# ADR 0001: Native Android workspace foundation

Date: 25 November 2026. Status: **Accepted** for the starter architecture.

## Context

The product's highest-risk interactions are Android input/accessibility, storage grants, lifecycle recovery and future key/service integration. iOS is optional. Existing apps already provide polished SSH/Mosh clients; owning another distribution does not establish a reason to switch.

## Decision

Use Kotlin and Compose, plain JVM `core:model` and `core:domain`, Android Room persistence in `core:data`, and an `app` composition root. Keep execution behind ports. Build persistent workspace/journal behavior first; then one remote SSH/SFTP vertical slice. Do not bundle a Linux distro or implement a custom terminal parser in the starter.

Android recommends Compose, repository boundaries and state-driven UI. Those recommendations support the choice; they do not prove comparative performance against Flutter or WebView. [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations).

## Consequences

Android system integrations stay direct, reliability policies can be unit tested, and a future terminal View/native component can be embedded without replacing the workspace model. Cross-platform UI sharing is deferred. An app-owned local copy and offline journal are useful without execution; all execution controls must honestly show their implementation status.

Local Linux remains an independent proof gate: execution/distribution rules, PTY/process lifetime, resource budgets, license notices, security updates and migration/repair must be demonstrated on supported Android versions. Modern targets cannot assume writable app-home binaries can be executed. [Android execution restrictions](https://developer.android.com/about/versions/10/behavior-changes-10#execute-permission).

Revisit if iOS becomes equally important or measured native integration costs exceed an alternative. Details: [system design](../system-design.md), [technology evaluation](../technology-evaluation.md).

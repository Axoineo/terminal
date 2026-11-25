# Implementation status

Implementation snapshot: **25 November 2026**; hosted CI setup updated **26 November 2026**. This file records the scaffold's actual scope; architecture and roadmap documents describe the larger target.

## Implemented foundation

| Area | Current behavior | Boundary |
| --- | --- | --- |
| Android UI | Compose workspace list, workspace naming dialog, saved-task list, readable empty/error states, heading semantics | No editor, terminal surface, host connection, or command-entry UI |
| Workspace storage | Room persists workspace ID, name, and intended backend kind | Creating an entry does not create a filesystem working tree or remote directory |
| Task model | Draft, running, completion unknown, succeeded, failed; completed state requires exit evidence | No backend produces real task events yet |
| Recovery | One application-scoped journal repository reconciles previous running rows before its I/O; repeated observers do not repeat recovery | No live socket/service restoration, output recorder, or remote reconciliation |
| Clock changes | Display timestamps are clamped without rejecting confirmed completion evidence | No distributed event-ordering protocol implemented |
| Sync policy | Three-way content comparison with explicit unknown baseline and conflict decisions | No scanning, transfer, merge UI, or atomic remote-save implementation |
| Paths | Rejects absolute/URI/traversal/noncanonical logical paths while preserving ordinary Unicode/spaces | Does not yet perform filesystem containment or symlink-safe I/O |
| Privacy | No networking/broad-storage permission, no credentials, platform backup/transfer exclusions | No encrypted journal, explicit export, retention UI, or restore tool |

## Pending product capabilities

SAF working-copy import/export, basic editing, SSH host-key/authentication support, terminal emulation, SFTP synchronization, output chunks/search, multiplexer support, accessible terminal-output selection, verified backup/restore, and managed local execution all remain pending.

## Verification record

Checked with JDK 17, Gradle 9.5.0, AGP 9.3.3, Kotlin 2.4.21, and compile SDK 37.1. The target remains API 37 and the minimum remains API 26. See [development setup](development.md) for setup and [validation plan](quality/validation-plan.md) for the larger release criteria.

| Check | Result | Evidence / limit |
| --- | --- | --- |
| Repository validation | **Passed** | Local Markdown links/anchors, all 33 ordered issue IDs, XML, TOML, module and wrapper files checked. External URLs are not fetched by this script. |
| Domain unit tests | **16 passed, 0 failed, 0 skipped** | Eight task-lifecycle, six sync-policy, and two logical-path tests. Report: `core/domain/build/reports/tests/test/index.html`. |
| Android debug build | **Passed** | `app/build/outputs/apk/debug/app-debug.apk`; final app resources also rebuilt cleanly with the build cache disabled. |
| Android lint | **Passed: 0 errors, 5 warnings** | Remaining warnings announce newer Gradle/AGP/coroutines releases. The starting toolchain is deliberately pinned; no warning suppression was added. Report: `app/build/reports/lint-results-debug.html`. |
| Persistence instrumentation compilation | **Passed** | `core/data/build/outputs/apk/androidTest/debug/data-debug-androidTest.apk` contains the database-reopen/recovery and repeated-observer scenarios. Compilation is not device execution. |
| Packaged permissions | **Checked** | APK inspection confirms no `INTERNET` or broad-storage permission. AndroidX contributes its signature-protected dynamic-receiver permission. |
| Connected instrumentation | **Not run** | No attached device/emulator was available; the host also lacks `/dev/kvm`. |
| TalkBack, braille, and runtime UI testing | **Not run** | Requires the declared devices and assistive-technology configurations. |
| Comparative studies / switching demand | **Not validated** | Product hypotheses and proposed study thresholds remain research work. |
| Hosted CI | **Run status tracked in Actions** | The first run found `sdkmanager` absent on the runner. The workflow now explicitly provisions SDK tools before running the checks. See [workflow runs](https://github.com/Axoineo/terminal/actions/workflows/ci.yml) for the current result. |

The composite check was run as:

```sh
./gradlew :core:domain:test :app:assembleDebug :app:lintDebug :core:data:assembleDebugAndroidTest \
  --no-daemon --max-workers=1 \
  -Dorg.gradle.jvmargs='-Xmx1024m -Dfile.encoding=UTF-8' --console=plain
```

After the launcher resource move, a clean app build with `--no-build-cache` removed stale generated resource outputs. Lint was refreshed after the empty old resource directory was removed. No source-level compiler errors remain. These checks establish the limited foundation, not the later terminal, synchronization, backup, or local-runtime features.

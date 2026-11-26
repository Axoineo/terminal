# Terminal Workspace

A small Android SSH client built by reusing **ConnectBot termlib 0.3.15** for the terminal and **mwiede JSch 2.28.8** for SSH. The current v1 focuses on a real remote terminal, explicit host-key trust, transient password authentication and optional saved received-output history. **Terminal Workspace** remains a working name.

The SSH/terminal adapters and history foundation are implemented in source; the first debug build and JVM checks have passed, while runtime dependency and Android device checks are in progress. See [implementation status](docs/implementation-status.md) for the exact current checks. A working APK build, a host-side SSH test and a real Android device session establish different things.

## V1 scope

- Reuse an existing terminal component and SSH implementation; no custom terminal parser or Linux distribution.
- Connect to a user-controlled SSH host with password authentication. Passwords/private keys are not saved by default.
- Ask explicitly before trusting an unknown SHA-256 host-key fingerprint; persist an approved pin before authentication. Changed keys fail closed.
- Use the backend's tested modern RSA-SHA2/ECDSA and strict-key-exchange configuration. Older servers, keyboard-interactive MFA, key authentication and unproven provider/algorithm combinations are outside the initial supported route.
- Optionally retain received output in app-private Room storage: capture starts off, at most 256 KiB per session and 20 saved sessions, with deletion. This is a bounded prefix of bytes actually received, not a complete remote transcript or per-command result record.
- Reconnect deliberately. Never replay unknown commands automatically, silently install host software or promise an Android process stays alive indefinitely.

Remote output can echo secrets even when the app never records typed input. Opt into history deliberately and clear it when needed. App-private storage is not an application-encrypted vault. OSC 52 clipboard copying and inline image decoding are disabled in the terminal adapter; URLs do not launch automatically, and clipboard paste requires an explicit user flow.

Custom project editing, SAF import/export, SFTP synchronization, conflict merging, Mosh, a host runner, backup/restore and managed local Linux are **later gates**, not prerequisites for v1. Experimental models or earlier workspace code do not make those features available.

## Start here

- [Documentation guide](docs/README.md), [roadmap](docs/product/roadmap.md) and conditional [delivery estimate](docs/product/delivery-estimate.md).
- [Dependency/security review](docs/security/dependency-review.md), [notices manifest](docs/security/THIRD_PARTY_NOTICES.md) and [packaged selected-component notices](app/src/main/assets/third_party/THIRD_PARTY_NOTICES.txt).
- [System design](docs/architecture/system-design.md), [technology evaluation](docs/architecture/technology-evaluation.md) and [validation plan](docs/quality/validation-plan.md).
- [Competitive audit](docs/research/competitive-audit.md) and [33-issue catalog](docs/research/issue-catalog.md): researched constraints and product hypotheses, not proven switching demand.
- [Development setup](docs/development.md) and [implementation status](docs/implementation-status.md).

The engineering target is **3–7 focused development days** for the narrow reused-library v1, conditional on library fit, security checks and hardware access. The separate hardening horizon is **6–10 weeks**. These are planning judgments, not measured velocity or guarantees.

## Project layout

```text
app/                   Compose UI, terminal adapter and dependency composition
app/src/main/assets/   Selected-component license/notice texts and provenance
core/model/            Immutable workspace/task/session values (JVM)
core/domain/           Policies, bounded output buffer and repository contracts (JVM)
core/data/             Room persistence and JSch SSH/host-key adapter
gradle/                Pinned versions and Gradle Wrapper
docs/                  Research, product, architecture, security and validation
scripts/               Repository/documentation validation
```

## Build and check

Install JDK 17, Python 3.11+, and the SDK packages in [development setup](docs/development.md), then run:

```sh
python3 scripts/validate_repository.py
./gradlew :core:domain:test :core:data:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest :core:data:assembleDebugAndroidTest
```

With an attached supported device/emulator:

```sh
./gradlew :core:data:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

The source includes real loopback SSH fixtures for host verification/authentication ordering and cancellation, plus domain and persistence checks. Their presence is not a passing result; see the status record and [CI runs](https://github.com/Axoineo/terminal/actions/workflows/ci.yml). No connected Android device was available in the initial environment.

The v1 needs internet access and the selected Android target's appropriate LAN permission for local hosts; denial/revocation must be handled by the app. Do not use a permissive host verifier or legacy crypto fallback to work around an unsupported server. Validate the final manifest and permission route on device.

Research snapshot: **25 November 2026**; implementation direction revised **26 November 2026**. The project-wide license is not selected yet. Upstream component texts are packaged for the selected engines; full final-runtime notice reconciliation and security checks remain explicit work, not blanket compliance/security claims.

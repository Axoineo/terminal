# Terminal Workspace

An Android-first mobile terminal project centered on consistent project files, recoverable work, and accessible interaction. **Terminal Workspace** is a working name; the product hypotheses have not yet been validated with users.

## Start here

- [Documentation guide](docs/README.md): reading order and decisions.
- [Competitive audit](docs/research/competitive-audit.md): 12 apps across four categories, with primary sources.
- [33-issue catalog](docs/research/issue-catalog.md): severity, market need, feasibility, evidence, and bounded remedies.
- [Product requirements](docs/product/product-requirements.md) and [implementation roadmap](docs/product/roadmap.md).
- [System design](docs/architecture/system-design.md), [technology evaluation](docs/architecture/technology-evaluation.md), and [architecture decisions](docs/architecture/decisions/0001-native-android-and-workspace-core.md).
- [Development setup](docs/development.md), [implementation status](docs/implementation-status.md), and [validation plan](docs/quality/validation-plan.md).

## What exists

This is a basic native Android scaffold, **not yet a usable terminal client**:

- Kotlin/Compose application with named workspace creation and Room persistence.
- JVM models and deterministic task-recovery, file-sync planning, and relative-path policies.
- Transactional task updates; repository startup classifies previously unconfirmed running tasks as **completion unknown**, without replaying commands.
- Domain tests and Android persistence/recovery tests.
- Research, solution designs, acceptance criteria, library comparisons, and 26 proposed implementation issues.

SSH/SFTP, a terminal emulator, executable project directories, SAF import/export, editing, output capture, credential storage, backup/restore, and a local Linux environment are **planned, not implemented**. The app requests no network or broad storage permission. Uninstall currently removes its private data; explicit export is a later milestone. Platform backup/transfer is excluded while the journal privacy and export contract is developed.

## Project layout

```text
app/                   Android application, Compose UI, dependency composition
core/model/            Immutable workspace, task, and revision types (JVM)
core/domain/           Recovery, conflict-planning, path policies, repository contracts (JVM)
core/data/             Room database, repositories, Android persistence tests
gradle/                Pinned versions and Gradle Wrapper
docs/research/         Competitive audit and all 33 issues
docs/product/          Requirements and staged implementation roadmap
docs/architecture/     Solution design, technology comparisons, ADRs
docs/quality/          Fault, accessibility, device, and comparative validation
scripts/               Repository/documentation validation
```

The first execution integration will target a user-controlled remote host. Local Linux support has its own execution, distribution, device-support, and maintenance feasibility gate. Terminal/SSH engine candidates are compared in the docs; none is silently selected or vendored.

## Build and check

Install JDK 17, Python 3.11+, and the Android SDK packages described in [development setup](docs/development.md), then:

```sh
python3 scripts/validate_repository.py
./gradlew :core:domain:test :app:assembleDebug :app:lintDebug :core:data:assembleDebugAndroidTest
```

With an attached supported emulator/device:

```sh
./gradlew :core:data:connectedDebugAndroidTest
```

Research snapshot: **25 November 2026**. Evidence, proposals, implemented behavior, and validated results are kept separate. Current verification results are recorded in [implementation status](docs/implementation-status.md). The project license is not selected yet; upstream candidates and their licenses are documented before integration.

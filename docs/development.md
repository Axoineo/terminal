# Development setup

## Toolchain

Versions are pinned in [libs.versions.toml](../gradle/libs.versions.toml) and the Gradle Wrapper. This is a reproducible starting set, not a claim that every pin is the newest release.

| Tool | Pin / requirement |
| --- | --- |
| Java | JDK 17 |
| Repository checks | Python 3.11+ (standard library only) |
| Gradle | 9.5.0, through `./gradlew` |
| Android Gradle Plugin | 9.3.3 |
| Kotlin / Compose compiler plugin | 2.4.21 |
| Android SDK | compile API 37.1; target API 37; minimum API 26 |
| SDK Build Tools | 36.0.0, AGP default |
| Compose BOM | 2026.11.01 |
| Room | 2.8.5 with KSP 2.3.14 |

AGP 9.3 documents Gradle 9.5, JDK 17, Build Tools 36.0.0 and API 37 support. AGP 9 uses built-in Kotlin for Android modules; do not add `org.jetbrains.kotlin.android` alongside it. The JVM modules use `org.jetbrains.kotlin.jvm`. Compose's compiler plugin version matches Kotlin. Kotlin's published compatibility table names AGP through 9.3.1 for this compiler line; this repository's later patch must be judged by actual build results. [AGP compatibility](https://developer.android.com/build/releases/agp-9-3-0-release-notes), [built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin), [Kotlin compatibility](https://kotlinlang.org/docs/gradle-configure-project.html), [Compose setup](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler).

## Install and build

1. Install Android Studio Quail 4 (2026.1.4) or newer compatible with the pinned AGP, or use JDK 17 and Google's SDK command-line tools. See the [API/tool compatibility table](https://developer.android.com/studio/releases).
2. Install Android API 37.1, Build Tools 36.0.0, and Platform Tools in SDK Manager, accepting the applicable SDK terms through that manager. The platform package is `platforms;android-37.1`.
3. Set `JAVA_HOME` to the JDK and configure SDK discovery through `ANDROID_HOME` or an untracked `local.properties` file containing `sdk.dir=/absolute/path/to/sdk`.
4. Open the repository root in Android Studio and sync, or run the commands below. First use needs network access to Google Maven, Maven Central, Gradle plugins, and the Gradle distribution.

```sh
python3 scripts/validate_repository.py
./gradlew :core:domain:test
./gradlew :app:assembleDebug :app:lintDebug :core:data:assembleDebugAndroidTest
```

The pinned Compose libraries require compilation against API 37.1, configured with `compileSdk { version = release(37) { minorApiLevel = 1 } }`. This changes available compile-time APIs; the target remains 37 and the minimum remains 26. See [Android's minor API configuration](https://developer.android.com/build). The wrapper includes a distribution checksum; use it instead of an arbitrary global Gradle installation. [Gradle Wrapper guidance](https://docs.gradle.org/current/userguide/gradle_wrapper.html).

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`. Install it through Android Studio, or use `adb install -r app/build/outputs/apk/debug/app-debug.apk` on a device you intend to use for development. It creates workspace metadata only; it does not connect to a host.

## Checks and their limits

Run Gradle checks in one invocation or sequentially. Separate simultaneous builds in the same checkout can collide on compiler outputs and multiply memory use. The starter limits Gradle to two workers and compiles Kotlin in-process. On a constrained machine, add `--max-workers=1` and close unused development processes.

If a renamed resource exists in source but linking still reports it missing, rebuild the affected module with `:app:clean :app:assembleDebug --no-build-cache` to discard stale generated and cached resource outputs.

| Check | What it establishes |
| --- | --- |
| `python3 scripts/validate_repository.py` | Local Markdown links/anchors, all 33 issue identities, parseable XML/TOML, and expected module files. It does not fetch external links or compile Kotlin. |
| `:core:domain:test` | Task-result uncertainty, no implicit replay transition, clock skew handling, three-way sync planning, and logical-path validation. |
| `:app:assembleDebug` | Android/Kotlin/Compose/Room code generation and APK packaging. |
| `:app:lintDebug` | Static Android checks; it does not prove real-device accessibility. |
| `:core:data:assembleDebugAndroidTest` | Persistence instrumentation test compilation, not execution. |
| `:core:data:connectedDebugAndroidTest` | On-device database reopen/recovery and repeated-observer tests. It does not test an actual killed transport. |

Attach an API 26+ emulator/device before running instrumentation. Read the [validation plan](quality/validation-plan.md) for the broader release matrix and fault suite. Do not present a compiled test APK as a passing device test.

Room schema JSON files in `core/data/schemas/` are versioned source artifacts, not build output. Add explicit migrations and migration tests when schema version changes; do not use destructive migration as an automatic repair.

## Module contracts

`core:model` contains immutable types only. `core:domain` depends on model plus coroutines for repository flows. `core:data` implements those repository contracts with Android Room. `app` composes the repositories and exposes lifecycle-aware Compose state.

Use one application-scoped `RoomJournalRepository`. Its initialization mutex classifies prior-process `RUNNING` records once before journal reads or mutations. It must not be recreated by every screen or used alongside a second independent session owner. Confirmed completion requires evidence from a future trusted execution adapter; a connection event alone is insufficient.

`RelativeWorkspacePath` validates canonical logical strings only. Filesystem adapters must separately enforce containment, safe symlink handling and race resistance. `SyncPolicy` plans from trusted content observations; `null` means confirmed absence, not a failed network read. A plan does not perform I/O or make a final-check/rename sequence atomic.

## Changes and dependency selection

Add transport/emulator adapters only after the [engine spike gate](architecture/decisions/0004-engine-selection-gates.md). Record exact artifact/revision, license notices, Android compatibility, capability limits and measured results. Update pinned dependencies intentionally and run the appropriate build/tests. No SSH credentials, personal SDK paths, build caches or signing material belong in version control.

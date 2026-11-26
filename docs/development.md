# Development setup

The current app is a small SSH terminal: Compose UI and ConnectBot termlib, a JSch password-authenticated PTY, explicit host-key trust, and optional bounded received-output history. The [implementation status](implementation-status.md) records the exact verified revision and what remains untested. The larger editor, file-sync and local-Linux designs are future work.

## Choose the v1 source

The SSH implementation is on **`feat/fast-v1-ssh`** in [PR #1](https://github.com/Axoineo/terminal/pull/1). While it remains open, `main` is the earlier scaffold. Start a v1 checkout with:

```sh
git clone --branch feat/fast-v1-ssh https://github.com/Axoineo/terminal.git
cd terminal
```

After that PR merges, use `main`. Match test/APK evidence to its recorded commit rather than assuming every branch contains the same product.

## Toolchain

Use the [Gradle Wrapper configuration](../gradle/wrapper/gradle-wrapper.properties) and [version catalog](../gradle/libs.versions.toml). These are deliberate pins, not a claim that each version is the newest available.

| Tool or component | Repository setting |
| --- | --- |
| Java | JDK 17 |
| Repository checks | Python 3.11+, standard library only |
| Gradle / Android Gradle Plugin | 9.5.0 / 9.3.3 |
| Kotlin JVM / Compose compiler plugin | 2.4.21 |
| Android SDK | Compile API 37.1; target 37; minimum 26 |
| SDK Build Tools | 36.0.0 |
| Compose BOM | 2026.11.01 |
| Room / KSP | 2.8.5 / 2.3.14 |
| Terminal / SSH | ConnectBot termlib 0.3.15 / mwiede JSch 2.28.8 |
| Loopback test server | Apache MINA SSHD 2.20.1, test scope only |

AGP 9 uses built-in Kotlin for Android modules; do not also apply `org.jetbrains.kotlin.android`. Plain JVM modules use `org.jetbrains.kotlin.jvm`. Keep the Kotlin/Compose compiler pins compatible when updating them. See [AGP compatibility](https://developer.android.com/build/releases/agp-9-3-0-release-notes), [built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin), and [Kotlin compatibility](https://kotlinlang.org/docs/gradle-configure-project.html).

## Install and build

1. Install an Android Studio version compatible with the pinned AGP, or JDK 17 and Google's SDK command-line tools. Configure `JAVA_HOME` for that JDK.
2. Install `platforms;android-37.1`, `build-tools;36.0.0` and `platform-tools` with SDK Manager, and accept the applicable SDK terms there.
3. Configure SDK discovery through `ANDROID_HOME` or an untracked `local.properties` file containing `sdk.dir=/absolute/path/to/sdk`.
4. Open the repository root in Android Studio, or run the checks below. Initial dependency resolution needs access to Google Maven, Maven Central, Gradle plugins and the Gradle distribution.

The compile SDK is configured with `compileSdk { version = release(37) { minorApiLevel = 1 } }`; target and minimum API levels remain separate. CI provisions command-line tools build `15859902` and SDK packages explicitly through the pinned setup action. See the actual [CI workflow](../.github/workflows/ci.yml) for runner setup.

```sh
python3 scripts/validate_repository.py
python3 -m unittest discover -s scripts -p 'test_*.py'
./gradlew :core:domain:test :core:data:testDebugUnitTest \
  :app:assembleDebug :app:lintDebug \
  :app:assembleDebugAndroidTest :core:data:assembleDebugAndroidTest --console=plain
```

Use `./gradlew.bat` on Windows. The wrapper pins the distribution checksum; do not substitute an arbitrary global Gradle installation. Run Gradle invocations sequentially in a shared checkout. Parallel builds can collide on generated outputs and multiply memory use. The repository allows two workers and compiles Kotlin in-process; add `--max-workers=1` on a constrained machine.

The APK is `app/build/outputs/apk/debug/app-debug.apk`. Install it through Android Studio or:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Debug builds from different machines or CI runs may use different signing keys, so Android can reject an update with a signature mismatch. Check the installed build and signer first. Do not automatically uninstall to bypass it: uninstall removes private saved history and trusted host pins, and v1 has no export/restore feature. Retain anything you need before deliberately replacing an installation.

Use a compatible disposable SSH host and verify its fingerprint independently before approving it. This v1 supports password authentication, strict key exchange and ECDSA/RSA-SHA2 host signatures; it does not implement key/MFA authentication, SFTP, Mosh or forwarding. Do not weaken a production server or the client policy to make a test connect. The app requests internet access and the target's local-network permission for LAN hosts; grant/denial/revocation behavior needs its own device checks. Read [SECURITY.md](../SECURITY.md) before collecting or sharing diagnostic output.

## Tests and reports

| Command / task | Scope and limitation |
| --- | --- |
| `python3 scripts/validate_repository.py` | Repository structure, local Markdown links/anchors, 33 issue IDs, XML/TOML and repository contracts; no Android compilation or live vulnerability scan |
| `python3 -m unittest discover -s scripts -p 'test_*.py'` | Offline tests of repository scripts, including audit input/pagination/failure handling |
| `:core:domain:test` | JVM task/path/sync policies and bounded received-output capture |
| `:core:data:testDebugUnitTest` | Real loopback SSH trust/authentication/cancellation tests and host-input validation on the JVM; no Android crypto-provider proof |
| `:app:assembleDebug` / `:app:lintDebug` | APK compilation/packaging and static Android checks |
| `:app:assembleDebugAndroidTest` / `:core:data:assembleDebugAndroidTest` | Instrumentation APK compilation, not execution |
| `:core:data:connectedDebugAndroidTest` | Room persistence, retention, deletion, recovery and migration on an attached device/emulator |
| `:app:connectedDebugAndroidTest` | Activity launch/recreation and packaged JNI terminal smoke tests; no full parser safety or SSH usability certification |

Run both connected suites with an API 26+ emulator/device available to `adb`:

```sh
adb devices
./gradlew :core:data:connectedDebugAndroidTest :app:connectedDebugAndroidTest --console=plain
```

The recorded passing revision is linked from [implementation status](implementation-status.md): 30 JVM tests and seven API 26 emulator tests are distinct evidence, with the exact split and limits recorded there. They do not establish a physical-phone SSH session, TalkBack/braille support, other ABIs or API 37 LAN-permission behavior. Record the commit, device/API, test route and failures for new results.

JVM reports are under `core/domain/build/reports/tests/test/` and `core/data/build/reports/tests/testDebugUnitTest/`. Lint reports are under `app/build/reports/`; connected-test reports are under each Android module's `build/reports/androidTests/`. CI artifacts expire; preserve important sanitized results with their provenance.

## Runtime inventory and security checks

For a dependency change, export the resolved debug binaries and the effective text graph, then query the inventory:

```sh
./gradlew :app:exportRuntimeArtifacts --console=plain
./gradlew :app:dependencies --configuration debugRuntimeClasspath --console=plain > app/build/reports/runtime-dependencies.txt
python3 scripts/audit_runtime_dependencies.py app/build/reports/runtime-artifacts.tsv --output app/build/reports/runtime-osv-report.json
```

The TSV contains resolved public Maven coordinates, artifact filenames and SHA-256 hashes. The Python wrapper sends only coordinates/versions to OSV. Exit codes are `0` for no mapped findings, `1` for findings, and `2` for invalid input, API failure or an incomplete scan. Neither a clean report nor a local artifact hash verifies publisher identity, vendored native code or package-unmapped advisories. Review the [dependency/security record](security/dependency-review.md) and [notices manifest](security/THIRD_PARTY_NOTICES.md) alongside the report. MINA is a JVM test peer and must remain absent from the app runtime.

With `ANDROID_HOME` configured, inspect the built APK as CI does:

```sh
"$ANDROID_HOME/build-tools/36.0.0/aapt2" dump permissions app/build/outputs/apk/debug/app-debug.apk
"$ANDROID_HOME/build-tools/36.0.0/zipalign" -c -P 16 -v 4 app/build/outputs/apk/debug/app-debug.apk
```

The CI workflow also checks packaged notice bytes against source assets. ZIP alignment is separate from ELF alignment and running on a physical 16 KiB-page device. Repeat the appropriate native, ABI and notice checks after changing terminal/native dependencies; keep the result's coverage explicit.

## Module and data contracts

- `core:model`: immutable workspace/task/session values.
- `core:domain`: deterministic policies, bounded output capture and repository contracts.
- `core:data`: Room storage plus the JSch connection and private host-key adapter.
- `app`: Compose screens, terminal adapter, connection ownership and dependency composition.

One `SshClient` owns one connection attempt. Collect its bounded output once and bind it to that attempt's immutable history identity; stop/join the old producer before resetting the terminal. Keep blocking SSH/socket work off the main thread, and retire native terminal resources only after their UI generation detaches. A PTY is a byte stream, not verified per-command exit evidence.

The application-scoped Room repositories reconcile previously live records when reopened. Received-output capture is opt-in and bounded; it must not become a keystroke/password recorder. Current task/workspace policies remain foundations for later work, not evidence of an implemented editor or synchronization feature.

The current Room schema is version 2, with an explicit v1→v2 migration. Commit generated schema JSON in `core/data/schemas/`. For a schema change, add a migration and exercise it with nonempty existing data through connected tests; never replace that work with destructive fallback. Preserve old schema files and evidence needed to verify the upgrade.

For dependency updates, change pins intentionally, inspect transitive/native components and current advisories, retain exact license texts, and update the relevant evidence. Keep secrets, personal SDK paths, signing material and caches out of version control. The project-wide license remains undecided; component notices do not choose it.

If stale generated resources cause a missing-resource error after a rename, rebuild the affected module with `./gradlew :app:clean :app:assembleDebug --no-build-cache`. Use this for that failure, rather than routinely discarding caches before every check. Broader fault/device acceptance criteria live in the [validation plan](quality/validation-plan.md).

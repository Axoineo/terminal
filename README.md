<h1 align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/brand/terminal-workspace-dark.svg">
    <img src="docs/brand/terminal-workspace-light.svg" alt="Terminal Workspace" width="520">
  </picture>
</h1>

<p align="center">
  <strong>An Android SSH terminal that keeps the output you choose.</strong><br>
  Connect to your server, work in a terminal, and reopen the output you saved.
</p>

<p align="center">
  <a href="#project-status"><img src="https://img.shields.io/badge/status-early_preview-0F766E" alt="Status: early preview"></a>
  <a href="#what-you-need"><img src="https://img.shields.io/badge/Android-8.0%2B-34A853?logo=android&amp;logoColor=white" alt="Android 8.0 or newer"></a>
  <a href="gradle/libs.versions.toml"><img src="https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?logo=kotlin&amp;logoColor=white" alt="Kotlin 2.4.21"></a>
  <a href="https://github.com/Axoineo/terminal/actions/runs/45868354385"><img src="https://img.shields.io/badge/verified_build-1ed783d-334155?logo=github" alt="Passing verification for source revision 1ed783d"></a>
</p>

<p align="center">
  <a href="#quick-start">Quick start</a> ·
  <a href="#build-from-source">Build</a> ·
  <a href="docs/product/roadmap.md">Roadmap</a> ·
  <a href="docs/README.md">Docs</a> ·
  <a href="CONTRIBUTING.md">Contributing</a> ·
  <a href="SECURITY.md">Security</a>
</p>

---

## About

Terminal Workspace is a small Android SSH client built around a useful first path:
connect, verify the server, use the terminal, and keep a bounded record of received
output when you need it. The shell and development tools run on your server.

The app reuses [ConnectBot termlib](https://github.com/connectbot/termlib) for terminal
emulation and rendering, and [mwiede JSch](https://github.com/mwiede/jsch) for SSH.
Kotlin, Jetpack Compose and Room provide the Android interface and local history.
Terminal Workspace is a working name.

| Available in the preview | What it does |
| --- | --- |
| **Interactive SSH terminal** | Password login, keyboard input, resize, Ctrl-C and Tab. |
| **Explicit server trust** | Show a SHA-256 host-key fingerprint before authentication; pin approved keys and block changed keys. |
| **Optional saved output** | Keep the first 256 KiB received per session, up to 20 sessions. Reopen or delete saved output. |
| **Deliberate paste and reconnect** | The app's Paste… action previews clipboard text. Connect explicitly; commands are never replayed automatically. |

## Project status

> [!NOTE]
> **Early preview — `0.1.0-dev`.** The SSH implementation currently lives on
> [`feat/fast-v1-ssh`](https://github.com/Axoineo/terminal/tree/feat/fast-v1-ssh)
> in [PR #1](https://github.com/Axoineo/terminal/pull/1). Until it is merged, `main`
> contains the earlier scaffold. Test builds are available through CI.

The [verified build](https://github.com/Axoineo/terminal/actions/runs/45868354385)
passed 30 JVM tests, seven tests on an Android 8 emulator, lint and APK packaging
checks. Its 99 resolved runtime artifacts were checked against OSV with zero mapped
findings. These results apply to source revision `1ed783d`; they are not a security
certification or a physical-phone SSH compatibility result. The
[implementation status](docs/implementation-status.md) records the evidence and limits.

**Next:** physical-phone and server testing, then key authentication. Mosh, SFTP,
project editing, synchronization and local Linux remain later work in the
[roadmap](docs/product/roadmap.md).

## What you need

- **Android 8.0 / API 26 or newer.** Internet access; local-network permission when
  connecting to LAN hosts on Android 17 / API 37 or newer.
- **An SSH server you control or are allowed to use.** This preview supports password
  authentication, strict key exchange, and an ECDSA or RSA-SHA2 host key.
- **The server's fingerprint from a trusted source.** Compare it before approving the
  first connection.

Key authentication, keyboard-interactive MFA and Ed25519-only host configurations
are outside the current supported route. Use a compatible test host; keep your
server's existing security policy intact.

## Quick start

### Get a test build

1. Open the [CI runs](https://github.com/Axoineo/terminal/actions/workflows/ci.yml) and
   choose a successful run for the preview revision you want to test.
2. Download **`android-debug-and-reports`**, unzip it, and install **`app-debug.apk`**
   on your test device. GitHub sign-in is required to download artifacts.
3. If the artifact has expired, [build from source](#build-from-source).

Artifacts are retained for seven days. They are debug builds, not a store release.
The [verification record](docs/security/evidence/2026-11-26/README.md) includes the
known tested APK's SHA-256 fingerprint.

### Make a connection

1. Enter the hostname, port, username and password. Choose **Save received output on
   this device** if you want history; recording starts **off**.
2. Tap **Connect**. Compare the host-key fingerprint with your trusted source, then
   approve it to continue. An existing pin that changes is blocked.
3. Work in the terminal. Use **Paste…** to review clipboard text, and **History** to
   reopen saved output. Disconnect explicitly when you finish.

Saved history is read-only received output, not a complete command log or a resumed
server session. Output not yet flushed may be lost if Android stops the app. Saving
normally runs about once per second; scheduling delays or storage failures can widen
that gap.

## Privacy and security

Passwords are not persisted, and typed input is not recorded separately. A remote
program can still echo secrets into output. Saved history and host-key pins live in
app-private storage; there is no separate application-level encryption. Backups are
disabled, and uninstalling the app removes its private data.

Remote OSC 52 clipboard writes and inline images are disabled. Remote output never
opens links automatically. The app's **Paste…** action requires review and send.
An IME can insert clipboard text as ordinary input without that preview.

Read [SECURITY.md](SECURITY.md) for reporting instructions and trust boundaries, and
the [dependency review](docs/security/dependency-review.md) for exact versions,
upstream fixes, artifact hashes and remaining verification work.

## Build from source

Install **JDK 17**, **Python 3.11+**, and the Android SDK packages in the
[development guide](docs/development.md): API **37.1**, Build Tools **36.0.0**, and
Platform Tools. Configure `JAVA_HOME` and `ANDROID_HOME` or an untracked
`local.properties` file.

```bash
git clone --branch feat/fast-v1-ssh https://github.com/Axoineo/terminal.git
cd terminal
./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. To install it on a
connected development device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Run the lightweight repository check with `python3 scripts/validate_repository.py`.
The [contribution guide](CONTRIBUTING.md#run-checks-that-match-the-change) gives the check commands
for code, persistence, terminal and dependency changes. Use the checked-in Gradle
Wrapper; a global Gradle installation is unnecessary.

## Documentation

| Read this | To learn |
| --- | --- |
| [Implementation status](docs/implementation-status.md) | What exists, what passed and what remains unverified. |
| [Roadmap](docs/product/roadmap.md) | The small SSH v1 and the gates for later features. |
| [Development guide](docs/development.md) | Toolchain, build, test and dependency-audit commands. |
| [Architecture](docs/architecture/system-design.md) | Session lifetime, host trust, storage and recovery boundaries. |
| [Competitive research](docs/research/competitive-audit.md) | The app audit, with a [33-issue catalog](docs/research/issue-catalog.md) of product gaps and constraints. |
| [All documentation](docs/README.md) | Product requirements, decisions, estimates and validation plans. |

## Contributing

Bug reports, device-testing results and focused pull requests are welcome. Start with
[CONTRIBUTING.md](CONTRIBUTING.md) for setup, scope and validation expectations.
Report suspected vulnerabilities using [SECURITY.md](SECURITY.md), keeping sensitive
details out of public issues.

## License

A project-wide license has not been selected yet. Third-party components retain
their own licenses; see the [notices inventory](docs/security/THIRD_PARTY_NOTICES.md)
and the texts packaged in the app's **Notices** screen.

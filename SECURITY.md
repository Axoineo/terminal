# Security

This project is an early Android SSH terminal prototype. This policy describes how
to report a vulnerability and the security boundaries of the current app.

## Reporting a vulnerability

Please [report a vulnerability privately through GitHub](https://github.com/Axoineo/terminal/security/advisories/new).
You can also open the repository's Security tab and choose **Report a vulnerability**.
Private vulnerability reporting was enabled and verified on 26 November 2026.
Keep exploit details, affected server addresses, session output, credentials and
other sensitive material out of public issues and pull requests.

In the private report, include:

- The app version or commit, Android version and device or emulator architecture.
- Reproduction steps using a server or test environment you are authorized to use.
- What happened, what you expected, and the likely security impact.
- A small sanitized example, relevant logs or a proposed fix, if available.

Review terminal output and logs for passwords, tokens, private hostnames and personal
data before sharing them. Do not send real credentials or private keys. Response
times depend on maintainer availability; this project has no published response or
fix deadline.

Ordinary bugs and feature requests belong in
[public issues](https://github.com/Axoineo/terminal/issues).

## Supported scope

Best-effort security maintenance targets the latest active SSH preview, currently
developed on [feat/fast-v1-ssh](https://github.com/Axoineo/terminal/tree/feat/fast-v1-ssh)
with app version `0.1.0-dev`. Until that work is merged, `main` contains the earlier
transport-free scaffold. The SSH behavior described below refers to the preview.
Historical commits and CI APKs do not have a separate maintenance, backport or
response-time commitment. Include your exact branch and commit in a report.

The implemented app connects to remote servers over SSH with password
authentication. A local Linux runtime, file synchronization, imported-key storage
and a production release are outside the current verified scope. Proposed features
in design documents do not establish a security guarantee for the app.

## Trust and privacy boundaries

**Verify the server on first connection.** The app asks before trusting a new host
key and stores the accepted key for that host and port. Compare the displayed
SHA-256 fingerprint through an independent trusted channel. Trust on first use
cannot detect an attacker already present during that first connection. A changed
key fails closed; investigate it with the server administrator before changing
local trust data.

**Authentication has a narrow compatibility policy.** The current backend requires
strict key exchange and an ECDSA or RSA-SHA2 host key. It does not silently enable
older algorithms to connect to an incompatible server. Host certificates,
keyboard-interactive authentication and imported private keys are not supported in
this version.

**Passwords are transient, and server output can still contain secrets.** The app
does not persist passwords or separately record typed input. This does not promise
perfect erasure of managed memory, or prevent a remote program from echoing a
password or token into terminal output.

**Recording is optional local storage.** Received-output capture is off by default.
When enabled for a connection, it retains the first 256 KiB of received bytes and
keeps at most 20 sessions in app-private Room storage. Saved sessions can be deleted.
Host-key pins and saved output rely on Android's app sandbox; there is no separate
app-level database encryption or forensic secure-erasure guarantee. The manifest
disables backup and its extraction rules exclude app data from cloud backup and
device transfer. This does not protect data on a compromised or unlocked device.

**Remote terminal output is untrusted input.** The app uses existing SSH and terminal
libraries, including native code. Remote clipboard writes through OSC 52 have no
clipboard callback, inline images are off, and remote output does not automatically
open links. These controls do not make parsing arbitrary output risk-free. Clipboard
input through the app's **Paste…** control requires review and an explicit send.
An IME may commit clipboard text directly as ordinary input without that preview.
Pasted newlines may execute commands on the server.

**A network connection is not a process-survival guarantee.** Android can terminate
the app or its connection. Saved received output is a bounded record, not a complete
command audit, proof of remote execution, or a way to resume a remote process.

## Verification and remaining limits

The [dependency and trust-boundary review](docs/security/dependency-review.md) records
selected upstream fixes, algorithm policy, dependency fingerprints, native checks
and their limits. The [preserved CI evidence](docs/security/evidence/2026-11-26/README.md)
includes 99 resolved runtime artifacts with zero mapped OSV findings, 30 JVM tests
and seven Android API 26 x86_64 emulator tests. Scanner results do not cover every
advisory, vendored component or app integration flaw.

Physical-device SSH, IME behavior, API 37 local-network permission, TalkBack, other
ABIs and OEM lifecycle behavior have not yet been exercised by that evidence.
Publisher-signature verification and reproducible source-to-binary correspondence
are also not established. These checks describe a debug prototype, not a security
certification or a completed production audit.

Third-party license texts and attributions are available from the app's Notices
screen and the [notices inventory](docs/security/THIRD_PARTY_NOTICES.md).

# ADR 0004: Select engines through measured Android spikes

Date: 25 November 2026. Status: **Accepted selection process; prototype engines adopted 26 November 2026**.

## Prototype adoption — 26 November 2026

The SSH preview adopts **ConnectBot termlib 0.3.15** and **mwiede JSch 2.28.8**.
The [dependency review](../../security/dependency-review.md) records the exact
artifacts, licenses and security findings; [implementation status](../../implementation-status.md)
records the passing prototype checks and remaining physical-device/server tests.
The decision below preserves the original candidate-selection rationale. Broader
comparative, accessibility, performance and interoperability gates remain open.

## Context

Parser correctness, Android IME/selection, crypto providers, JNI packaging and licenses matter more than a library popularity comparison. A basic project should not imply its eventual transport/rendering dependencies have been validated.

## Initial decision — 25 November 2026

Spike ConnectBot termlib first, compare isolated Termux terminal modules, and retain an adapter boundary. Termlib is Apache-2.0 with MIT libvterm; Termux's repository is GPLv3-only with explicit Apache-2.0 exceptions for terminal-view/emulator. Audit selected code and dependencies rather than applying a repository-wide shortcut. [termlib](https://github.com/connectbot/termlib), [Termux license](https://github.com/termux/termux-app/blob/master/LICENSE.md).

Spike maintained `mwiede/jsch` first for Android SSH/SFTP and compare SSHJ. Consider cbssh after a maturity/API/security review. These are candidates, not guarantees of Android compatibility. MINA remains an alternative with an explicit upstream Android support limitation. No engine dependency is added before a working tested slice. [JSch](https://github.com/mwiede/jsch), [SSHJ](https://github.com/hierynomus/sshj), [cbssh](https://github.com/connectbot/cbssh), [MINA Android statement](https://github.com/apache/mina-sshd/blob/master/docs/android.md).

## Required evidence

- License/notice and transitive dependency record for exact revisions; current vulnerability check.
- Terminal comparison with one corpus and equal scrollback limits: IME, accessibility, wide/combining characters, full-screen TUIs, paste, resize, output flood, mouse/touchpad and malformed escapes.
- SSH/SFTP verification: host keys, auth/MFA, Android crypto/signing, cancellation, rekey, interruption and ambiguous rename recovery.
- Measured CPU/memory/frame/battery results on phone and tablet, and native ABI/package checks where applicable.

## Consequences

The product architecture is committed while risky engines remain replaceable. A spike may change the preferred candidates without changing workspace/save/journal guarantees. No performance winner or cross-version support promise is made before measurements. See [technology evaluation](../technology-evaluation.md) for alternatives and full gates.

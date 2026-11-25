# Technology evaluation

Reviewed 25 November 2026 using primary project and platform sources. “Preferred spike” means the first candidate to test, not a performance or compatibility claim. No terminal, SSH, SFTP or local-runtime dependency is included merely because it appears here. Dependency versions belong in the repository's pinned build configuration and are rechecked when selected.

## Application platform

| Option | Fit for this product | Decision |
| --- | --- | --- |
| Kotlin + Jetpack Compose | Direct Android lifecycle, IME, accessibility, SAF, Keystore and service integration; Kotlin domain rules are JVM-testable | Accepted for Android shell/workspace UI |
| Traditional Android View terminal embedded in Compose | Reuses an engine whose input/rendering behavior is already useful; requires deliberate focus and accessibility integration | Valid adapter option |
| Flutter | Can provide a shared UI, but terminal input, JNI, SAF and services still need platform integration; iOS is optional today | Defer; no measured claim that Flutter is inherently slower |
| WebView + xterm.js | Mature web terminal semantics and possible web reuse; adds JS/native message, selection, focus and accessibility boundaries on Android | Reserve for comparative spike if native candidates fail; not the default product shell |
| Full custom terminal parser/renderer | Maximum control but duplicates a large compatibility and security surface before proving the workflow | Reject for initial development |

Native Kotlin is an architectural fit decision, not a universal framework ranking. Compose is Android's recommended modern UI toolkit. Keep the terminal renderer behind an adapter, throttle screen invalidation, and avoid rebuilding immutable full scrollback lists on every byte. [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations).

Room is accepted for structured workspace/journal persistence because its query checking and migrations reduce routine database mistakes. Use a version compatible with the pinned Kotlin/KSP/AGP toolchain; newer documentation may target another major package. Prefer Room transactions for small durable records. Large output chunks belong in a bounded design measured separately. Preferences may later use DataStore; credentials do not belong in either ordinary preference values or journal rows. [Room documentation](https://developer.android.com/training/data-storage/room).

## Terminal engine shortlist

| Candidate | Verified license/scope | Advantages | Costs and gate | Position |
| --- | --- | --- | --- | --- |
| ConnectBot `termlib` | Apache-2.0 component; libvterm dependency identified as MIT | Existing Compose component; JNI-backed parser; selection, resize, complex scripts and graphics documented | JNI ABI/package maintenance; touchpad/TUI/accessibility regressions must be retested; readme feature lists are not conformance tests | **Preferred first spike**, aligned with native Compose |
| Termux `terminal-emulator` + `terminal-view` | Repo GPLv3-only **with explicit Apache-2.0 exceptions for these libraries** | Established Android terminal implementation; alternative to a JNI parser integration | Audit selected files and all copied/shared dependencies; View/Compose adapter; full app fork has different licensing and scope | **Required comparison candidate** |
| Upstream libvterm with own thin Android bridge | MIT; emulator library, not a complete Android terminal UI | Parser/screen state can be independent of UI | Need renderer, IME, scrollback, shaping, selection and JNI lifecycle; greater owned code than termlib | Fallback if component fails gates |
| xterm.js/WebView | MIT in upstream license | Existing web parser/rendering ecosystem | Android keyboard, accessibility and bridge proof required; JS/native security boundary | Secondary comparison, no initial dependency |

Sources: [ConnectBot termlib and embedded-library notice](https://github.com/connectbot/termlib), [Termux license exceptions](https://github.com/termux/termux-app/blob/master/LICENSE.md), [libvterm upstream](https://www.leonerd.org.uk/code/libvterm/), [xterm.js license](https://github.com/xtermjs/xterm.js/blob/master/LICENSE).

Do not copy the whole Termux application under the assumption that its component exception licenses all application/shared code. Equally, do not reject its isolated terminal modules simply because the repository's main license is GPL. Record exact revisions, file notices, modifications and transitive artifacts before incorporation. The archived Neovim libvterm fork is not an appropriate assumption for the canonical maintained upstream; its archive notice points to Neovim's vendored copy. [Archived fork notice](https://github.com/neovim/libvterm).

JNI is acceptable where an engine earns its cost. Keep one explicit ownership boundary, serialize access to parser state, bound incoming data, batch byte feeds and damage updates, and close native handles deterministically. Avoid a JNI transition per cell/key/render operation. Native crash isolation, fuzz tests, sanitizer builds, ABI coverage and Android native packaging checks become required release work. Android recommends minimizing marshalling and keeping the JNI surface small. [JNI guidance](https://developer.android.com/ndk/guides/jni-tips).

### Terminal decision experiment

Use the same recorded byte streams and real sessions for both preferred candidates. Cover UTF-8 split reads, combining/wide emoji, Arabic/CJK/Indic shaping, resize and reflow, alternate screen, selection during output, bracketed paste, IME composition, TalkBack, Bluetooth/USB keys, mouse/touchpad reports, OSC 52, malformed sequences and output floods. Compare memory at an equal scrollback cap, startup latency, sustained throughput, frame jank and battery on a modest phone and tablet. Measure; neither native code nor a popular app proves a winner. Open ConnectBot reports show why a component demonstration is insufficient: [TUI wheel #2351](https://github.com/connectbot/connectbot/issues/2351), [touchpad #2549](https://github.com/connectbot/connectbot/issues/2549).

## SSH and SFTP shortlist

| Candidate | Verified main license | Fit and limitations | Position |
| --- | --- | --- | --- |
| `mwiede/jsch` | BSD-style, three redistribution conditions | Maintained fork of JSch; compact familiar shell/SFTP API; algorithm availability depends on runtime/provider | **Preferred first Android transport spike**, conditional on crypto/Keystore/cancellation tests |
| SSHJ | Apache-2.0 | Java shell, exec and SFTP; known-host and keyboard-interactive support; current upstream flags historic vulnerable releases; Android crypto-provider integration still must be tested | **Required comparison candidate**; credible alternative |
| ConnectBot `cbssh` (formerly `sshlib`) | Apache-2.0 | Kotlin coroutines, explicit protocol state, PTY/SFTP/forwarding, JVM 17 target; newer library/rewrite increases review and compatibility work | Promising Android-oriented candidate; maturity/security gate before adoption |
| Apache MINA SSHD | Apache-2.0, **not BSD** | Broad client/server stack; upstream explicitly says Android is not an actively supported/tested goal | Defer as default; only select if Android spike demonstrates a needed advantage |
| libssh2 | BSD-style, three conditions | C SSH/SFTP option with explicit JNI/native crypto packaging costs | Defer absent a proven gap in JVM candidates |

Primary sources: [mwiede JSch README](https://github.com/mwiede/jsch) and [license](https://github.com/mwiede/jsch/blob/master/LICENSE.txt); [SSHJ features/security notice](https://github.com/hierynomus/sshj) and [license](https://github.com/hierynomus/sshj/blob/master/LICENSE); [cbssh features/license](https://github.com/connectbot/cbssh); [MINA Android support](https://github.com/apache/mina-sshd/blob/master/docs/android.md) and [license](https://github.com/apache/mina-sshd/blob/master/LICENSE.txt); [libssh2 license](https://libssh2.org/license.html).

These licenses cover main projects, not every optional crypto provider, compressed library or bundled binary. Create a dependency/SBOM record and check each actual release's notices and security advisories before integration. Do not enable SHA-1/DSA or weak algorithms globally to make one old appliance connect. Any legacy profile is explicit, narrowly scoped and explained to that user.

### Transport decision experiment

Require OpenSSH and Dropbear interoperability; unknown/changed host keys and hashed known-host entries; Ed25519 and RSA-SHA2; passphrase-protected OpenSSH keys; password and keyboard-interactive MFA; rekey under load; cancellation and resource cleanup; PTY resize; arbitrary remote filenames; SFTP extension discovery; disconnect during upload/rename; and network handoff. Verify Android minimum API, desugaring and provider behavior with the selected artifact. A JVM 17 build requirement alone does not prove Android runtime support.

Prove that signing can use the desired Keystore-backed key through an adapter, or document encrypted software-key limitations. Record unsupported certificate/FIDO/proxy/jump features as capabilities; do not add UI toggles that imply support. Host verification is mandatory in every candidate; permissive sample verifiers are never production defaults.

## PTY, tracked execution and resilience

| Mechanism | What it provides | What it cannot promise |
| --- | --- | --- |
| SSH interactive PTY | Bytes, terminal modes, resize and an interactive shell | Per-command outcomes, durable replay or accurate cwd without integration |
| SSH exec channel | Separate command stream and possible exit-status event | Completion knowledge if the channel is lost; exactly-once side effects |
| tmux/screen | Host-side session persistence and history while that host session survives | Client-side tunnel survival or logs across every server restart |
| Mosh | Roaming interactive screen transport with host component/UDP | Full historical output, ordinary SFTP/forwarding, immunity to host death |
| Cooperating task runner | Queryable task IDs, captured output and optional deduplication | Universal exactly-once semantics for arbitrary external effects |

Use SSH first, support explicit multiplexer attachment, and add Mosh only after target workflows show it improves reliability enough to justify another transport/server/license surface. No automatic server install or dotfile mutation. [SSH channel semantics](https://www.rfc-editor.org/rfc/rfc4254), [Mosh project](https://mosh.org/), [ConnectBot practical Mosh constraints](https://connectbot.org/guides/mosh/).

## Local execution alternatives

| Option | Value | Why deferred |
| --- | --- | --- |
| Minimal packaged PTY backend | Small local tools and offline shell access | Native execution, modern target SDK, PTY/process-tree and distribution proof needed |
| Full Termux fork/package ecosystem | Rich Android-adapted userland | Significant maintenance, licensing, packaging and ecosystem commitment; weak differentiation alone |
| PRoot distro wrapper | Familiar distro filesystem/packages | Additional syscall/process overhead and kernel feature limits; does not escape Android lifecycle |
| Android AVF Linux VM | Real guest Linux kernel on supported devices | Availability/device/service contract is outside an ordinary app's universal control |
| Remote host | Existing full Linux/developer environment | Requires reachability and user-controlled host; first useful execution route |

The optimal staged choice is remote execution plus offline local drafts/history. It is not a claim that local environments are unimportant. A local runtime becomes a separate approved product investment after proof gates, rather than an untested dependency concealed inside a basic project. [Modern Android executable-file restriction](https://developer.android.com/about/versions/10/behavior-changes-10#execute-permission).

See [system design](system-design.md), [ADR 0001](decisions/0001-native-android-and-workspace-core.md) and [ADR 0004](decisions/0004-engine-selection-gates.md).

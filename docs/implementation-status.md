# Implementation status

Source/status review: **26 November 2026**. The scope is now the fast reused-library SSH v1. This record distinguishes current source, earlier foundation checks and validation still pending for the new integration. Do not transfer a previous scaffold's passing build into a claim that new SSH/UI changes have passed.

## Current source

| Area | Implemented source / integration status | Boundary |
| --- | --- | --- |
| Terminal engine | Pinned ConnectBot termlib 0.3.15; Compose adapter owns a native emulator through a retained session owner | No custom parser/renderer; device IME, resize, lifecycle and accessibility behavior still need testing |
| SSH | Pinned mwiede JSch 2.28.8; one-shot client per attempt, password auth, raw PTY stream, resize/send/disconnect and cancellation cleanup | No SFTP/Mosh, key/MFA auth, forwarding, host bootstrap or automatic reconnect/replay |
| Host trust | App-private pins, explicit SHA-256 TOFU approval, changed-key/algorithm rejection, atomic pin persistence before auth, damaged-store rejection | Does not advertise host-certificate/CA support or automatic key replacement; real handshake/device verification remains separate |
| Crypto policy | Strict key exchange required; bounded configured RSA-SHA2/ECDSA route and modern cipher/MAC set; no certificate-to-key fallback | Unsupported servers/providers must fail explicitly; no claim that all host algorithms work on Android |
| History | App-private Room session metadata/output, 256 KiB received-output cap, 20-session retention and deletion; bounded prefix buffer with truncation state | UI capture is intended default off per attempt; echoed remote secrets remain possible; no typed-input recorder, complete remote transcript or per-command result inference |
| UI integration | Being integrated with the real terminal/SSH route and optional history | Check the final source/CI record before claiming the app's end-to-end route works; no real-device pass recorded here |
| Existing foundation | Workspace/task values, recovery/conflict/path policies and Room repositories remain | These models are not a working project editor or synchronization engine |
| Selected-component notices | Complete text copies for termlib/Apache-2.0, vendored libvterm/MIT and width notice, JSch/JZlib/jBCrypt are in app assets, with provenance metadata | Final APK inclusion and transitive/runtime inventory reconciliation remain pending |

## Not in v1

No custom project file editor, SAF import/export route, SFTP conflict merge/sync, full received-output search/index, shell-semantic task tracking, multiplexer/host runner, Mosh, verified backup/restore or managed Linux environment is claimed. The [roadmap](product/roadmap.md) separates those later decisions from the narrow v1.

## New-integration verification record

| Check | Current recorded result | Evidence / limitation |
| --- | --- | --- |
| termlib source fix selection | **Reviewed** | 0.3.15 tag includes the known CSI overflow fixes; exact commits and source evidence in [dependency review](security/dependency-review.md) |
| Pinned-source native regression | **Passed on host** | Three CSI cases compiled with ASan/UBSan exited 0; leak checking blocked by ptrace and disabled for the successful run. Not execution of packaged Android `.so` files. |
| Artifact/ABI inspection | **Partial evidence obtained** | Inspected termlib AAR SHA-256 and four ABI LOAD alignments recorded; final APK packaging/device checks and publisher signature verification are separate |
| JSch known-advisory triage | **Reviewed for named findings** | Selected 2.28.8 is newer than the upstream 2.28.6 revocation fix and includes later bounds hardening; this is not an exhaustive dependency scan |
| Real loopback SSH tests | **Source added; execution result pending** | Seven controlled-server scenarios cover refusal, approval/persistence-before-auth, raw PTY echo, changed key, port scope, persistence failure, damaged pins and cancellation; count by test methods rather than subcases |
| New domain/history tests | **Source added; execution result pending** | Bounded output/recovery tests must run against this exact revision |
| New Android build/lint/instrumentation compilation | **Pending refreshed result** | Current changes require a fresh CI/build record; earlier scaffold results below do not cover them |
| Resolved runtime/build-plugin graph and complete advisory dispositions | **Pending integration output** | Top-level version pinning does not enumerate embedded/native or transitive code |
| Final packaged notices/permissions | **Pending refreshed inspection** | Asset source exists; inspect actual APK contents and network/LAN permission behavior |
| Android end-to-end SSH, IME, TalkBack/braille and lifecycle | **Not run on device in this environment** | No attached device/emulator; host lacks `/dev/kvm` |
| User switching demand / comparative field use | **Not validated** | Research hypotheses remain hypotheses |

The backend's duplicate coroutine cancellation-handler registration found during source review was corrected before this record; real loopback execution is still needed. Shared output across attempts is prevented by one-shot clients and caller-owned immutable session identity; final UI integration must preserve that boundary.

## Historical foundation results

The earlier transport-free scaffold was checked with JDK 17, Gradle 9.5.0, AGP 9.3.3, Kotlin 2.4.21 and compile SDK 37.1 (target API 37, minimum API 26). Its recorded results were: repository validation passed; **16 domain tests passed**; Android debug build passed; lint passed with **0 errors/5 update warnings**; persistence instrumentation compiled; APK had no internet/broad-storage permission. Those are historical results for that limited source and are superseded by new network/UI/schema changes until refreshed.

Connected instrumentation and assistive-technology checks were not run. Hosted CI initially needed explicit SDK-tool provisioning; current run results are tracked in [Actions](https://github.com/Axoineo/terminal/actions/workflows/ci.yml). See [development setup](development.md) for commands and [validation plan](quality/validation-plan.md) for device/fault gates.

When CI supplies a new result, record exact revision, command, result and limits here. A compiled test APK is not a device pass, a loopback JVM peer is not Android provider compatibility, and a copied license bundle is not a complete final-runtime security or license audit.

# Implementation status

Verified code revision: **`1ed783d5eb171fb9addedaa7acd2bf94ab953f73`**, 26 November 2026. [Both CI jobs passed](https://github.com/Axoineo/terminal/actions/runs/45868354385). Subsequent documentation-only changes do not alter this APK.

## Implemented first slice

| Area | Current behavior | Boundary |
| --- | --- | --- |
| Terminal | ConnectBot termlib 0.3.15 supplies parsing, native emulation, rendering and input | Fresh engine per connection; no custom parser or local Linux environment |
| SSH | JSch 2.28.8 supplies a password-authenticated PTY, input, resize and disconnect | One attempt per client; no key/MFA authentication, forwarding, SFTP or Mosh |
| Host trust | Explicit SHA-256 fingerprint approval, private atomic pins before authentication, rejection of changed keys/types or damaged pins | Independent fingerprint verification is the user's responsibility; no automatic key replacement |
| Crypto | Strict key exchange, RSA-SHA2/ECDSA host signatures, modern AES/SHA2 algorithms | Older servers and Ed25519-only configurations are outside this first supported route |
| History | Capture starts off; optionally save the first 256 KiB received per session, latest 20 sessions, with truncation and deletion | Remote output can echo secrets; no input recorder, complete remote transcript or inferred command results |
| Recovery | Saved output survives reopening; previously live session records become interrupted | Unflushed output may be lost; normal approximately one-second flush scheduling is not a durability bound. No automatic reconnect/replay or guaranteed process lifetime. |
| UI | Connection form, host approval, terminal, app Paste… review, Ctrl-C/Tab, read-only history and notices | IMEs may commit clipboard text without the app's paste preview; phone IME, accessibility and network usability still require field testing |
| Existing foundation | Workspace/task values, conflict/path policies and Room repositories retained | No working editor, file synchronization, host runner or backup feature |

## Verification

All results below apply to the verified revision above.

| Check | Result | Practical limit |
| --- | --- | --- |
| Repository links, issue catalog, XML/TOML and Python audit contracts | **Passed**; five Python contract tests | Contract tests exercise the OSV wrapper, not vulnerability absence |
| Domain policies and bounded output | **20 JVM tests passed** | Pure policies, not Android network behavior |
| SSH security/configuration | **10 JVM tests passed** | Seven real loopback server tests plus three host-validation tests; verifies pre-auth trust, pin persistence, changed keys, raw echo, cancellation and invalid input |
| Debug APK, lint, both Android test APKs | **Passed; zero lint errors** | Update/unused-resource notices remain; debug signing is for testing |
| Android persistence | **5 tests passed** on API 26 x86_64 emulator | Includes journal recovery, received-byte recovery, retention, deletion and v1→v2 migration |
| Android app/native terminal | **2 tests passed** on that emulator | Launch/recreation plus packaged JNI CSI, split UTF-8 and engine replacement smoke test; not a parser memory-safety certification |
| Resolved binary inventory and live OSV check | **99 external artifacts queried; zero mapped findings** | Native/vendored and package-unmapped advisories reviewed separately; no publisher-signature or vulnerability-absence claim |
| APK permission inspection | Internet, local-network permission, AndroidX's own signature permission only | No broad-storage permission; API 37 LAN grant/denial needs field testing |
| APK native packaging | **16 KiB ZIP alignment passed** | Earlier inspection of the same native dependencies found all eight ELF files aligned to 16 KiB with non-executable stacks; physical 16 KiB-page devices remain untested |
| Packaged notices | **All 15 asset files match source bytes** | Includes 13 license/attribution texts; final release inventory still needs reconciliation |
| Pinned-source parser regression | **Three host ASan/UBSan cases passed** | Leak checking was unavailable; this is separate from the packaged Android smoke test |

Preserved [runtime inventory](security/evidence/2026-11-26/runtime-artifacts.tsv), [OSV report](security/evidence/2026-11-26/runtime-osv-report.json), [dependency graph](security/evidence/2026-11-26/runtime-dependencies.txt) and [permission report](security/evidence/2026-11-26/apk-permissions.txt) remain available after CI artifact expiration. See the [security review](security/dependency-review.md) for upstream fixes, exact hashes and coverage limits.

The debug APK SHA-256 is `779dfc1546843991a8b559f853ff100b57d062b0065b28bdeb5ab979b75a348f`. It is available in the [CI build artifact](https://github.com/Axoineo/terminal/actions/runs/45868354385/artifacts/15140978957) while retained. CI artifacts require GitHub access and expire after seven days; they are not a production release channel.

## Next acceptance work

Test the build on a physical Android phone and a compatible controlled SSH host: fingerprint approval, authentication, typing/paste/resize, network loss, app switching and reopening history. JVM SSH tests plus Android native/UI tests do not establish Android SSH provider/server interoperability. Check API 37 LAN permission denial/revocation, OEM background killing, TalkBack/braille and common IMEs. Key authentication is the next interoperability feature; do not weaken a server's authentication or the client's crypto policy to fit this prototype.

Publisher identity/signatures, reproducible source-to-binary correspondence, full release licensing/security reconciliation and release signing remain release work. CI also reports deprecated Node-20-era action majors (forced onto Node 24), including retired setup-java v4; migrating those pinned build tools is maintenance work, not a claimed app vulnerability. Research switching demand remains unvalidated.

The earlier transport-free scaffold passed 16 domain tests and built without network permission. Those historical results are superseded by the concrete checks above. The [roadmap](product/roadmap.md), [delivery estimate](product/delivery-estimate.md) and [validation plan](quality/validation-plan.md) distinguish this narrow prototype from the broader workspace proposal.

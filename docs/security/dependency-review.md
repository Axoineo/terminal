# Dependency and trust-boundary review

Reviewed **26 November 2026** for the fast Android SSH v1. This is a bounded review record and remaining verification plan. It is not an exhaustive audit, penetration test, proof of vulnerability absence, or a physical-device pass. Both jobs in CI run `45868354385` at commit `1ed783d` passed: debug build/inventory/OSV/notices/alignment verification, and seven instrumented tests on an Android 8 / API 26 x86_64 emulator. Final release and physical-device routes remain separate gates.

## Selected components and current evidence

| Component | Selected version / source | Evidence obtained | Still required |
| --- | --- | --- | --- |
| Terminal component | `org.connectbot:termlib:0.3.15`; upstream tag commit `7a50d641ec92ef164645af1f4e39d0bf1a86b480` | Tagged fixes and pinned-source sanitizer regressions reviewed; all four terminal native files equal inspected AAR bytes; API 26 x86_64 emulator native CSI/split-UTF-8/reset regression passed | Other ABIs/devices, broader malformed-stream/resize/IME checks, publisher provenance |
| SSH library | `com.github.mwiede:jsch:2.28.8`; [upstream release](https://github.com/mwiede/jsch/releases/tag/jsch-2.28.8) | Known revocation fix predates release; resolved runtime JAR fingerprint recorded; ten JVM backend security/configuration tests passed, including real MINA loopback handshakes | Publisher/signature verification; minimum-API/device SSH route |
| AndroidX/Compose/Room/Kotlin/coroutines | Effective graph includes Kotlin 2.4.21, coroutines 1.10.2, serialization 1.7.3, Room 2.8.5 | Exact-version notices packaged, including Kotlin BSD/Boost terms; resolved 99-artifact OSV query completed with zero mapped findings | Final release artifact/resource/NOTICE reconciliation; unmapped/native advisory review remains separate |
| Crypto/logging additions | No extra BC/Conscrypt provider; SLF4J API 2.0.20 | No BC, Conscrypt, MINA, Logback, Log4j or `slf4j-simple` coordinate in actual app runtime graph | Test configured RSA-SHA2/ECDSA route on minimum API/device; graph absence does not inspect all statically bundled code |

The initial SSH route retains no passwords or private keys by default. Password input is transient and must not be written to Room/history, diagnostics or analytics. Managed strings and third-party APIs may make perfect memory zeroization impossible; do not describe transient Java/Kotlin strings as guaranteed erased memory. An imported/persisted-key feature needs its own storage/signing review before it is advertised.

## Known security findings and disposition

| Finding | Primary evidence | Disposition for selected scope |
| --- | --- | --- |
| **CVE-2026-86231 / GHSA-x2rp-h5qg-fmqg:** certificate-form `@revoked` known-host entries mishandled | [Upstream issue 1091](https://github.com/mwiede/jsch/issues/1091), [2.28.6 fix release](https://github.com/mwiede/jsch/releases/tag/jsch-2.28.6), [GHSA record](https://github.com/advisories/GHSA-x2rp-h5qg-fmqg) | Selected 2.28.8 is newer than upstream's fixed 2.28.6. Do not select affected 2.28.0–2.28.5. V1 does not advertise host-certificate/CA/revocation-file support; restrict negotiated host-key algorithms accordingly. |
| **CVE-2023-48795 (Terrapin):** SSH transport integrity weakness | [JSch changelog](https://github.com/mwiede/jsch/blob/master/ChangeLog.md) records strict key exchange mitigation in 0.2.15 | Selected version includes that mitigation lineage. Verify actual negotiated algorithms/server behavior; do not silently re-enable weak compatibility algorithms or claim an old server became safe merely by upgrading the client. |
| **termlib issue 389:** native CSI parameter overflow/crash reported for 0.3.13/0.3.14 | [Issue 389](https://github.com/connectbot/termlib/issues/389); [0.3.15 source](https://github.com/connectbot/termlib/tree/7a50d641ec92ef164645af1f4e39d0bf1a86b480) | GitHub issue events show completed closure on 12 November 2026. Selected tag includes fix commits `b805f8d85eac1dedab505b2b9722e1753c1f2694` and `ccb762611090f5eb9c8f52041f7043d3c24525db`; parser guards and excess-parameter/subparameter tests inspected. Three host-side pinned-source cases passed as described below; packaged Android execution remains a separate check. |
| **CVE-2026-24805:** null dereference in LiteIDE's vendored libvterm | [NVD record](https://nvd.nist.gov/vuln/detail/CVE-2026-24805), [upstream LiteIDE patch](https://github.com/visualfc/liteide/pull/1326) | Named affected product is LiteIDE before x38.4, not automatically every libvterm consumer. Check relevant source behavior in the selected vendored copy; do not mark termlib vulnerable or unaffected solely from a library-name match. |
| Security hardening without a published CVE | [JSch 2.28.8 notes](https://github.com/mwiede/jsch/releases/tag/jsch-2.28.8) include SSH_FXP_DATA, auth-prompt and arithmetic bounds fixes; termlib 0.3.15 also fixes REP/resize cases | Review upstream fixes as well as scanner output. Do not downgrade to an older release merely because it has no matching scanner advisory. |

The GHSA revocation record is **unreviewed** and has no mapped package/affected-version fields for automated Dependabot alerts, despite describing the affected/fixed versions in its text. This is a concrete coverage gap: repository-local “no published advisories” and a clean automated report are not enough. Recheck the global advisory database, upstream issues/releases and vendored native sources.

## Source and artifact provenance

Selected termlib source identifies vendored libvterm revision **847**, Bazaar revision ID `leonerd@leonerd.org.uk-20261029213117-ufuqbcpo9edo3vqg`. Static source checks found numeric clamping and parameter/subparameter index guards in `lib/src/main/cpp/libvterm/src/parser.c`, with regression cases in `t/02parser.test`. The two fix commits above are ancestors of the selected tag. This supports source-selection disposition; it is not proof that a downloaded binary was built reproducibly from that source.

The pinned source was also compiled on the host with `gcc -fsanitize=address,undefined -fno-omit-frame-pointer -g` and exercised with the issue's 17-parameter SGR sequence, 17 colon subparameters and an oversized numeric CSI argument. The binary exited 0 without ASan/UBSan diagnostics. Initial leak checking was blocked by the host's ptrace environment; the successful rerun used `ASAN_OPTIONS=detect_leaks=0`. No leak-freedom claim follows. Temporary evidence is `/tmp/terminal-workspace-termlib-parser-regression.c` and its same-name executable. These three source regressions do not establish correctness of all terminal input or execution of the Maven-packaged Android `.so`.

| Downloaded artifact inspected | SHA-256 | Meaning / remaining limitation |
| --- | --- | --- |
| `termlib-0.3.15.aar` | `85276764f139bc8457918dbfff8a4e3fd22da9b500e4569d2d882f37d67f8e57` | Fingerprint of inspected AAR; establish trusted publisher/signature or independent provenance separately |
| `jsch-2.28.8-sources.jar` | `c7d70a63b86a2b26015d28e1dc42dc26aa1ed43aaa97c16d3bce8ec53955f62c` | Source artifact only; **not** a fingerprint of the runtime JAR |
| `jsch-2.28.8.jar` | `2afca1357d816028f2ac33f22407d2488a68406682973949cebb1b8ea6688a8b` | Actual resolved debug runtime JAR, recorded in preserved artifact inventory; publisher authentication remains separate |

Record download repository URL, artifact identity, retrieval date, expected upstream tag and signer full fingerprint where verified. A checksum from the same compromised repository is not independent authenticity evidence. A signed tag is also not a signature for a Maven binary. Keep integrity, publisher identity, source-to-binary correspondence and vulnerability screening as separate results. Gradle's [dependency verification documentation](https://docs.gradle.org/current/userguide/dependency_verification.html) explains why generated verification metadata still needs review.

Use pinned versions and approved repositories. Review the effective runtime and build-plugin graphs; plugins execute on the build host even when they do not ship in the APK. Do not add unreviewed repositories, dynamic versions, snapshots, downloaded build scripts or blanket checksum/signature exceptions to fix resolution. If verification metadata is generated, review it against trusted evidence before calling it trusted; do not claim that merely hashing the cache verifies its origin.

### Runtime inventory and OSV check

The `:app:exportRuntimeArtifacts` task exports resolved `debugRuntimeClasspath` artifact coordinates, filenames and SHA-256 values to `app/build/reports/runtime-artifacts.tsv`. Columns are exactly `coordinate`, `file`, `sha256`, separated by tabs. It fingerprints resolved AAR/JAR artifacts rather than asserting their publisher identity, and does not expose local cache paths.

```sh
./gradlew :app:exportRuntimeArtifacts
python3 scripts/test_audit_runtime_dependencies.py
python3 scripts/audit_runtime_dependencies.py app/build/reports/runtime-artifacts.tsv --output app/build/reports/runtime-osv-report.json
```

The thin Python wrapper uses the [official OSV querybatch API](https://google.github.io/osv.dev/post-v1-querybatch/), public Maven coordinates/versions only, batching and per-package pagination. It sends no artifact bytes/hashes or local project paths. Exit 0 means **no mapped OSV findings**, 1 means findings, and 2 means invalid input/API unavailability/incomplete results; JSON distinguishes those statuses. Five offline contract tests exercise project exclusion, pagination and failure handling. The separate live CI result is preserved below.

Apache MINA SSHD is a `testImplementation` peer for loopback tests in `core:data`, not a production SSH dependency. Confirm its absence from the resolved app runtime/APK; do not treat a test dependency as shipped merely because its version appears in the build. Vendored libvterm and package-unmapped advisories still need the manual source/advisory review above even if OSV reports no mapped findings.

The preserved [runtime dependency graph](evidence/2026-11-26/runtime-dependencies.txt) and [binary artifact inventory](evidence/2026-11-26/runtime-artifacts.tsv) confirm MINA is absent. They select termlib 0.3.15, JSch 2.28.8, Kotlin standard library/parcelize 2.4.21, coroutines core JVM/Android 1.10.2, serialization core JVM 1.7.3, SLF4J API 2.0.20 and AndroidX graphics-path 1.1.0. No Bouncy Castle, Conscrypt, Logback, Log4j or SLF4J simple provider appears. Compose BOM 2026.11.01 resolves Compose runtime/UI/foundation to 1.13.0 and Material3 to 1.5.0; do not assume termlib's requested Compose version is the effective version. The exporter records external resolved binary artifacts and excludes local project artifacts; platforms/constraints in the text graph are not extra shipped binaries.

The final debug [OSV report](evidence/2026-11-26/runtime-osv-report.json), generated at `2026-11-26T17:31:38.097423+00:00`, records **99 external binary artifacts, 99 queried Maven packages and zero mapped findings** (`no_known_findings`). The report's inventory SHA-256 matches the preserved TSV: `f3b0535778ed682d9d745ae229b4ec9df1ea4154078a4d3e80ae429c1600697a`. This supersedes the preliminary 139-coordinate text-graph query as the debug binary result. It does not cover the package-unmapped JSch advisory or vendored native source; the manual dispositions above remain necessary. Report SHA-256: `c95b5e3eff50bd0977aa32d2d065a0b021712f3af2aaf0c16eedfdabd108ee99`.

## Runtime and native artifact gate

Capture the actual debug/release runtime graph, including selected variants and dependency substitutions. Distinguish shipped libraries, optional upstream dependencies not present, bundled code inside JARs/AARs, and build/test-only tools. Review crypto/logging additions explicitly. JSch contains JZlib/jBCrypt license notices even without adding separate dependencies; termlib statically embeds libvterm, which a Maven-only graph will not enumerate.

The debug [native file inventory](evidence/2026-11-26/apk-native-files.txt) contains **eight native files**: `libjni_cb_term.so` and `libandroidx.graphics.path.so` for each of `arm64-v8a`, `armeabi-v7a`, `x86` and `x86_64`. Earlier `readelf` inspection of these selected library binaries found all three LOAD segments of each file aligned to `0x4000`, with RW/non-executable GNU_STACK flags; all four packaged terminal native files matched the inspected Maven AAR. The final debug CI also passed the APK 16 KiB ZIP-alignment check. These are artifact checks, not an Android memory-safety/ABI certification or a 16 KiB device execution pass. [Android 16 KiB guidance](https://developer.android.com/guide/practices/page-sizes).

| Inspected integration evidence | SHA-256 / scope |
| --- | --- |
| CI `45868354385` debug APK at `1ed783d` | `779dfc1546843991a8b559f853ff100b57d062b0065b28bdeb5ab979b75a348f` |
| Preserved `runtime-dependencies.txt` | `d26458f3ec6fab10f5bd8ee2546ec5293ffe068e7225518c2e23c472a7bd3a80` |
| Notice packaging | All 15 `assets/third_party/` files match source bytes; all 13 provenance-recorded text fingerprints checked in CI |
| APK permissions | [Preserved manifest output](evidence/2026-11-26/apk-permissions.txt): `INTERNET`, `ACCESS_LOCAL_NETWORK`, and the app-scoped AndroidX dynamic-receiver signature permission; no other requested permissions |

The APK's `provenance.json` is intentionally the earlier build-time review snapshot. This document and the preserved CI evidence record subsequent verification; changing a packaged snapshot would produce a different APK hash.

Pinned termlib source also bounds unterminated OSC accumulation: `TerminalTextDecoder.kt` discards/frees oversized ordinary/property payloads at 1 MiB and clipboard payloads at 16 MiB, with a fixed set of decoder stream keys. `Terminal.cpp` passes JNI fragments of at most 64 KiB. Inline images are off in this integration. A null OSC 52 callback prevents the clipboard side effect; it **does not prevent parsing or transient allocation up to the existing 16 MiB bound**, and the public API does not expose a lower cap. This is source inspection, not an executed packaged-native stress test. No unbounded unterminated-OSC accumulator was found in that inspected path; other terminal/native inputs still require device regression checks.

## App integration verification

| Boundary | Required fast-v1 behavior / test |
| --- | --- |
| SSH host identity | Explicit SHA-256 TOFU prompt with host/port/algorithm; no password sent before acceptance; exact known key accepted; changed key fails closed; cancel cannot pin or connect |
| Algorithm policy | Tested modern RSA-SHA2/ECDSA configuration; no DSA, SHA-1 host-signature, weak cipher or compatibility fallback silently added. Unsupported Ed25519/provider combinations produce an honest unsupported error. |
| Credentials | No persistent password/private key by default; no typed-input logging; secrets absent from database/history/debug logs and error strings |
| Terminal output | Remote bytes are untrusted, including clipboard/URL/title/image/control sequences. OSC 52 disabled or deliberately gated; remote output never launches a URL automatically; bound parsing/history/resources. |
| Paste | Preview and deliberate send for clipboard/multiline content; no invisible newline-triggered command execution |
| History | Capture default off and explicitly enabled per attempt; app-private Room storage caps received output at 256 KiB per session and 20 saved sessions, with deletion. Serialize snapshot writes so an older snapshot cannot replace a newer one. Received-output-only claim: echoed passwords/tokens can still appear, and users can disable/clear capture. |
| Cancellation/lifetime | Cancel auth/connect, drop network, resize and close; cleanup sockets/native handles; restored state does not auto-replay input or invent a successful result |
| Backup/export | Verify final manifest/extraction behavior for retained output/host metadata; no implicit cloud synchronization or credential backup claim |

These are acceptance conditions; only the recorded checks are established. CI passed 20 domain tests and ten backend security/configuration tests, including a real loopback MINA peer for host approval before authentication, changed-key refusal and cancellation. The API 26 x86_64 emulator then passed **seven instrumented tests**: five core-data tests (two journal and three session-history) and two app tests (native terminal CSI/split-UTF-8/reset, and Activity launch/recreation). This is actual emulator execution, with a narrower scope than a complete mobile SSH workflow. Physical-device SSH, IME, API 37 local-network permission, TalkBack, other ABIs and OEM lifecycle behavior have not been exercised.

CI annotations also identified action-maintenance work: Node 20 actions were forced onto Node 24, and setup-java v4 is deprecated. These are tracked build-action migration concerns, not newly established vulnerabilities in the APK. Review the upstream action upgrade and its immutable commit pin separately; no app dependency or behavior was changed in response to those annotations.

## Remaining review record

| Evidence | Status at initial document creation |
| --- | --- |
| Selected direct-library source/license and named upstream-fix triage | Reviewed as described above |
| Resolved debug runtime graph and binary inventory | Preserved graph plus 99-artifact hash TSV reviewed; final release/build-plugin graph remains pending. |
| Runtime JAR/AAR publisher signature verification and trusted fingerprints | Pending; hashes above are not a substitute |
| Applicable-advisory screening | Completed debug resolved-artifact OSV query: 99 packages, zero mapped findings. Named and vendored-native findings manually reviewed as described, not an exhaustive native audit. |
| Engine and reviewed runtime license assets | All 15 assets match source bytes in final debug APK; 13 recorded text hashes verified. Final release/resource obligations still require reconciliation: [notices manifest](THIRD_PARTY_NOTICES.md). |
| Android execution | Seven API 26 x86_64 emulator tests passed as scoped above. Physical SSH/IME/TalkBack/API 37 LAN-permission routes, other ABIs and OEM lifecycle checks remain unrun. |

Recheck on any dependency/engine upgrade and before release. Maintain a dated finding/disposition log with exact artifact versions and reproducible checks. For newly discovered vulnerabilities use the upstream project's private security-report mechanism if offered, or its maintainer-published security contact. Do not invent an email address or send exploit/credential details to a public issue tracker. At review, [JSch](https://github.com/mwiede/jsch/security) and [termlib](https://github.com/connectbot/termlib/security) security pages did not provide a SECURITY.md policy; check current reporting options before disclosure. This document sends no external report.

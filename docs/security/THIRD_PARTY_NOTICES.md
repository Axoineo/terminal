# Third-party notices manifest

Prepared **26 November 2026** for the fast-v1 components and reviewed debug runtime graph. This is a source-linked notice inventory, **not a declaration that the final release APK's complete license obligations are already satisfied**. Reconcile the final variant and embedded native/resources, preserving complete applicable license/NOTICE texts. URLs and a license label alone are not a finished notice bundle.

Complete text copies for the selected terminal/SSH engines, reviewed Kotlin/coroutines/serialization/SLF4J runtime components and identified bundled notices are under [app assets](../../app/src/main/assets/third_party/THIRD_PARTY_NOTICES.txt). [Provenance metadata](../../app/src/main/assets/third_party/provenance.json) records exact source/artifact origins and copied-file SHA-256 values. CI run `45868354385` at commit `1ed783d` verified all **15 notice assets match source bytes** in the debug APK, including all 13 provenance-recorded text hashes. APK SHA-256: `779dfc1546843991a8b559f853ff100b57d062b0065b28bdeb5ab979b75a348f`. Final release/resource reconciliation remains required.

## Selected direct and embedded components

| Component | Exact selected scope | License / notice source | Distribution work |
| --- | --- | --- | --- |
| ConnectBot termlib | `org.connectbot:termlib:0.3.15`, tag `7a50d641ec92ef164645af1f4e39d0bf1a86b480` | [Apache-2.0 LICENSE at selected tag](https://github.com/connectbot/termlib/blob/7a50d641ec92ef164645af1f4e39d0bf1a86b480/LICENSE) | Include license and any applicable retained source/NOTICE attributions from the selected artifact; record local changes if any |
| libvterm embedded by termlib | Vendored revision 847 in that tag; not a separate Maven dependency | [Selected vendored MIT LICENSE](https://github.com/connectbot/termlib/blob/7a50d641ec92ef164645af1f4e39d0bf1a86b480/lib/src/main/cpp/libvterm/LICENSE); copyright Paul Evans, 2008 | Retain complete MIT copyright/permission text; inspect additional source notices such as Unicode width data |
| JSch maintained fork | `com.github.mwiede:jsch:2.28.8` | [BSD-style LICENSE.txt](https://github.com/mwiede/jsch/blob/jsch-2.28.8/LICENSE.txt); original JCraft copyright notice retained upstream | Include exact artifact's complete license/disclaimer and retained copyright text |
| JZlib code bundled in JSch | Code/notice in selected JSch artifact, even if compression is unused | [JZlib notice](https://github.com/mwiede/jsch/blob/jsch-2.28.8/LICENSE.JZlib.txt), BSD-style | Preserve selected artifact's `META-INF/LICENSE.JZlib.txt` |
| jBCrypt code bundled in JSch | Code/notice in selected JSch artifact, even when fast v1 has no imported-key feature | [jBCrypt notice](https://github.com/mwiede/jsch/blob/jsch-2.28.8/LICENSE.jBCrypt.txt), ISC-style permission notice | Preserve selected artifact's `META-INF/LICENSE.jBCrypt.txt` |

The inspected JSch source JAR contains `META-INF/LICENSE.txt`, `META-INF/LICENSE.JZlib.txt` and `META-INF/LICENSE.jBCrypt.txt`; exact copies are in `app/src/main/assets/third_party/` and now verified in the debug APK. The termlib license and selected vendored MIT/width texts were copied from tag `7a50d641ec92ef164645af1f4e39d0bf1a86b480`. The [resolved binary inventory](evidence/2026-11-26/runtime-artifacts.tsv) fingerprints actual JSch and termlib runtime artifacts, separate from source-JAR inspection. No Termux application code or full Linux distribution is selected by this manifest.

## Resolved runtime notice reconciliation

| Resolved family | Recorded evidence / remaining work |
| --- | --- |
| Kotlin standard library and parcelize runtime 2.4.21 | Exact source-JAR copyright comments retained in `Runtime-Apache-NOTICES.txt`; shared `Apache-2.0.txt` is byte-identical to the selected Kotlin/GWT/Guava license files. [Exact release license manifest](https://github.com/JetBrains/kotlin/blob/v2.4.21/license/README.md) identifies stdlib ThreeTenBP-derived time code and Boost-derived JVM math: full `Kotlin-ThreeTenBP-BSD.txt` and `Kotlin-Boost-1.0.txt` included. The Kotlin compiler is build tooling, not an APK runtime component. |
| Coroutines core JVM/Android 1.10.2 and serialization core JVM 1.7.3 | Complete exact-release `Coroutines-Apache-2.0.txt` and `Serialization-Apache-2.0.txt` copied; serialization source copyright comments retained. [Coroutines release license](https://github.com/Kotlin/kotlinx.coroutines/blob/1.10.2/LICENSE.txt), [serialization release license](https://github.com/Kotlin/kotlinx.serialization/blob/v1.7.3/LICENSE.txt). |
| AndroidX, Compose, Room and Material | Shared Apache-2.0 text included. Actual graph resolves Compose UI/runtime/foundation 1.13.0, Material3 1.5.0 and Room 2.8.5. Exact final artifact/resource/NOTICE reconciliation remains required. AndroidX graphics-path 1.1.0 AAR's embedded Apache LICENSE and source attribution inspected; APK contains its native `.so` for all four ABIs. |
| SLF4J API 2.0.20 | Complete MIT notice copied from exact runtime JAR as `SLF4J-MIT.txt`; no Logback/Log4j/SLF4J-simple provider in inspected runtime graph. |
| JetBrains annotations 23.0.0, JSpecify 1.0.0, Error Prone annotations 2.15.0, Guava listenablefuture 1.0 | Exact source JAR Apache copyright comments retained in `Runtime-Apache-NOTICES.txt`; shared Apache text provided. A graph coordinate is not itself proof that every class is retained in a final optimized APK. |
| Optional crypto, socket and test peer | No Bouncy Castle, Conscrypt or MINA coordinate in inspected app runtime graph. MINA is a test-only peer; do not attribute it as shipped. Statically embedded code still requires separate inspection. |
| Build/test tooling | Maintain an inventory distinct from APK runtime notices; these tools can execute during builds even if they do not ship |

This table records a practical bounded reconciliation against the preserved [99-artifact debug inventory](evidence/2026-11-26/runtime-artifacts.tsv), [effective dependency graph](evidence/2026-11-26/runtime-dependencies.txt) and [eight-file native inventory](evidence/2026-11-26/apk-native-files.txt). The [live OSV report](evidence/2026-11-26/runtime-osv-report.json) records zero mapped findings for 99 queried packages; that is a separate security result, not a license-compliance decision. A dependency absent from the graph may still be statically bundled in an AAR/JAR. A dependency mentioned in an upstream release's build updates is not automatically shipped by this app. Local artifact/text hashes establish fingerprints, not independent publisher authentication.

The packaged provenance JSON intentionally remains the earlier build-time review snapshot. Subsequent CI packaging checks and exact resolved binary results are recorded here and in the preserved evidence; the APK is not modified merely to rewrite its audit snapshot.

## Completion record before release

- Export the resolved runtime graph for the exact debug/release variants being distributed, and inventory APK native libraries and assets.
- Record each component's artifact coordinate/version, upstream revision where known, exact license/NOTICE sources, included copyright texts, modifications and packaged notice location.
- Package complete required texts and provide an accessible in-app notices route or equivalent distributed materials. Check that packaging exclusions have not removed required notices.
- Mark this manifest complete only after reconciling it against the actual artifact. Keep [security review status](dependency-review.md) separate: a permissive license is not evidence of secure code, and a patched library still needs its notices.

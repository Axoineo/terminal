# Third-party notices manifest

Prepared **26 November 2026** for selected fast-v1 components. This is the source-linked inventory/template for assembling distribution notices, **not a declaration that the final APK's complete license obligations are already satisfied**. Reconcile the actual packaged runtime graph and embedded native/resources, preserve required copyright notices, and include complete applicable license/NOTICE texts in distributed materials. URLs and a license label alone are not the finished notice bundle.

Complete text copies for the selected terminal/SSH engines and their identified bundled notices are now under [app assets](../../app/src/main/assets/third_party/THIRD_PARTY_NOTICES.txt). [Provenance metadata](../../app/src/main/assets/third_party/provenance.json) records the exact selected source/artifact origins and copied-file SHA-256 values. The source bundle exists; final APK inclusion and the remaining runtime/transitive inventory still need reconciliation.

## Selected direct and embedded components

| Component | Exact selected scope | License / notice source | Distribution work |
| --- | --- | --- | --- |
| ConnectBot termlib | `org.connectbot:termlib:0.3.15`, tag `7a50d641ec92ef164645af1f4e39d0bf1a86b480` | [Apache-2.0 LICENSE at selected tag](https://github.com/connectbot/termlib/blob/7a50d641ec92ef164645af1f4e39d0bf1a86b480/LICENSE) | Include license and any applicable retained source/NOTICE attributions from the selected artifact; record local changes if any |
| libvterm embedded by termlib | Vendored revision 847 in that tag; not a separate Maven dependency | [Selected vendored MIT LICENSE](https://github.com/connectbot/termlib/blob/7a50d641ec92ef164645af1f4e39d0bf1a86b480/lib/src/main/cpp/libvterm/LICENSE); copyright Paul Evans, 2008 | Retain complete MIT copyright/permission text; inspect additional source notices such as Unicode width data |
| JSch maintained fork | `com.github.mwiede:jsch:2.28.8` | [BSD-style LICENSE.txt](https://github.com/mwiede/jsch/blob/jsch-2.28.8/LICENSE.txt); original JCraft copyright notice retained upstream | Include exact artifact's complete license/disclaimer and retained copyright text |
| JZlib code bundled in JSch | Code/notice in selected JSch artifact, even if compression is unused | [JZlib notice](https://github.com/mwiede/jsch/blob/jsch-2.28.8/LICENSE.JZlib.txt), BSD-style | Preserve selected artifact's `META-INF/LICENSE.JZlib.txt` |
| jBCrypt code bundled in JSch | Code/notice in selected JSch artifact, even when fast v1 has no imported-key feature | [jBCrypt notice](https://github.com/mwiede/jsch/blob/jsch-2.28.8/LICENSE.jBCrypt.txt), ISC-style permission notice | Preserve selected artifact's `META-INF/LICENSE.jBCrypt.txt` |

The inspected JSch source JAR contains `META-INF/LICENSE.txt`, `META-INF/LICENSE.JZlib.txt` and `META-INF/LICENSE.jBCrypt.txt`; exact copies are in `app/src/main/assets/third_party/`. The termlib license and selected vendored MIT/width texts were copied from tag `7a50d641ec92ef164645af1f4e39d0bf1a86b480`. Confirm the runtime JAR and APK packaging rather than assuming source-JAR presence satisfies binary distribution requirements. No Termux application code or full Linux distribution is selected by this manifest.

## Reconcile from the resolved graph

| Family / possible transitive | What to record |
| --- | --- |
| Kotlin standard library and kotlinx.coroutines | Effective versions, upstream Apache-2.0 licenses/notices and any bundled third-party texts; [coroutines license](https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt) |
| AndroidX, Compose, Room and Material components | Every packaged artifact/effective version, upstream license/notice metadata and included resources/fonts; do not copy a test-app asset into production without checking its own notice |
| SLF4J API/provider if present | Effective versions and [MIT license](https://www.slf4j.org/license.html); distinguish API from any added logging provider |
| Optional crypto, compression, socket or native libraries | Only mark included when the actual graph/artifact shows inclusion; review added providers separately. No extra Bouncy Castle provider is planned for fast v1. |
| Build/test tooling | Maintain an inventory distinct from APK runtime notices; these tools can execute during builds even if they do not ship |

This table is intentionally not an invented exhaustive list. A dependency absent from the Gradle graph may still be statically bundled in an AAR/JAR. A dependency mentioned in an upstream release's build updates is not automatically shipped by this app.

## Completion record before release

- Export the resolved runtime graph for the exact debug/release variants being distributed, and inventory APK native libraries and assets.
- Record each component's artifact coordinate/version, upstream revision where known, exact license/NOTICE sources, included copyright texts, modifications and packaged notice location.
- Package complete required texts and provide an accessible in-app notices route or equivalent distributed materials. Check that packaging exclusions have not removed required notices.
- Mark this manifest complete only after reconciling it against the actual artifact. Keep [security review status](dependency-review.md) separate: a permissive license is not evidence of secure code, and a patched library still needs its notices.

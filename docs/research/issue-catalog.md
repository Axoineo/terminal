# Master issue catalog

Research snapshot: **2026-11-25**. Stable issue IDs **01–33** preserve the original audit's ordering. The [competitive audit](competitive-audit.md) supplies product/category context. Direct source links are included here so this document remains useful independently.

## Classification rules

**Severity:** `[Critical]` blocks a core workflow, causes a forced termination/crash or makes essential data/work unusable; `[High]` creates major daily friction; `[Medium]` is noticeable friction or missing convenience; `[Low]` is minor/cosmetic. Severity is conditional on encountering the issue, not its frequency. No low-severity cosmetic items were promoted into this consolidated catalog.

**Usefulness:** `[Must-Solve Differentiator]` identifies a plausible switching reason for the stated cohort; `[High Utility]` is broadly useful; `[Nice-to-Have]` improves polish; `[Low Need / Niche]` serves a narrow cohort. These are product judgments to validate, not measured demand.

**Feasibility:** `[Solvable in App]` applies to the bounded app behavior specified below; `[Requires Workaround]` needs a changed workflow, backend, host software or user/device configuration; `[Blocked by Mobile OS]` means the unrestricted promise is unavailable to an ordinary unprivileged app on the specified platform. A workaround is not equivalent to eliminating the restriction.

**Evidence:** `D` = documented capability/constraint; `R` = user/maintainer issue report; `H` = product hypothesis/inference. Reports were inspected or identified in the research snapshot. Their state can change; an open issue is not proof of prevalence, root cause or failure in every current release. Unless stated otherwise, “affected” identifies the execution model exposed to a risk, not a confirmed defect in every named app.

## Comparison and prioritization

The table re-evaluates every severity-tagged issue by usefulness and feasibility. The six app-solvable differentiator candidates are **06, 15, 16, 18, 19 and 26**; they can be combined into fewer coherent product bets.

| ID | Severity | Real-world usefulness and market need | Feasibility for the bounded remedy |
|---|---|---|---|
| [01](#issue-01) | [Critical] | [High Utility] | [Blocked by Mobile OS] |
| [02](#issue-02) | [High] | [High Utility] | [Requires Workaround] |
| [03](#issue-03) | [High] | [High Utility] | [Requires Workaround] |
| [04](#issue-04) | [Critical] | [High Utility] | [Requires Workaround] |
| [05](#issue-05) | [High] | [High Utility] | [Requires Workaround] |
| [06](#issue-06) | [High] | [Must-Solve Differentiator] | [Solvable in App] |
| [07](#issue-07) | [Critical] | [High Utility] | [Requires Workaround] |
| [08](#issue-08) | [High] | [Low Need / Niche] | [Blocked by Mobile OS] |
| [09](#issue-09) | [High] | [High Utility] | [Requires Workaround] |
| [10](#issue-10) | [High] | [High Utility] | [Requires Workaround] |
| [11](#issue-11) | [Critical] | [High Utility] | [Blocked by Mobile OS] |
| [12](#issue-12) | [Critical] | [Low Need / Niche] | [Requires Workaround] |
| [13](#issue-13) | [High] | [Low Need / Niche] | [Blocked by Mobile OS] |
| [14](#issue-14) | [Critical] | [High Utility] | [Requires Workaround] |
| [15](#issue-15) | [Critical] | [Must-Solve Differentiator] | [Solvable in App] |
| [16](#issue-16) | [Critical] | [Must-Solve Differentiator] | [Solvable in App] |
| [17](#issue-17) | [High] | [High Utility] | [Requires Workaround] |
| [18](#issue-18) | [High] | [Must-Solve Differentiator] | [Solvable in App] |
| [19](#issue-19) | [High] | [Must-Solve Differentiator] | [Solvable in App] |
| [20](#issue-20) | [High] | [Low Need / Niche] | [Blocked by Mobile OS] |
| [21](#issue-21) | [High] | [High Utility] | [Solvable in App] |
| [22](#issue-22) | [High] | [High Utility] | [Solvable in App] |
| [23](#issue-23) | [High] | [High Utility] | [Solvable in App] |
| [24](#issue-24) | [High] | [High Utility] | [Solvable in App] |
| [25](#issue-25) | [High] | [High Utility] | [Solvable in App] |
| [26](#issue-26) | [Critical] | [Must-Solve Differentiator] — accessibility cohort | [Solvable in App] |
| [27](#issue-27) | [High] | [High Utility] | [Solvable in App] |
| [28](#issue-28) | [High] | [High Utility] | [Requires Workaround] |
| [29](#issue-29) | [High] | [High Utility] | [Requires Workaround] |
| [30](#issue-30) | [Medium] | [Nice-to-Have] | [Solvable in App] |
| [31](#issue-31) | [High] | [Low Need / Niche] | [Solvable in App] |
| [32](#issue-32) | [Medium] | [Nice-to-Have] | [Requires Workaround] |
| [33](#issue-33) | [Medium] | [Nice-to-Have] | [Requires Workaround] |

<a id="issue-01"></a>
## 01 — Android local processes killed

**Affected:** Termux and local runtimes used by UserLAnd, Andronix, proot-distro and Acode; other Android clients can also lose their own process. **Evidence: D.** The [Termux repository](https://github.com/termux/termux-app) documents phantom/excessive-CPU process termination concerns. Exact behavior depends on Android version, target SDK, ROM and device state; do not apply an old numerical limit indiscriminately to every modern device.

**Consequence/remedy:** a build or shell may terminate without a normal exit. Guaranteed survival under memory pressure, force-stop or OS termination is blocked. Preserve recoverable metadata/output and offer remote execution for critical work; neither recreates arbitrary lost local process memory.

<a id="issue-02"></a>
## 02 — Screen-off CPU work confused with keeping the display awake

**Affected:** Android local jobs and remote clients expecting continuous background connectivity; explicit report in UserLAnd. **Evidence: D + R.** [Android Doze guidance](https://developer.android.com/training/monitoring-device-state/doze-standby) distinguishes CPU/network restrictions; [UserLAnd #2395](https://github.com/CypherpunkArmory/UserLAnd/issues/2395), November 15, 2026, reports delayed/frozen timed code after wake-lock changes despite a keep-screen-on option.

**Remedy:** distinct display/CPU controls, appropriate user-visible execution services and device-specific guidance. Wake locks do not guarantee network access, defeat Doze in all states or prevent process killing. The report's precise cause is unconfirmed.

<a id="issue-03"></a>
## 03 — Plain SSH and tunnels interrupted by networks or backgrounding

**Affected:** SSH sessions/tunnels in Termius, ConnectBot, Moshi, Blink and SSH launched from local environments. **Evidence: D.** [Mosh](https://mosh.org/) documents roaming/intermittent-connectivity support; [Android power management](https://developer.android.com/training/monitoring-device-state/doze-standby) explains background restrictions.

**Remedy:** connection-state UI, reconnect, supported roaming transport and host persistence. An interrupted TCP tunnel may need reopening; Mosh terminal recovery does not provide equivalent TCP forwarding. Avoid promising background socket immortality.

<a id="issue-04"></a>
## 04 — Reconnection does not guarantee remote task survival

**Affected:** remote interactive shells across SSH/Mosh clients. **Evidence: D.** [ConnectBot's Mosh guide](https://connectbot.org/guides/mosh/) explicitly recommends a host multiplexer for work that should survive app closure/session endings. [Moshi documentation](https://getmoshi.app/docs) already describes durable multiplexed workspaces and reattachment.

**Remedy:** detect/provision an allowed host multiplexer, reconnect to a stable session identifier, and clearly report whether a task is still running. Requires host software/cooperation. Auto-reattach already exists in competitors; it alone is not a new-product differentiator. Server reboot, multiplexer termination and task failure remain separate failure modes.

<a id="issue-05"></a>
## 05 — Mosh prerequisites and forwarding/bastion limits

**Affected:** Mosh workflows in Termius, ConnectBot, Moshi and Blink. **Evidence: D.** [ConnectBot guidance](https://connectbot.org/guides/mosh/) requires compatible host software and reachable UDP; [Moshi connections](https://getmoshi.app/docs/connections) documents alternative transports and connection configuration.

**Remedy:** preflight SSH authentication, server executable, UDP reachability and jump-host route; offer an explainable fallback. Restricted hosts/networks may prevent installing software or opening UDP. Keep SSH forwarding separate rather than implying Mosh forwards arbitrary TCP services. Transport negotiation and diagnosis already have competing implementations.

<a id="issue-06"></a>
## 06 — Durable output and history

**Affected:** users of bounded terminal buffers and reconnecting remote sessions; explicitly documented for Moshi. **Evidence: D + H.** [Moshi scrollback documentation](https://getmoshi.app/docs/scrolling) distinguishes in-app history, host multiplexer history and permanent transcripts.

**App-solvable promise:** persist **received** output before discarding it, restore/search/export that record, identify sessions and mark gaps. Test this across app restarts and abrupt process loss. **Boundary:** screen-state synchronization cannot recover output never sent to the phone; complete offline-period capture needs host logging and retrieval. A transcript may contain secrets: give users deliberate recording, retention and deletion controls. Market need for a first-class durable output workspace requires user validation.

<a id="issue-07"></a>
## 07 — Modern Android native-binary execution restrictions

**Affected:** local environments downloading packages or executing generated native binaries; relevant to new runtime architecture and distribution. **Evidence: D.** [Android API 29+ behavior changes](https://developer.android.com/about/versions/10/behavior-changes-10#execute-permission) restrict direct `execve()` of writable app-home files for apps targeting Android 10+.

**Remedy:** select a supportable packaged runtime/backend and distribution model, use supported interpreters or remote execution, and validate the target-SDK-specific design early. A legacy-target working app does not establish a viable modern-target Play design. PRoot, broad storage permission and cosmetic shell changes do not automatically remove this restriction.

<a id="issue-08"></a>
## 08 — Root, kernel namespaces and full init unavailable in ordinary PRoot

**Affected:** PRoot in UserLAnd, Andronix, proot-distro and Acode. **Evidence: D.** The [proot-distro limitations](https://github.com/termux/proot-distro) document fake root and unavailable kernel-dependent facilities.

**Remedy/boundary:** do not promise privileged Docker Engine, namespaces, mounts or full systemd parity inside ordinary unrooted PRoot. OCI-image manipulation is a distinct capability. A supported VM, rooted device or remote Linux changes the execution model and needs separate validation. Importance is high for affected workloads, but this is a narrower market than routine scripting/remote administration.

<a id="issue-09"></a>
## 09 — ABI, libc, paths, architecture and package/tutorial mismatch

**Affected:** Termux, distro wrappers, Acode and users following desktop instructions on ARM/mobile. **Evidence: D.** [Termux execution documentation](https://github.com/termux/termux-packages/wiki/Termux-execution-environment) explains its environment differences; [Acode terminal guidance](https://docs.acode.app/user-guide/terminal) identifies its Alpine package model.

**Remedy:** detect architecture/runtime/libc, label supported recipes, verify project tool versions and explain incompatible binaries. A distro can improve compatibility but cannot make an x86 binary or kernel-specific tool work natively on every ARM Android device. Curated setup alone is not novel; Andronix already provides [development packs](https://docs.andronix.app/use/dev-packs).

<a id="issue-10"></a>
## 10 — PRoot syscall overhead

**Affected:** filesystem-heavy package installation/build work in PRoot-based environments. **Evidence: D.** [proot-distro](https://github.com/termux/proot-distro) documents interception overhead.

**Remedy:** benchmark representative projects, reduce unnecessary filesystem work and offer native/VM/remote backends where supported. A faster terminal renderer does not remove syscall translation costs. Performance should be measured per device/workload; no universal slowdown multiplier is established by this audit.

<a id="issue-11"></a>
## 11 — Native Linux VM available only on selected devices

**Affected:** Android native Linux Terminal and any product planning to depend on its availability. **Evidence: D.** [AOSP](https://source.android.com/docs/core/virtualization/usecases) identifies selected-device availability and a Developer Settings activation path; [Googlebook preview material](https://developer.android.com/blog/posts/googlebook-linux-terminal-preview-vm) applies to a particular device/product context.

**Remedy/boundary:** detect backend support and expose an alternative. An ordinary application cannot add missing firmware/hypervisor/platform support. Do not promise the stock Linux Terminal or its newest file/network integration on every Android phone.

<a id="issue-12"></a>
## 12 — Native Terminal startup affected by VPN configuration

**Affected:** the configuration described in native Terminal [Google issue 488270267](https://issuetracker.google.com/issues/488270267). **Evidence: R.** This is a specific reported startup/network problem; root cause and broad applicability were not established.

**Remedy:** reproduce on a versioned device/VPN matrix, explain required routing and provide diagnosis or documented configuration alternatives. Do not recommend disabling all VPN protection as a universal fix or label the native VM incompatible with every VPN. Keep this below broad workspace opportunities until prevalence is known.

<a id="issue-13"></a>
## 13 — iOS unrestricted native Linux parity

**Affected:** an optional iOS local-runtime product; comparison applies to Blink and other iOS terminals. **Evidence: D.** [App Review execution rules](https://developer.apple.com/app-store/review/guidelines/) and [background task APIs](https://developer.apple.com/documentation/backgroundtasks) constrain the operating model.

**Remedy/boundary:** offer bundled commands, permitted interpreted/emulated workloads or remote Linux with explicitly different capability contracts. Do not promise unrestricted downloaded native-code execution, a full Linux kernel or immortal daemons from an ordinary App Store terminal. This does not imply that useful local terminal commands are impossible on iOS.

<a id="issue-14"></a>
## 14 — Runtime installation fails

**Affected:** local runtime installers; concrete Acode report. **Evidence: R.** [Acode #3108](https://github.com/Acode-Foundation/Acode/issues/3108), November 22, 2026, reports `tar: exec gunzip: Permission Denied` on Acode 1.13.5, Android 8/MIUI and SD configured as main storage. The SD/permission cause is a reporter hypothesis.

**Remedy:** preflight supported placement, space and executability; resumable verified assets, staged installation and actionable recovery. Feasibility remains a workaround until the specific cause is reproduced; do not claim a UI change alone repairs an execution-policy failure. Older-device prevalence needs validation.

<a id="issue-15"></a>
## 15 — Damaged or stranded environments

**Affected:** UserLAnd asset updates and local package environments including Termux; general app-owned runtime lifecycle. **Evidence: R + D.** [UserLAnd #2384](https://github.com/CypherpunkArmory/UserLAnd/issues/2384), October 30, 2026, describes inaccessible existing Ubuntu data after a failed asset/update sequence, including export failure. [Termux environment guidance](https://github.com/termux/termux-packages/wiki/Termux-execution-environment) warns about unsupported partial upgrades.

**App-solvable promise:** stage assets, validate before activation, keep user projects separate, retain a rollback point and provide recovery/export without starting the damaged runtime. Verify by interrupted-update fault injection and recovery drills. Arbitrary destructive package operations and missing historical backups cannot be reversed by promise.

<a id="issue-16"></a>
## 16 — Fragmented backup and uninstall data loss

**Affected:** local workspaces in Acode, Andronix, UserLAnd and Termux; source/channel migration magnifies the risk. **Evidence: D + R.** [Acode backup scope](https://docs.acode.app/user-guide/settings/backup-restore) excludes runtime files, connections and session state; [discussion #2920](https://github.com/Acode-Foundation/Acode/discussions/2920) describes export uncertainty. [Andronix backups](https://docs.andronix.app/use/backups) documents copying files out because full in-app distro backup is unavailable.

**App-solvable promise:** a clearly scoped workspace export, manifest/checksums, verified restore and an export destination outside the app's uninstall lifecycle. Explicitly identify excluded credentials, runtime caches and unsent remote changes. A backup button is insufficient without restoring it in a clean installation. Local app-private copies alone do not protect against uninstall.

<a id="issue-17"></a>
## 17 — Shared storage and SAF are not executable POSIX filesystems

**Affected:** Termux, Acode, UserLAnd and Andronix local/shared-file workflows. **Evidence: D.** [Android SAF documentation](https://developer.android.com/training/data-storage/shared/documents-files) describes user-granted document URIs; [Termux environment documentation](https://github.com/termux/termux-packages/wiki/Termux-execution-environment) covers execution/filesystem constraints. A selected document is not automatically a stable raw path with Unix permission/symlink semantics.

**Remedy:** explicitly materialize a working copy where supported, preserve identities/metadata and export through the granted provider. Shared/cloud providers differ in random access, rename and availability. Broad file access is not a guarantee of executable filesystem semantics. Keep this platform constraint distinct from app-solvable workspace coordination in issue 18.

<a id="issue-18"></a>
## 18 — Editor, LSP and runtime workspace mismatch

**Affected:** Acode documented external/SAF/remote project paths; similar multi-app workflows are an inference. **Evidence: D + H.** [Acode LSP guidance](https://docs.acode.app/user-guide/language-servers) says external/remote paths are skipped by default for STDIO and may be virtual/cached when enabled; its [agent tutorial](https://docs.acode.app/tutorials/ai-agent) limits command execution to terminal folders.

**App-solvable promise:** one app-owned canonical project copy shared by editor, terminal, LSP and task runner; show import/export or remote linkage separately. Preserve project identity across rename and permission renewal. Test multi-file edits and runtime relative paths. This remedy does not directly execute an arbitrary SAF/cloud folder and does not invent full editor integration that Acode already has.

<a id="issue-19"></a>
## 19 — Remote edit download–edit–upload friction

**Affected:** Termius Android graphical remote-edit workflow; broader remote-file coordination is a hypothesis. **Evidence: D + H.** [Termius SFTP instructions](https://docs.termius.com/organize-and-connect-to-hosts/managing-files-with-sftp) document the editing round trip. Acode and [Blink Code](https://docs.blink.sh/advanced/code) already offer integrated editing approaches.

**App-solvable promise:** open/edit/save with remote identity, pending-save state, retry and recoverable drafts visible in the same workspace. Detect a changed remote base before overwriting; retain both versions on conflict. Distinguish supported atomic rename from providers lacking it. No representative evidence here proves widespread corrupt/partial transfers across competitors, and “SFTP exists” is not a new-product gap.

<a id="issue-20"></a>
## 20 — Other apps' private files inaccessible

**Affected:** all ordinary Android terminal apps attempting cross-app private-data access. **Evidence: D.** The [Android application sandbox](https://source.android.com/docs/security/app-sandbox) isolates app UIDs/resources and recommends explicit sharing APIs.

**Remedy/boundary:** accept user/provider-authorized content, share intents or explicit cooperating-app integrations. Storage permission, PRoot and a different terminal emulator do not grant access to another app's private directory. Root/debugging paths, when available, change assumptions and are not the default product contract.

<a id="issue-21"></a>
## 21 — IME composition, history and modifier-key failures

**Affected:** reported Acode and UserLAnd combinations; terminal input quality is a wider engineering concern. **Evidence: R.** [Acode #2549](https://github.com/Acode-Foundation/Acode/issues/2549), July 28, 2026, reports backspace failure on HeliBoard/Fossify after history/autocomplete; other tested keyboards were unaffected. [UserLAnd #2390](https://github.com/CypherpunkArmory/UserLAnd/issues/2390), November 7, 2026, reports Japanese input and Ctrl composition regressions.

**Remedy:** a tested input bridge supporting composition, deletion and control sequences, with explicit compatibility modes. Validate Japanese/Chinese/Korean, dead keys, emoji, physical modifiers and history on real keyboards. A key toolbar alone does not address composition-state errors.

<a id="issue-22"></a>
## 22 — Composer hides output, loses drafts and mishandles multiline text

**Affected:** the source-built ConnectBot alpha in [#2667](https://github.com/connectbot/connectbot/issues/2667), November 17, 2026. **Evidence: R.** Reported version is `git-v1.12.0-alpha-3343`, Android 17/One UI 9; do not generalize it to stable versions.

**Remedy:** a docked/resizable composer, saved drafts and explicit send behavior. Respect negotiated bracketed-paste behavior and distinguish paste from individual Enter keystrokes. Verify accidental dismissal, rotation and app restart. A multiline composer is valuable only if it preserves terminal semantics and leaves output readable.

<a id="issue-23"></a>
## 23 — Mouse-mode TUI interaction conflicts with touch selection

**Affected:** UserLAnd and reported ConnectBot terminal interactions. **Evidence: R.** [UserLAnd #2389](https://github.com/CypherpunkArmory/UserLAnd/issues/2389), November 7, 2026, supplies a mouse-mode selection reproducer; [ConnectBot #2351](https://github.com/connectbot/connectbot/issues/2351) concerns mouse/scroll-wheel events.

**Remedy:** deliberate modes/gestures for selection, local history and remote mouse events, without silently stealing the same gesture. Test tmux mouse mode, Vim and full-screen TUIs. Existing functionality varies by app/version; do not describe every client as missing TUI support.

<a id="issue-24"></a>
## 24 — Hardware-keyboard and touchpad inconsistencies

**Affected:** reported ConnectBot touchpad setup; keyboard-dependent problems also occur in UserLAnd and other device combinations. **Evidence: R + H.** [ConnectBot #2549](https://github.com/connectbot/connectbot/issues/2549) records a touchpad concern; [UserLAnd issue tracker](https://github.com/CypherpunkArmory/UserLAnd/issues) includes physical-meta/right-click reports.

**Remedy:** configurable mappings and a real-device matrix for modifiers, function keys, right-click and wheel events. Treat laptop-style tablet operation as a separate acceptance path from touchscreen-only use. Existing shortcut/custom-keyboard features must be benchmarked before claiming absence.

<a id="issue-25"></a>
## 25 — Zoom, resize and activity changes corrupt displayed output

**Affected:** specific Termux versions/device combinations. **Evidence: R.** [#5203](https://github.com/termux/termux-app/issues/5203), July 23, 2026, reports pinch corruption after heavy output on 0.118.3/Android 15/Redmi K80 Pro. [#5433](https://github.com/termux/termux-app/issues/5433), November 10, 2026, reports missing non-newline output after activity switching on **0.119.0-beta.3**, Android 10.

**Remedy:** preserve buffer/cursor state, validate reflow/alternate-screen transitions and stress resizing during output. Distinguish rendering loss from process/data loss. These reports do not establish that every current stable release is affected.

<a id="issue-26"></a>
## 26 — Essential terminal workflows inaccessible with TalkBack

**Affected:** the blind-user selection workflow reported in Termux. **Evidence: R.** [#5360](https://github.com/termux/termux-app/issues/5360), October 2, 2026, says output is readable by TalkBack but not selectable through its Select mode, blocking precise copy of errors/addresses.

**App-solvable promise:** expose navigable/selectable output and a labeled composer/actions through accessibility semantics; support character/word/line selection, copy, session changes and controlled live announcements. Validate with blind users and TalkBack, including high-volume output. This is a critical switching reason for the affected cohort, not an estimate of total-market size. Reading a canvas aloud is insufficient.

<a id="issue-27"></a>
## 27 — Failure diagnosis crosses too many layers

**Affected:** local runtime, package, permissions, transport, host, multiplexer and TUI workflows. **Evidence: D + H.** [Andronix reports/doctor](https://docs.andronix.app/troubleshooting/report-a-problem) and [Moshi troubleshooting](https://getmoshi.app/docs/troubleshooting) already diagnose many layers.

**Remedy:** identify the failing layer, show a specific next action and export redacted diagnostics with consent. Separate expected backend limits from defects. A generic doctor is high utility but not a standalone differentiator; project reproducibility, versioned compatibility and recoverable repair must exceed existing implementations to justify switching.

<a id="issue-28"></a>
## 28 — Signatures, plugins and channel migration complicate installation

**Affected:** Termux app/plugins, TermOne Plus and cross-source installations including UserLAnd. **Evidence: D + R.** [Termux installation rules](https://github.com/termux/termux-app) require matching source signatures for cooperating plugins. [UserLAnd #2381](https://github.com/CypherpunkArmory/UserLAnd/issues/2381), October 26, 2026, reports channel/update confusion.

**Remedy:** show the installed channel, compatible update source and an export/restore migration path. An app cannot arbitrarily replace a package signed by another key. Never tell users to uninstall a data-bearing environment before a verified export, or imply every source's plugin APKs can be mixed.

<a id="issue-29"></a>
## 29 — Capability drift between distribution channels

**Affected:** documented Termux variants, TermOne Plus and Acode Play/non-Play storage capabilities. **Evidence: D.** [TermOne Plus](https://www.termoneplus.com/) distinguishes Play/F-Droid capabilities; [Acode terminal settings](https://docs.acode.app/user-guide/settings/terminal-settings) excludes All Files Access from Play; [Termux](https://github.com/termux/termux-app) distinguishes its experimental Play route.

**Remedy:** a maintained channel/capability matrix and feature detection. Publishing the same UI cannot make platform/store restrictions disappear. An unsupported broad permission should not be presented as required for ordinary project access when a scoped import/workspace flow suffices.

<a id="issue-30"></a>
## 30 — Subscription boundaries around advanced features

**Affected:** commercial feature tiers including Termius and Moshi. **Evidence: D + H.** [Termius pricing](https://termius.com/pricing) and [Moshi documentation](https://getmoshi.app/docs) describe free/paid capabilities. Prices and entitlements change; verify platform and plan before comparison.

**Remedy:** clear local/offline guarantees, transparent entitlement and export policies, and a sustainable model. Cheaper/free alone is not a durable product advantage. Core SSH/SFTP/Mosh functionality must not be labeled universally paid in Termius; substantial free capabilities already exist.

<a id="issue-31"></a>
## 31 — External automation callback regression

**Affected:** Blink x-callback-url automation users. **Evidence: maintainer statement + R.** [Blink #2253](https://github.com/blinksh/blink/issues/2253), opened May 5, 2026, describes removal of the old callback approach, a planned replacement and affected-user workflows.

**Remedy:** stable documented intents/Shortcuts contracts, result/error callbacks and migration guidance. Recheck released replacements before asserting an unresolved absence. This may be high severity for automated workflows while remaining a niche market opportunity.

<a id="issue-32"></a>
## 32 — Desktop-oriented UIs squeezed into a small viewport

**Affected:** distro desktops and complex TUIs across Andronix, UserLAnd, native VM desktops and remote clients. **Evidence: H, informed by documented desktop/terminal modes.** [Andronix desktop documentation](https://docs.andronix.app/) and [Blink Code](https://docs.blink.sh/advanced/code) show the relevant interaction models.

**Remedy:** adaptive panels, readable font defaults, foldable/tablet layouts and task-oriented views. External displays or remote/mobile-specific interfaces may improve particular tasks. Do not promise that arbitrary desktop software becomes usable on a phone simply by scaling it. Validate task completion rather than judging screenshots alone.

<a id="issue-33"></a>
## 33 — Copied output contains application-inserted hard wraps

**Affected:** wrapped TUI/agent output; explicitly addressed by Moshi guidance. **Evidence: D.** [Moshi clipboard documentation](https://getmoshi.app/docs/clipboard) describes code arriving wrapped at terminal width.

**Remedy:** preserve terminal soft-wrap metadata when available, offer file/transcript copy and make any dewrapping explicit. When the remote application actually emitted newline bytes, the client cannot always infer original code structure safely. Separate this from ordinary terminal reflow and never silently rewrite a copied command.

## Opportunities that pass the app-solvable filter

| Coherent product bet | Catalog traceability | Reviewable outcome | Main validation risk |
|---|---|---|---|
| Durable output workspace | 06; supporting 22, 25, 27 | Received output and drafts survive app restart, remain searchable/exportable, and show gaps. | Users may already meet this need with host transcripts; never imply unreceived output recovery. |
| Recoverable project/environment lifecycle | 15, 16 | Interrupted update can roll back; user files export without starting the runtime; restore is verified. | Existing backup tools may suffice; runtime execution restrictions remain independent. |
| Coherent editor/runtime project | 18; supporting 17 | Editor, LSP, shell and runner use the same identified app-owned project. | Import/sync copies can cause confusion; arbitrary SAF POSIX parity is out of scope. |
| Recoverable remote editing | 19; supporting 03, 04 | Saved drafts, clear pending state and remote-change checks prevent ambiguous overwrite. | Competitors already integrate editing; switching value must be shown on Android workflows. |
| Accessible terminal interaction | 26; supporting 21–24 | A blind user independently reads, selects, copies, composes and changes sessions. | Requires direct cohort testing and sustained accessibility engineering. |

These are design candidates, not five independent features to build simultaneously. A focused release should prove one workflow advantage and its supporting reliability before expanding scope.

# Competitive audit: mobile terminals and developer consoles

Research snapshot: **2026-11-25**. Primary platform: Android; iOS is a comparison platform. This is desk research of first-party documentation, source repositories and issue reports, not a hands-on device benchmark or a statistically representative user survey.

The strongest opportunity is a terminal workspace that makes projects, output and recovery dependable across mobile interruptions. A new terminal renderer, SSH connection list, extra-key row, AI chat view or Linux installer alone would substantially overlap existing products. The [issue catalog](issue-catalog.md) records all 33 consolidated issues and separates severity, usefulness and feasibility.

## Evidence rules

- **Documented capability/constraint:** a vendor or platform documents the behavior. Documentation can lag released versions; a feature on a website or development branch does not prove every installed version supports it.
- **User report:** a concrete report identifies a broken workflow. An open issue is evidence of a problem worth investigating, not its prevalence or a confirmed cause.
- **Product hypothesis:** an architectural or UX inference to validate with users. Missing documentation is not proof that a feature is absent.
- **Market need:** the usefulness labels in the catalog are research judgments. They are not claims of demonstrated willingness to switch or pay.

Severity measures the consequence **when an affected user encounters the issue**. A critical failure on an uncommon device may have lower market value than a widespread high-severity workflow problem. Feasibility applies to a precisely bounded remedy, not a promise to remove OS restrictions.

## Four categories and twelve named products

| Category | Product | Execution model and established strengths | Relevant residual problems |
|---|---|---|---|
| Local environment/emulator | Termux | Android terminal plus a maintained package ecosystem, local shells and optional integrations. | Android lifecycle/execution limits, environment compatibility, channel migration, recovery and terminal ergonomics. |
| Local environment/emulator | TermOne Plus | Maintained successor to Android Terminal Emulator; accesses Android's shell or a separately installed shell. | Shell/tool availability must not be confused with a bundled Linux distribution; channel-specific permissions. |
| Local environment/emulator | Android native Linux Terminal | Selected-device Debian-based VM using AVF. | Device/firmware availability, VM integration and device-specific startup problems. |
| Linux distribution wrapper | UserLAnd | Rootless distribution/app installation and managed sessions. | Reported update recovery, input/selection regressions and background behavior. |
| Linux distribution wrapper | Andronix | Current in-app Linux route plus Termux route; desktop integration, development packs and diagnostics. | PRoot limits and documented in-app feature/backup boundaries. |
| Linux distribution wrapper | proot-distro | CLI-managed Linux userlands and OCI-image workflows without a Docker daemon. | Kernel semantics and syscall overhead; image management is not kernel isolation. |
| Remote developer client | Termius | Cross-platform SSH, Mosh, SFTP and forwarding; organized host workflows. | Android remote-edit round trips; recovery/history and platform/tier differences need careful qualification. |
| Remote developer client | ConnectBot | Open-source SSH/Mosh client, forwarding and terminal keyboard support. | Concrete input/TUI/touchpad reports; release-track differences. |
| Remote developer client | Moshi | Remote agent-oriented workspace, multiplexers, transport alternatives, files, diffs and notifications. | Host prerequisites, bounded scrollback and complete-history recovery. |
| Remote developer client, iOS | Blink Shell | SSH/Mosh, Blink Code and Files integration with offline-capable replicas. | iOS execution/lifecycle limits, synchronization semantics and reported automation regression. |
| Mobile editor/IDE | Acode | Integrated local terminal, editor, LSP, plugins and remote files. | Runtime visibility differs from editor visibility; input, backup and installation edge cases. |
| Visual programming comparison | Pocket Code | Catrobat blocks for mobile games, stories and animations. | Category boundary: not a conventional Unix terminal IDE. |

## Local environments and emulators

### Termux

Termux already supplies a serious local terminal and packages. Its [official repository](https://github.com/termux/termux-app) documents source-signature requirements for the app and plugins, the need for export before changing sources, and Android process-killing concerns. Google Play must not be described as universally abandoned: the project documents a separately maintained experimental Play variant for Android 11+. Package availability and capabilities must be checked for the actual channel.

The [execution-environment documentation](https://github.com/termux/termux-packages/wiki/Termux-execution-environment) explains why Android/Bionic, filesystem paths and execution rules differ from desktop Linux. Running a familiar command is not equivalent to supporting an arbitrary desktop binary or tutorial unchanged. A distro layer can improve userland compatibility without removing the Android kernel or app sandbox.

Concrete reports used in the catalog include [TalkBack selection #5360](https://github.com/termux/termux-app/issues/5360), [resize #5203](https://github.com/termux/termux-app/issues/5203) and [output rendering #5433](https://github.com/termux/termux-app/issues/5433). These are version/device-specific reports, not a claim that the whole product is unreliable. [Selection request #5413](https://github.com/termux/termux-app/issues/5413) concerns improving range selection; existing auto-scroll must not be presented as absent.

### TermOne Plus

The [official product page](https://www.termoneplus.com/) describes a terminal using Android's shell or a custom shell installed separately, with multiple windows and startup commands. Additional SSH/OpenSSL tooling is associated with Secure Box. Do not evaluate it as if it promised a Termux-like package distribution. Play and F-Droid differ in permissions/capabilities and their packages cannot simply be interchanged. The [version history](https://termoneplus.com/vers.html) demonstrates ongoing maintenance; an “abandoned emulator” assumption is unsupported.

### Android native Linux Terminal

The [AOSP AVF use-case documentation](https://source.android.com/docs/core/virtualization/usecases) describes a Debian-based **non-protected VM** on selected devices, enabled in Developer Settings. Its WebView connects to a guest terminal service. This is a materially different backend from both Android's `/system/bin/sh` and PRoot.

The [Googlebook Linux Terminal preview](https://developer.android.com/blog/posts/googlebook-linux-terminal-preview-vm) describes additional integration such as files, localhost access and VM controls in that specific preview. It does not establish universal phone availability. The [VPN-related startup report](https://issuetracker.google.com/issues/488270267) is a configuration-specific observation, not proof that every VPN or native Terminal installation fails. VM availability is a dependency to detect, not a capability an ordinary app can grant to unsupported phones.

## Linux distribution wrappers

### UserLAnd

The [project repository](https://github.com/CypherpunkArmory/UserLAnd) describes rootless distribution/application installation. Its [getting-started guide](https://github.com/CypherpunkArmory/UserLAnd/wiki/Getting-Started-in-UserLAnd) explains session/filesystem setup and initially minimal package sets. That wiki is older material and should not be treated as a complete description of the current interface.

Recent reports provide more actionable evidence: [#2384](https://github.com/CypherpunkArmory/UserLAnd/issues/2384) describes an old Ubuntu session becoming inaccessible after interrupted asset updates while a new session works; [#2390](https://github.com/CypherpunkArmory/UserLAnd/issues/2390) describes Japanese IME/Ctrl problems after a terminal replacement; [#2389](https://github.com/CypherpunkArmory/UserLAnd/issues/2389) reproduces failed long-press selection in mouse mode; [#2395](https://github.com/CypherpunkArmory/UserLAnd/issues/2395) reports delayed/frozen loops after wake-lock changes. These are reported regressions, not measured failure rates.

The older [file-sharing guide](https://github.com/CypherpunkArmory/UserLAnd/wiki/Importing-and-exporting-files-in-UserLAnd) documents export and uninstall-loss concerns. Its suggestion to navigate `Android/data` manually is version-dependent and should not become modern universal guidance.

### Andronix

Andronix should no longer be described solely as a script launcher requiring Termux and VNC. Its current [10.1 comparison](https://docs.andronix.app/install/standalone-and-termux) documents an in-app route on arm64 Android 8+ and a Termux route for other supported devices. Imports from Termux are supported; reverse movement is not documented as equivalent.

[Development packs](https://docs.andronix.app/use/dev-packs) already install common toolchains with space checks and verification. [Diagnostics and reporting](https://docs.andronix.app/troubleshooting/report-a-problem) already provide `andronix doctor`, redacted logs and failure context. A generic “doctor” or one-command Node/Python setup would overlap these capabilities.

The [in-app limitations](https://docs.andronix.app/troubleshooting/in-the-app) include one active distro and experimental graphics; the [10.1 limitations](https://docs.andronix.app/reference/limitations) also document no in-app SSH support and no microphone access. [Backup documentation](https://docs.andronix.app/use/backups) says complete in-app distro backup is not yet available; copying data out remains necessary. [Android file integration](https://docs.andronix.app/use/android-files) provides shared folders, import/share and Files access, with Android 11+ ownership/visibility boundaries. It does not expose the entire phone as a normal Linux filesystem. Where general and specific documentation differ, verify the installed version before making a product claim.

### proot-distro

The current [README](https://github.com/termux/proot-distro) documents OCI-image installation/build/export and caches; saying it cannot use container images is stale. PRoot still translates paths and intercepts syscalls rather than providing a separate kernel. Root, namespace-dependent services, full init systems and filesystem-heavy performance differ from native Linux. Individual long-running processes can run; “PRoot cannot run background processes” is too broad. A familiar image does not imply Docker Engine, BuildKit or privileged-container parity.

## Remote SSH, Mosh and developer clients

### Termius

The [current pricing matrix](https://termius.com/pricing) lists substantial Starter functionality, including SSH, SFTP, Mosh and forwarding. Do not claim SFTP or Mosh universally requires payment. Advanced logs, synchronization and organizational functions need platform/tier verification; exact prices are intentionally not frozen here.

Its [SFTP guide](https://docs.termius.com/organize-and-connect-to-hosts/managing-files-with-sftp) documents both file transfers and switching between a host's terminal and SFTP. Android's graphical file-edit route still involves downloading/editing/uploading. A shorter, recoverable remote-edit flow is a reasonable opportunity, but saying Termius lacks file management or change tracking would be false. Conflict-safe project synchronization is a hypothesis to validate, not a proven widespread defect.

### ConnectBot

Current [official Mosh guidance](https://connectbot.org/guides/mosh/) explicitly documents Mosh, its server/UDP prerequisites and the need for SSH for forwarding. The [repository](https://github.com/connectbot/connectbot) distinguishes development/alpha and maintenance tracks. Historical “SSH-only” comparisons are stale.

[Input report #2667](https://github.com/connectbot/connectbot/issues/2667) concerns a source-built 1.12 alpha, not every stable installation: its composer obscures output, loses drafts and treats multiline input as separate Enter events. [#2351](https://github.com/connectbot/connectbot/issues/2351) concerns terminal mouse/scroll events; [#2549](https://github.com/connectbot/connectbot/issues/2549) concerns touchpad behavior. Existing terminal-keyboard and shortcut support means a new extra-key row alone is not differentiation.

### Moshi

Moshi is a current Android **and** iOS competitor, not just an iOS SSH skin. The [installation guide](https://getmoshi.app/docs/install), [connections guide](https://getmoshi.app/docs/connections) and [documentation index](https://getmoshi.app/docs) describe host setup, transport alternatives, multiplexers, notifications, file/agent integrations and diagnostics. Reattachment, rich agent controls and host troubleshooting already exist. Verify platform parity for each specific feature instead of transferring iOS screenshots into an Android claim.

Its [scrollback guide](https://getmoshi.app/docs/scrolling) is especially useful: an in-app buffer is bounded and holds received/rendered history; it is not a complete permanent host transcript. Mosh screen synchronization cannot recreate output never received. Durable received-output capture is app-solvable; complete offline-period output needs host-side recording and retrieval. This distinction underpins issue 06.

### Blink Shell on iOS

[Blink Code](https://docs.blink.sh/advanced/code) already edits local/remote projects through a VS Code-oriented workflow. [Files integration](https://docs.blink.sh/advanced/files-app) already offers remote replicas, offline access and staged uploads. Its documentation also describes last-change-wins semantics, extension lifetime constraints and noninteractive authentication requirements. These are useful comparison points for explicit synchronization state and recoverability, not proof that mobile remote editing is entirely unsolved.

[Issue #2253](https://github.com/blinksh/blink/issues/2253) reports an external-automation callback regression. Its cohort is narrower than ordinary interactive terminal users. iOS is also subject to [App Review execution rules](https://developer.apple.com/app-store/review/guidelines/) and [background-task constraints](https://developer.apple.com/documentation/backgroundtasks); Blink-style bundled commands should not be interpreted as unrestricted native Linux execution.

## Integrated editors and the category boundary

### Acode

The [terminal guide](https://docs.acode.app/user-guide/terminal) already covers a PRoot/Alpine terminal, editor CLI, tabs, foreground service, installation backup and process UI. The substantial gap is **workspace coherence**: [LSP documentation](https://docs.acode.app/user-guide/language-servers) says external/SAF/remote folders are skipped by STDIO servers by default; opting in uses virtual/cached paths. [AI-agent guidance](https://docs.acode.app/tutorials/ai-agent) similarly distinguishes editable projects from command-executable terminal projects. It also documents resume and edit undo, so those broad concepts are not absent.

[#2549](https://github.com/Acode-Foundation/Acode/issues/2549) reports a keyboard-specific backspace failure. [#3108](https://github.com/Acode-Foundation/Acode/issues/3108) reports an installation permission error on an older SD-storage device; the cause is unconfirmed. [General backup](https://docs.acode.app/user-guide/settings/backup-restore) excludes connections, runtime files and app state; terminal backup is separate. [Discussion #2920](https://github.com/Acode-Foundation/Acode/discussions/2920) illustrates uncertainty over exporting Terminal Public before reinstall.

### Pocket Code

Catrobat's [product description](https://catrobat.org/our-apps/) identifies Pocket Code as visual blocks for games and animations. Keep it in the research because it was requested, but exclude it from Unix-terminal feature scoring. A shell's absence in a learning/creative programming product is not automatically an unmet need.

## Product implications and limits

Prioritize bounded promises that can be demonstrated: preserve received output, recover app-owned workspaces without booting their runtime, verify exports, and make an editor's project match the terminal's working copy. Remote editing should show file identity, unsent changes and the result of save/upload. Accessibility is a core workflow requirement for the affected cohort.

Avoid universal promises of immortal local processes, arbitrary native-binary compatibility, access to other apps' private files, full privileged Docker inside PRoot or VM availability on every device. Foreground services, wake locks, multiplexers and remote execution each solve different parts of those problems.

Validate whether users switch for a complete recovery/project workflow or would prefer these capabilities as integrations with Termux, Acode or an existing SSH client. The audit establishes problems and competing capabilities; it does not establish that a new standalone app is the optimal delivery vehicle.

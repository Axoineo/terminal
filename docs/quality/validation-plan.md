# Terminal Workspace: validation plan

For the API 37 transport gate, include LAN permission denied/granted/revoked cases alongside a public-internet host. Test direct sockets, local hostname resolution and VPN routing against the [documented LAN contract](../architecture/system-design.md#local-network-access-on-the-selected-target-sdk). No such permission flow exists in the transport-free scaffold.

Status: proposed QA and research gates. Last reviewed: 2026-11-25. The procedures and thresholds below have not been run unless a separate result record says so. They are acceptance targets, not benchmark results or proof of market demand.

The [product requirements](../product/product-requirements.md) define the intended behavior; the [roadmap](../product/roadmap.md) identifies when each gate is needed. The repository README lists the checks actually run for the current scaffold.

## Evidence levels and result records

Keep these evidence types separate:

| Type | What it can establish | What it cannot establish |
| --- | --- | --- |
| Primary documentation | Published capability, supported integration, or OS rule | Real-world frequency or satisfaction |
| Issue report | A reported failure under the named configuration | That all builds/devices fail the same way |
| Deterministic test | A policy or fixture behaves as specified | End-to-end Android or server behavior |
| Device fault test | Behavior under a reproduced lifecycle/storage/network fault | Every vendor/device behavior |
| Formative user study | Workflow errors, accessibility barriers, perceived usefulness | Market size or statistically proven superiority |
| Field pilot | Repeat use, actual recovery incidents, switching reasons | Universal demand from a small convenience sample |

For every check or study, record: date, commit/build, tester, device and OS/API, app/server/protocol versions, setup, fixture/input, expected behavior, actual behavior, result, evidence artifact, and unresolved limitations. Use `pass`, `fail`, `blocked`, or `not run`. Never count a blocked or unrun check as a pass.

Store future result records in a dated `docs/quality/results/` subdirectory. Do not create fabricated result files to populate that directory.

## Gate Q0: honest scaffold and domain correctness

Run the available JVM domain tests and Android build/lint checks documented in the README. Add tests for invariants with meaningful failure consequences, rather than mirroring data-class construction or UI text.

| Area | Required cases | Pass condition |
| --- | --- | --- |
| Project path policy | Normal path, repeated separators, `.`, internal `..`, leading/trailing separators, absolute path, traversal beyond root, NUL/invalid forms | Canonical paths are accepted consistently; noncanonical forms are rejected or safely normalized according to the declared contract; escaping paths are rejected |
| Filesystem containment, once I/O exists | Relative symlink within project, link to parent/outside root, link changed between check and open, archive path/link escape | No read/write escapes the allowed root; safe behavior remains when lexical checks pass |
| Conflict decisions | Neither changed; only local changed; only target changed; both changed; equal new content; deletion versus edit; missing/unknown base | Policies never authorize an unacknowledged destructive overwrite |
| Task result policy | Disconnect before send, after send, mid-output, after confirmed result; cancel request without acknowledgment; reconnect | Transport state alone cannot manufacture success/failure/cancellation; uncertainty stays visible |
| Demo UI | Startup, rotation, accessibility traversal | UI clearly says disconnected/demo and cannot execute or fabricate a remote result |

Use adversarial path/property cases when I/O or archive support is added. Lexical path tests alone do not prove filesystem isolation.

Before selecting engines, execute the [technology-evaluation experiments](../architecture/technology-evaluation.md) and [engine decision gates](../architecture/decisions/0004-engine-selection-gates.md). The shortlist is a research decision, not an installed dependency:

| Spike | Comparison and evidence required |
| --- | --- |
| Terminal component | ConnectBot termlib versus the isolated Termux terminal libraries using identical byte streams, scrollback caps, resize/input fixtures, and manual TalkBack tests; record exact revisions, licenses, JNI/ABI cost, throughput, memory, frame behavior, and battery observations |
| SSH/SFTP adapter | Maintained JSch fork versus SSHJ, with cbssh evaluated if its maturity/support fits; test selected algorithms/providers on the minimum supported Android API, host verification, supported key handling, cancellation, rekey, resource cleanup, and negotiated SFTP extensions |
| Runtime, only if proposed | Prove the actual target-SDK/distribution/device execution path, package/update ownership, process behavior, storage budget, and restore strategy with a disposable implementation |

Set performance budgets after recording baseline device measurements and the intended workload; do not invent comparative benchmark results or pick an engine because it is written in native code. A candidate that blocks essential accessibility or trust handling cannot pass on throughput alone.

## Gate Q1: Android persistence and lifecycle

Use an on-device Room test suite for actual persistence behavior and an on-disk database for restart/durability checks. Android provides [Room testing guidance](https://developer.android.com/training/data-storage/room/testing-db). A purely in-memory database cannot prove recovery after process death.

| Fault or transition | Inject at | Required outcome |
| --- | --- | --- |
| Rotation and resize | Editing, selection, conflict dialog, connection attempt | Same project/task stays selected; no duplicate execution request; essential controls remain reachable |
| Background then system process death | Before/after intent commit; before/after output commit | Committed records restore; pending/incomplete work is classified truthfully |
| Force-stop then manual relaunch | Running or unknown task | Saved records remain; no claim that Android will restart background work or preserve a connection |
| Reboot then relaunch | Saved project and journal | Durable records reopen; live transport is disconnected until separately established |
| Database transaction interruption | Task creation, output append, status update | No impossible combination such as a verified result without its task identity; retries are idempotent |
| Startup recovery versus UI recreation/new dispatch | Open multiple collectors, recreate the screen, and race a new task against initialization | Recovery happens once for the process before observations/dispatch depend on it; recreating a UI cannot mark a live task interrupted |
| Wall clock moves backward or forward | Before start, disconnect, late completion, and recovery | Valid evidence is not rejected solely because display time changed; ordering follows the defined state/sequence contract |
| Migration from every supported schema | Nonempty database with projects, tasks, retained output metadata, and sync bases | Counts, identities, ordering, and content survive; destructive fallback is not silently used for user data |
| Low storage / I/O failure | Intent commit, output append, file save, export | Failure is visible, no unsaved output is acknowledged as durable, active data stays recoverable |
| Corrupt or unavailable journal segment | Recovery read | App identifies damaged/gapped output without inventing content or crashing the whole workspace |
| App clear-data / uninstall | After independent export and without export | Documentation and UI match Android behavior: private data is removed; only a retained compatible export can be restored |

Distinguish activity recreation, process death, force-stop, and device reboot in result records. “Don't keep activities” tests activity recreation; it does not by itself cover all process-death or force-stop cases. [Android state guidance](https://developer.android.com/topic/libraries/architecture/saving-states) distinguishes saved UI state from durable application storage.

For journal persistence, test a known numbered stream and compare the recovered committed prefix byte-for-byte. A pass requires no missing acknowledged chunk, no duplicate displayed chunk, and explicit gaps/unknown tail state when evidence is incomplete. Abrupt device power loss is a separate durability experiment; successful process-death tests do not establish power-loss guarantees.

## Gate Q2: storage, SAF, and synchronization

Test a local provider and at least one cloud/document provider after provider support exists. Use controlled fixtures with known byte hashes; include a many-file tree, a large file, Unicode names, case collisions, long names, links, executable bits, and missing/unsupported metadata. Record each provider's capabilities instead of assuming they match local files.

Android's [SAF documentation](https://developer.android.com/training/data-storage/shared/documents-files) describes URI-based access and directory restrictions. The product's canonical copy must work within those rules, not claim they disappeared.

| Scenario | Required result |
| --- | --- |
| User cancels picker or denies/revokes persisted URI permission | No imported project is falsely marked synchronized; regrant/import options are accessible |
| Provider goes offline during enumeration or read | Import stops/retries explicitly; source and completed private data remain intact |
| Provider changes document identity, returns stale metadata, or offers only partial capabilities | Capability-aware handling or an explicit unsupported state; no false success |
| Both local and target change since recorded base | A conflict is shown with both versions retained until resolution |
| Target changes after planning and before final revalidation | Detect the changed revision; save retries or becomes a conflict |
| Target changes between final recheck and rename | Test and document the remaining SFTP/provider race. A cooperating atomic-precondition endpoint must reject it; weaker endpoints must offer save-copy or an explicitly disclosed weaker mode, without a conflict-proof claim |
| Delete on one side and edit on other; rename versus edit; directory/file collision | No silent deletion of user changes; explain the chosen conflict action |
| Interrupted upload/replacement and failed cleanup | Old or staged versions remain discoverable; pending/unknown state is recorded |
| Same operation retried after crash | No duplicate destructive write, repeated rename, or accidental resurrection without the recorded decision |
| Out-of-space during private copy/staging/export | Existing source and active project remain usable; partial artifacts have a documented cleanup path |
| Worktree requires symlink/permission semantics absent from provider | Keep semantics in the private working copy; explain export limitations |
| Concurrent editor save and terminal-generated edit | Detect version change; do not overwrite the newer copy silently |

For remote synchronization, run the same races against a controlled SFTP endpoint once available. Include permission failure, dropped connection, server rename limitations, clock skew, changed host identity, and server-side edits during transfer. Hash/version evidence, not remote timestamps alone, must drive decisions that could overwrite data.

## Gate Q3: transport, terminal, and task evidence

Run only after real transport/emulation integration exists. Use disposable servers and fixture tasks, including a fast read-only command, large numbered output, UTF-8/ANSI output split at arbitrary byte boundaries, an interactive full-screen program, and a mutating command whose duplicate execution is detectable.

| Scenario | Required result |
| --- | --- |
| Unknown, accepted, changed, and revoked host identity | Correct trust prompt/state; reconnect never silently bypasses host-key verification |
| Auth rejection, missing agent/key, key passphrase cancellation | Clear recoverable error without command submission or secret logging |
| Wi-Fi to mobile, offline/online, captive portal, VPN on/off, IP change | Tested transport behavior is documented; task outcome remains independent of connection |
| Server restart or multiplexer/session disappearance | No fabricated reattachment; preserve journal and explain unavailable remote state |
| Disconnect immediately before/after submission acknowledgment | Show verified-unsent or unknown according to evidence; never auto-repeat unknown mutation |
| Completion while client is disconnected | Recover result/output only if the chosen remote facility retained it; otherwise show incomplete evidence |
| Duplicate/out-of-order output or reconnect replay | Deduplicate/order by defined protocol IDs where available; expose unresolved gaps |
| Terminal resize, keyboard show/hide, split ANSI/UTF-8 frames, cursor/reflow | Emulator conforms to supported behavior; no lost/corrupted visible output |
| Huge output and slow persistence | Bounded memory/retention; no hidden main-thread stall or false saved marker |
| Raw shell without semantic integration | Output remains usable, while command boundary/cwd/result claims stay unverified |

When evaluating Mosh, distinguish refreshed terminal screen state from a complete transcript. Do not construct a passing “complete history” test from only the final Mosh screen. An optional remote agent or multiplexer must have its own installed-version, reconnection, authorization, and retention tests.

A clipboard/paste test must include multiline commands, bracketed paste behavior, newline normalization, IME composition, modifier handling, and copy without unintended soft-wrap/newline changes. Confirm potentially destructive pasted commands are not executed by an invisible automatic send.

## Gate Q4: accessibility from the first screen onward

Compose semantics and lint checks help find missing labels/roles and target-size problems; they cannot certify a custom terminal. Review [Android accessibility guidance](https://developer.android.com/guide/topics/ui/accessibility/apps) and combine automated checks with real assistive-technology use.

Minimum manual configurations proposed for the supported matrix:

- TalkBack with touch exploration and linear navigation on a real phone.
- A hardware keyboard with keyboard-only focus traversal; IME choice and hardware shortcut behavior recorded.
- At least one supported braille display/input configuration with a regular braille user; record Android/TalkBack/display firmware and connection method.
- Large font/display scaling, portrait/landscape, dark/light contrast, and tablet/desktop-mode input where supported.

Essential tasks must be testable without visually interpreting the terminal canvas:

1. Create/open a project and identify which copy is authoritative.
2. Reach a command composer, inspect modifier state, enter and review a command, and deliberately send it when transport exists.
3. Navigate output by line/word, select an IP address/error fragment exactly, copy it, and return to command composition.
4. Pause high-volume announcements, inspect current task status, and resume announcements without losing focus.
5. Understand disconnect/unknown result, reopen the journal, and choose a safe next action.
6. Inspect both versions of a conflict, choose/cancel resolution, and read the resulting sync state.
7. Export/restore a workspace and understand progress/error state when those features exist.

Proposed accessibility release gate: every essential task for the stage is independently completable by the tested cohort; zero blocking focus traps, unlabeled essential controls, or reliance on color/drag gestures. Record friction and workarounds even when a task completes. Track braille as unvalidated until real hardware/users have exercised it.

## Gate Q5: backup and managed-environment recovery

Backup QA begins before any reset/repair UI is offered. Local runtime repair remains gated on engine feasibility; the scaffold has no such environment.

| Fixture | Pass condition |
| --- | --- |
| Project, journal, settings, mappings, and deliberate excluded secrets | Manifest accurately reports included/excluded data; restored content matches expected checksums and identities |
| Truncated/corrupt/incompatible archive | Import fails before activation and leaves active workspace unchanged |
| Archive traversal, absolute paths, symlink escape, extreme expansion | Reject safely within documented size/storage limits |
| Interrupted export/restore, out-of-space, user cancellation | No false completed backup/recovery; existing workspace/export remains available |
| Fresh install with retained external export | User can select and verify a compatible restore without depending on removed app-private files |
| Supported environment with broken dependency/package manager | Diagnose or choose staged rebuild; project data remains separate and intact |
| Failed repair health command | Do not mark recovered or activate unusable replacement |
| Environment switch interrupted | On restart, identify the last verified usable environment or explicit recovery route |

The proposed safety gate is zero silent project loss or false verified recovery across the fault suite. This is an engineering acceptance criterion, not proof that every conceivable hardware/OS failure is preventable.

## Gate Q6: comparative product research

Run a small formative study before building broad scope. Proposed recruitment: 8–12 Android developers/operators using their own current workflow, plus a separate 4–6-person TalkBack/braille cohort with relevant experience. These are planning ranges, not recruited participants or a statistical power calculation. Adapt recruitment after piloting tasks; compensate participants and obtain consent for recordings and retained logs.

Use sandbox projects/servers with seeded faults and non-sensitive data. Counterbalance task order to limit learning effects. Compare each participant with their actual baseline configuration and paid/free tier; do not remove useful incumbent features to create an artificial advantage.

| Workflow | Comparison | Measures |
| --- | --- | --- |
| Edit a provider/remote file, run its command, and return the change | Acode with its documented terminal/integration and/or the user's existing Termux workflow; Termius Android remote edit process | Completion, elapsed time, app switches, manual transfers, wrong-copy/run incidents, conflicts understood |
| Recover from an interrupted remote task | Termius configured logs where available; Moshi reattachment; prototype journal | Correct final-status judgment, output recovered, time to locate evidence, repeated-command errors, participant confidence |
| Precise output extraction and command review | Current Android terminal and prototype with same TalkBack/braille setup | Independent completion, selection accuracy, focus failures, assistive workarounds, effort reported |
| Repair a deliberately broken supported runtime | Existing manual repair/reinstall/export process; prototype only after local gate | Files retained, usable tooling verified, time, comprehension of plan, need for expert intervention |
| Optional iOS reference | Blink Shell on supported iOS hardware using its existing connection/editor/file features | Design lessons and capability comparison; do not present raw cross-OS times as a controlled Android benchmark |

Moshi, Termius, Acode, and Blink versions/features may change. Recheck primary documentation and installed behavior immediately before the study; log the exact configuration. A limitation solved by a configuration option is onboarding evidence, not evidence the capability is absent.

Proposed formative continuation criteria: at least 80% independent completion for the primary Android workspace and recovery tasks; a reduction in median app switching relative to each person's baseline; correct distinction between confirmed and unknown result; no silent overwrite or duplicate mutating execution caused by the prototype. Investigate failures and report denominators, individual observations, and uncertainty. Small samples do not support claims of statistical superiority.

For a bounded field pilot, predefine what counts as a core workflow completion and a return session. Collect opt-in aggregate events without raw commands/output by default. Ask what users replaced, when they reverted to prior tools, and which failure made them stop. Report stated preference, demonstrated reuse, and willingness to pay separately. Stop or narrow scope if users cannot identify a repeated advantage beyond existing logs, file transfer, or keyboard controls.

## Device and environment matrix

Select final support versions after checking available SDKs and target policy. Before public release, cover the minimum supported API, target/current stable Android on a Pixel-class device, and a Samsung device with vendor background/input behavior. Include at least one constrained device and a tablet/desktop input configuration if advertised. An emulator supplements, rather than substitutes for, real hardware lifecycle, IME, braille, and vendor tests.

Record CPU architecture, RAM/storage budget, battery mode, background permissions, distribution channel, enabled developer settings, keyboard/IME, provider, server OS, shell, and transport features. Test default Android behavior first; a developer-option workaround must be labeled in the result. Do not apply the Android 15 six-hour `dataSync`/`mediaProcessing` timeout to every foreground service; service type and target SDK matter, as the [official timeout documentation](https://developer.android.com/develop/background-work/services/fgs/timeout) explains.

## Release blockers and maintenance

Block a release for silent overwrite/data loss, false completed tasks, duplicate unknown-result mutations, lost acknowledged journal data, unsafe host-key trust, escaping path/archive writes, unusable essential accessibility flows, or destructive unverified restore. A missing real-device or braille result stays `not run`/`blocked` and limits the corresponding support claim.

Revalidate affected tests after storage/state/protocol changes, schema migrations, engine upgrades, Android behavior changes, or new device support. Recheck market hypotheses after material incumbent releases. Update the reviewed date only after reviewing evidence; preserve older results so regressions and changed assumptions remain visible.

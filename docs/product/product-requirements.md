# Terminal Workspace: product requirements

Status: proposed product requirements, with a limited Android scaffold. Last reviewed: 2026-11-25. Research findings describe documented capabilities or reports; switching behavior and willingness to pay remain hypotheses. Revalidate competitor capabilities, Android policy, and library choices before each release gate.

## Product decision

Build an Android workspace for short, consequential development tasks: open the right project, edit it, run a command remotely, inspect the result, and recover context after interruption. The first product stage owns a local project working copy and a durable task journal. A supported remote connection will provide execution. Local package environments, arbitrary on-device execution, and an iOS implementation are separate research gates.

The product must make the location and state of work clear. A local copy, a selected Android document provider, and a remote directory are different locations. Received output, durably saved output, a live connection, and a confirmed command result are different facts.

This repository is a starting point, not a working terminal client. Its intended initial implementation consists of Kotlin models, domain policies, an Android Room data layer, and a Compose demonstration screen. SSH, Mosh, PTY emulation, editing, SAF synchronization, backups, and recovery workflows are not implemented by that scaffold. Build and test results belong in the [repository README](../../README.md), not in product claims.

## Users and jobs

| Cohort | Job to complete | Current alternative to compare |
| --- | --- | --- |
| Android developer working away from a computer | Make a small project change, run its command, and send the result back without losing track of copies | Acode with a terminal, Termux, or an external SSH client |
| Operator or developer on unreliable connectivity | Inspect a remote task and understand whether it finished after the app or network disappeared | Termius, Moshi, or SSH with a remote multiplexer |
| User maintaining a supported local environment, if this stage is approved | Repair a broken environment while retaining project files and proving the repair worked | Manual package repair, export, reinstall, or rebuild |
| Blind or braille-using developer | Compose a command, navigate output, select the relevant value, and recover focus after reconnecting | Existing terminal with TalkBack/braille support, including its limitations |

The initial audience is users doing bounded tasks from a phone or tablet. Full desktop replacement, fleet administration, arbitrary Android system debugging, and running all Linux software locally are outside the first product stage.

## Evidence and traceability

The [research issue catalog](../research/issue-catalog.md) contains the full severity, need, feasibility, and source assessment. These are the issues that drive this product:

| Research issue | Consequence | Product response | Evidence limit |
| --- | --- | --- | --- |
| [06: durable output/history](../research/issue-catalog.md#issue-06) | Reconnection does not necessarily restore complete prior output or command context | Persist received output and task metadata; distinguish unknown outcomes | Termius logs and Moshi reattachment are prior art. Journaling alone does not establish novelty. |
| [15: damaged environments](../research/issue-catalog.md#issue-15) | A failed environment or upgrade can strand a project | Diagnose and stage non-destructive repair for app-owned supported environments | Reported failures do not prove frequency across the market. |
| [16: fragmented backup/loss](../research/issue-catalog.md#issue-16) | Settings backup can omit terminal files, connections, or working state | Explicit export coverage and tested restore | A backup file's existence does not prove recoverability. |
| [17: SAF/shared storage semantics](../research/issue-catalog.md#issue-17) | Editor-visible files may not support the shell's execution and filesystem needs | App-owned canonical working copy, with explicit import/export or sync | The underlying filesystem/OS restriction remains; this is a workaround. |
| [18: editor/runtime mismatch](../research/issue-catalog.md#issue-18) | An opened project cannot necessarily be used by its runtime, LSP, or command integration | Use one declared project root and propagate its identity | Integrations must be tested individually; a shared label is insufficient. |
| [19: remote edit friction](../research/issue-catalog.md#issue-19) | Manual download, external editing, and upload invite mistakes | Conflict-aware round trip with visible sync status | Basic SFTP and remote editing already exist. |
| [26: TalkBack output selection](../research/issue-catalog.md#issue-26) | Essential output extraction is blocked for affected users | Accessible command composition and line/word output operations | This is critical for the affected cohort; no broad market-size claim follows. |

Supporting primary sources include [Acode terminal documentation](https://docs.acode.app/user-guide/terminal), [Termux execution environment](https://github.com/termux/termux-packages/wiki/Termux-execution-environment), and the [TalkBack selection report](https://github.com/termux/termux-app/issues/5360). Use the catalog for app-specific source details and current status.

## Four hypotheses to test

| Hypothesis | Reason someone might switch | Validation decision |
| --- | --- | --- |
| H1: a consistent workspace reduces copy and location mistakes | The editor, command, file transfer, and preview refer to the intended project revision | Test an edit–run–sync task against the user's current Acode/terminal or Termius workflow. Measure completion, app switches, wrong-copy incidents, and conflict handling. |
| H2: verified repair is preferable to manual reset | A supported environment can be recovered without sacrificing work | Use fault-injected disposable environments. Require restored project checksums and a successful health command. Only build a local engine after its feasibility gate. |
| H3: a durable journal reduces reconstruction after interruption | Users can tell what was submitted, what output was saved, and what result remains unknown | Interrupt known remote tasks, restart the app, and compare reconstructed context with logs/reattachment in Termius and Moshi. |
| H4: accessible interaction enables otherwise blocked work | A TalkBack or braille user can select a precise value and finish a command workflow independently | Co-design and task-test with users of the relevant assistive technology. Automated semantics checks are necessary but insufficient. |

No hypothesis has passed validation in this repository. Proposed study procedures and release thresholds are in the [validation plan](../quality/validation-plan.md).

## Functional requirements

### Canonical project workspace

| ID | Requirement | Acceptance condition |
| --- | --- | --- |
| W-01 | Each project has one app-owned canonical local root, a stable ID, and separately identified optional provider/remote mappings. | Editor, task request, and synchronization records refer to the same project ID and explicit revision. A provider URI is never presented as an executable native path. |
| W-02 | Validate canonical relative project paths and reject traversal outside the root. | Absolute paths, traversal beyond the root, and invalid/noncanonical forms are rejected or safely canonicalized before use. Filesystem operations also enforce containment after symlink resolution; lexical validation alone is insufficient. |
| W-03 | Import or link a selected document tree through Android's supported access model. | Permission denial/revocation, unavailable provider, read-only items, and changed document IDs produce recoverable visible states. The source remains intact if import fails. |
| W-04 | Plan synchronization before writing. | Each operation compares the last synchronized base with both current versions. Observed concurrent changes become an explicit conflict. An unknown base cannot authorize an automatic overwrite. |
| W-05 | Treat save as an operation that can fail, race, or be interrupted. | Recheck the target immediately before replacement, preserve the previous recoverable version where supported, and record partial/unknown results. Ordinary SFTP has no universal atomic revision-check-and-replace; use save-copy or a cooperating endpoint for a stronger guarantee. Any weaker concurrency mode is explicitly disclosed before replacement. |
| W-06 | Make location and freshness readable. | Users can identify the local working copy, remote mapping, unsent changes, conflicts, and last successful synchronization using text and accessibility semantics. |
| W-07 | Preserve shell compatibility as a separate concern. | Importing files does not claim to make arbitrary SAF locations POSIX or executable. Git metadata, links, permissions, large trees, and case collisions have defined handling. |

### Task execution and durable journal

| ID | Requirement | Acceptance condition |
| --- | --- | --- |
| J-01 | Persist a task's intent and destination before transmitting an execution request. | A stable task ID, project/revision, host identity, requested command, and timestamp survive an app restart before a send is attempted. |
| J-02 | Separate task result from connection state. | A disconnected transport cannot mark a task successful, failed, or cancelled without evidence. Loss of confirmation produces a visible unknown outcome. |
| J-03 | Persist ordered received output with an explicit durability boundary. | Every chunk acknowledged as saved can be read after process death. Duplicate chunks do not appear twice; sequence gaps are visible. Uncommitted tail output is never described as durable. |
| J-04 | Restore context without automatically repeating commands. | After restart, the app restores task identity, saved output, and last confirmed state. Restarting or resubmitting requires a deliberate action, especially when the previous result is unknown. |
| J-05 | Reconcile remote state only through supported evidence. | A multiplexer or optional remote agent may confirm task identity/result where available. A Mosh screen refresh is not treated as recovery of unseen output or a complete command log. |
| J-06 | Make journal capture and retention controllable. | Users can pause capture, remove retained records, choose bounded retention, and export selected output. Credentials are not persisted in journal metadata; potential command/output secrets are explained during capture setup. |
| J-07 | Provide a useful baseline without enhanced shell integration. | Raw sessions are labeled as such. Command boundaries, cwd, and exit codes are only shown as verified when supported shell/agent integration supplies them. |

### Environment safety and verified recovery

These requirements apply only after a supported app-owned execution environment is selected. They do not authorize modifying another app's private rootfs or promise arbitrary Linux compatibility.

| ID | Requirement | Acceptance condition |
| --- | --- | --- |
| R-01 | Diagnose before repair and expose the proposed change. | A plan identifies affected environment files, project exclusions, required free space, and recovery options. An unsupported environment gets an actionable unsupported state. |
| R-02 | Preserve project work independently of disposable runtime state. | Fault injection during upgrade/reset does not remove the canonical working copy or its verified export. Storage exhaustion prevents unsafe staging. |
| R-03 | Stage replacement and verify it before switching active runtime. | Failed health checks retain the last usable version or a documented rebuild path. “Recovered” requires a successful health command plus project integrity verification. |
| R-04 | Export backup coverage explicitly. | A manifest identifies included project files, task history, settings, environment metadata, and excluded secrets/cache. The export has an integrity check and compatible format version. |
| R-05 | Restore into a separate destination before replacement. | Restore rejects corrupt/incompatible archives safely, detects path traversal and link escapes, verifies content, and leaves the active workspace untouched on failure. |

### Accessible operation and precise input/output

Accessibility is a foundation of every stage, including the first screen and recovery dialogs.

| ID | Requirement | Acceptance condition |
| --- | --- | --- |
| A-01 | Expose meaningful labels, roles, state, and logical focus order. | Essential project, task, connection, and conflict actions work with TalkBack and keyboard navigation; state is never conveyed solely by color. |
| A-02 | Provide accessible output navigation and selection. | Users can navigate saved output by line/word, select and copy a precise value, search, and return to command composition without losing their place. |
| A-03 | Support deliberate command composition. | Modifiers have announced state, multiline paste is reviewable, and IME composition does not send partial commands. Hardware keyboard shortcuts are discoverable and configurable where the platform permits. |
| A-04 | Control output announcements. | A high-volume stream does not trap focus or flood speech. Users can pause/resume announcements and request the current line or task status. |
| A-05 | Test braille and resizing on real hardware. | Supported braille configurations can compose, navigate, select, and activate essential actions. Large text, display scaling, rotation, and keyboard display do not hide them. |

Android's [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults) are a starting point, including minimum touch targets; custom terminal rendering still needs explicit semantics and manual testing.

## Architecture and operating constraints

The [system design](../architecture/system-design.md) owns module boundaries and detailed state contracts. Product behavior must respect these constraints:

- A disconnected demonstration screen cannot imply a host connection, execute a sample command, or fabricate completed work.
- Persist the journal and project metadata in durable storage. Small saved UI state can restore selected IDs; it is not the durable journal. Android distinguishes these lifetimes in its [state-saving guidance](https://developer.android.com/topic/libraries/architecture/saving-states).
- SSH host identity and credentials require explicit, reviewable handling before any usable transport release. Reconnection must not silently accept a changed host key.
- Android can stop app processes and constrain background execution. Recovery must work when this happens; the product cannot guarantee indefinite local jobs.
- A universal Linux runtime cannot be assumed. Android's [native Linux environment](https://source.android.com/docs/core/virtualization/usecases) is device-dependent, and downloaded execution has platform and distribution constraints.
- Remote protocols have different capabilities. Supported features must follow a tested capability contract rather than the transport's marketing label.

## Proposed success and stop criteria

These are design targets for validation, not observed results:

| Gate | Proposed threshold | Stop or redesign trigger |
| --- | --- | --- |
| Safety | Zero silent overwrites, false completed states, or lost acknowledged journal chunks in the defined fault suite; advertised concurrency protection matches the endpoint's capability | Any occurrence or unsupported stronger guarantee blocks the release gate |
| Workspace utility | At least 80% of participants in an initial task study finish an edit–run–sync scenario independently; median app switching falls relative to each participant's baseline | Users still need their prior tools for the core round trip, or cannot identify the authoritative copy |
| Recovery utility | At least 80% independently find saved output and correctly distinguish confirmed completion from unknown outcome | Users repeat a potentially mutating command because the product falsely implies failure |
| Accessibility | All essential scripted tasks can be completed by the tested TalkBack/braille cohort without sighted intervention; blocking findings are fixed before expansion | Essential operation depends on visual terminal gestures or inaccessible dialogs |
| Market | Repeated use and an explicit switching reason from a defined cohort, captured separately from study-task success | Users prefer existing combinations after real use, or value is only cosmetic |

Recruitment, sample sizes, comparison controls, and results reporting are defined in the validation plan. A small formative study can identify failures; it cannot establish market size or statistical superiority.

## Maintenance and open decisions

The [roadmap](roadmap.md) turns these requirements into implementable milestones. Before transport alpha, decide terminal engine and license, supported shells, remote multiplexer versus optional agent, host-key storage, and journal persistence format. Before managed runtime work, decide execution/distribution feasibility, package ownership, storage budget, and restore strategy. Before public release, establish retention defaults, supported device matrix, export compatibility, and user-support scope.

Recheck sources and competitor behavior at each gate, after a material app release, and after Android background/storage policy changes. Record the checked app version, Android version, source date, reviewer, and changed assumptions in research notes. Do not silently convert a user report or a proposed requirement into an implemented feature or validated market claim.

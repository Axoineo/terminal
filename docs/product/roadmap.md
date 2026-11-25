# Terminal Workspace: implementation roadmap

Status: staged proposal, not a delivery commitment. Last reviewed: 2026-11-25. No dates or effort estimates below are established facts. Each stage ends in an evidence gate; a later stage does not imply the previous gate passed.

The [product requirements](product-requirements.md), [issue catalog](../research/issue-catalog.md), [system design](../architecture/system-design.md), and [validation plan](../quality/validation-plan.md) define the rationale and expected behavior. The repository README records what actually builds and which checks have run.

## Current baseline

The initial scaffold establishes Android/Kotlin/Compose module boundaries, domain models and policies, a Room data layer, and a safe disconnected demonstration UI. It is not a working SSH/Mosh client, terminal emulator, code editor, synchronization engine, environment manager, or backup tool. Test names and model types do not constitute end-to-end product validation.

Track work using the proposed issue IDs below. Split implementation issues further when needed, while retaining the requirement and research links.

## Stage 0: validate the problem and establish a reliable foundation

| Issue | Implementable scope | Done when | Dependencies / traces |
| --- | --- | --- | --- |
| TW-001 — Reproduce competitor workflows | Record exact installed versions and run edit–run–sync, interruption recovery, and accessible-output tasks in Acode, Termius, Moshi, and optional iOS Blink | A dated baseline names task setup, app tier, device, restrictions, and observed outcomes; untested claims stay labeled | H1/H3/H4; issues 06/18/19/26 |
| TW-002 — Establish state contracts | Define project identity, relative paths, sync base/version observations, task intent, connection state, and evidence-backed outcomes | Deterministic policies reject impossible transitions and ambiguous overwrite decisions; model documentation matches code | W-01/W-02/W-04/J-01/J-02 |
| TW-003 — Persist minimal workspace and task records | Implement Room repositories and migrations for real records; keep fixture/demo data separately labeled | A record is written, app relaunched, and record read on a device; migrations have a defined test path | J-01; no transport prerequisite |
| TW-004 — Make the foundation accessible | Add labeled controls, readable status, focus order, large-text behavior, keyboard navigation, and semantics tests | Scaffold's essential actions work with TalkBack and keyboard; failures are logged before adding screens | A-01/A-05; required throughout |
| TW-005 — Select terminal and transport through a spike | Evaluate emulator/protocol libraries, licenses, platform support, host-key handling, and capability boundaries | [Engine gate](../architecture/decisions/0004-engine-selection-gates.md) has a recorded decision and a reproducible spike, not a claimed integration | Required before Stage 2 |

Exit gate: the core policy tests and Android assemble/lint checks pass in a supported build environment, the limited scaffold is honestly labeled, and initial competitor observations identify a concrete switch-worthy workflow. A missing SDK or unrun device check leaves the corresponding gate pending and must be reported rather than marked passed.

## Stage 1: a safe app-owned project working copy

| Issue | Implementable scope | Done when | Dependencies / traces |
| --- | --- | --- | --- |
| TW-006 — Create/import a project | Create a private canonical root; import a user-selected SAF tree with cancellable progress and explicit permission states | Failed/cancelled import does not damage the provider source; unsupported files and name collisions are explained | TW-002/003/004; W-01/W-03/W-07; issues 17/18 |
| TW-007 — Implement file save and containment | Resolve paths safely, handle symlinks, use staging for writes, and preserve recoverable prior content | Traversal/link-escape fixtures cannot write outside the project; disk-full and interrupted-save cases retain a truthful recoverable state | TW-006; W-02/W-05 |
| TW-008 — Implement sync planning and conflict UI | Track a base observation, compare both sides, show local/remote/provider differences, and revalidate before writing | Observed two-sided edits become conflicts; protection matches endpoint capabilities; accessible save-copy/strong-precondition options avoid promising universal SFTP compare-and-swap; cancellation preserves both versions | TW-007; W-04/W-05/W-06; issue 19 |
| TW-009 — Add basic editor and common project context | Bind file tabs, saved content, requested commands, and supported tooling to one project root | A controlled edit–run preparation scenario refers to the intended project/revision; unsupported LSP/provider combinations are explicit | TW-006; W-01; issue 18 |
| TW-010 — Test the workspace against the baseline | Run a formative study using the same fixture tasks as TW-001 | Results identify completed tasks, errors, app switches, confusion about authoritative copies, and specific next changes | TW-008/009; H1 |

Exit gate: the workspace safety suite passes, core screens are usable with TalkBack, and users can identify where their current work lives. This stage can validate storage and UX without claiming to execute commands.

## Stage 2: remote command execution with a durable journal

| Issue | Implementable scope | Done when | Dependencies / traces |
| --- | --- | --- | --- |
| TW-011 — Implement one supported SSH path | Add host profiles, verified host-key enrollment/change handling, supported credentials, connection lifecycle, and a real terminal view | A documented supported server can be connected securely; disconnects and auth/host-key failures are actionable; no silent trust bypass | TW-005; J-01/J-02 |
| TW-012 — Persist command intent and received output | Commit task intent before send; append ordered output with explicit committed boundaries and retention | Forced process death cannot lose chunks the app acknowledged as saved; no success is inferred from a closed socket | TW-003/011; J-01/J-02/J-03; issue 06 |
| TW-013 — Restore task context and reconcile | Restore records after relaunch; identify unknown outcomes; offer explicit reattach/check/restart actions according to capabilities | A killed app restores saved evidence; unknown-result commands are never automatically resubmitted | TW-012; J-04/J-05 |
| TW-014 — Add supported shell integration | Identify command boundaries, cwd, and exit status for a declared shell set; maintain raw-session fallback | Unsupported or absent integration does not fabricate semantic metadata; terminal protocol conformance fixtures pass | TW-011/012; J-07 |
| TW-015 — Add remote round-trip synchronization | Apply conflict-safe planning to a supported remote directory and associate execution with its synchronized revision | Remote changes during edit or upload produce conflicts or retryable errors; the UI identifies the revision the run used | TW-008/011; W-04/W-05/W-06; issue 19 |
| TW-016 — Make terminal output accessible | Add line/word navigation, exact copying, controllable announcements, focus restoration, and braille input support | Real TalkBack/braille users complete command composition, output extraction, and recovery tasks independently | TW-004/011/012; A-02/A-03/A-04/A-05; issue 26 |
| TW-017 — Compare recovery with incumbents | Test the same interrupted remote tasks in the prototype, Termius, Moshi, and optional Blink | Report distinguishes saved client output, server-side retained output, and reconnect-only state; demonstrates value beyond existing logs/reattachment or changes scope | TW-013/016; H3/H4 |

Exit gate: lifecycle, journal, credential/host identity, synchronization, and accessibility checks pass on the declared device/server matrix. Formative participants correctly distinguish a confirmed result from an unknown outcome. A remote connection alone is not this milestone.

Mosh is a later transport option. Its approval requires a specific capability contract; it must not be sold as providing complete unseen remote output or durable command history. A server-side multiplexer or optional agent is a separate dependency, with setup, compatibility, and failure behavior evaluated explicitly. TW-011 also includes the selected target SDK's [LAN permission contract](../architecture/system-design.md#local-network-access-on-the-selected-target-sdk), including denied/revoked access rather than treating every connection timeout as a server failure.

## Stage 3: portable recovery and backup before destructive maintenance

| Issue | Implementable scope | Done when | Dependencies / traces |
| --- | --- | --- | --- |
| TW-018 — Version the export format | Define manifest, included/excluded data, checksums, credentials policy, format version, and compatibility errors | Users can see whether files, journal, settings, and remote mappings are included; excluded data is explicit | TW-003/012; R-04; issue 16 |
| TW-019 — Implement export and staged verified restore | Stream export safely; import into a separate destination; validate integrity and safe paths before activation | Corrupt, truncated, oversized, incompatible, and traversal archives fail without damaging the active workspace; a verified round trip restores test content | TW-018; R-05 |
| TW-020 — Add recoverability diagnostics | Report workspace/DB/export health and supported next actions | A failed check offers a concrete recovery path while preserving original evidence and files | TW-019; H2; issues 15/16 |

Exit gate: a restore drill on a fresh app installation passes with a real export. Uninstall removes app-private data; successful recovery requires an independent retained export. Backup transport or format support must never imply a backup already exists.

## Stage 4: decide whether to own a local runtime

| Issue | Implementable scope | Done when | Dependencies / traces |
| --- | --- | --- | --- |
| TW-021 — Prove local engine and distribution feasibility | Compare bundled supported tools, external Termux cooperation, PRoot, available AVF integration, and remote-only scope | Document execution restrictions, supported devices, Play/sideload channel behavior, package ownership, update strategy, licenses, and background limits using current primary evidence | Stage 2/3 learning; engine gate; issues 15/17 |
| TW-022 — Prototype staged environment repair | For one app-owned supported runtime, stage repair/rebuild and run health checks without touching project work | Injected package damage, disk exhaustion, interrupted repair, and failed verification preserve project content and a documented recovery route | TW-019/020/021; R-01/R-02/R-03 |
| TW-023 — Validate repair value with users | Compare safe repair/rebuild with users' existing recovery workflow using disposable fixtures | Users understand the plan, retain work, verify usable tooling, and prefer it for a specific recurring problem | TW-022; H2 |

Exit gate: implement local execution only if it is feasible on the intended devices/channel and provides value beyond the remote-first stage. Otherwise keep the product remote-first and document the decision. No app-level design can promise arbitrary local processes survive Android indefinitely.

## Stage 5: release hardening and product decision

| Issue | Implementable scope | Done when | Dependencies / traces |
| --- | --- | --- | --- |
| TW-024 — Finish operational quality gates | Complete device matrix, large-output/storage-budget tests, migrations, export compatibility, network transitions, and security review of trust boundaries | All release-blocking findings have reproducible passing checks; residual supported limitations are documented | Validation plan |
| TW-025 — Run a bounded field pilot | Let target users perform real work with capture consent, support, and a known fallback | Report repeat usage, failures, switching reasons, accessibility findings, and abandonment reasons separately from study benchmarks | H1/H3/H4; H2 only if local gate passes |
| TW-026 — Decide launch scope and support model | Choose supported servers/devices, distribution channels, pricing hypothesis, retention defaults, and support responsibility | Claims match tested capabilities; market assumptions and operating costs have explicit owners and evidence | TW-024/025 |

## Work deliberately outside the initial roadmap

An iOS implementation, full desktop GUI streaming, arbitrary Docker/systemd/kernel compatibility, fleet management, AI command generation, broad plugin stores, and theme catalogs do not establish the current switching reason. They can be reconsidered when observed user work calls for them.

## Keeping the roadmap honest

Use `proposed`, `in progress`, `implemented`, and `validated` separately in tracking. Attach a commit and check result to implementation completion; attach a study/fault-test record to validation completion. Record app and OS versions when rechecking competitors. Before expanding scope, re-evaluate whether existing apps have closed the claimed gap.

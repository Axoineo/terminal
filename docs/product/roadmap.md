# Terminal Workspace: implementation roadmap

Revised **26 November 2026**: prioritize a small Android SSH terminal built from existing terminal/SSH libraries. The broader workspace/editor/sync proposal is subsequent work, not a prerequisite for v1. The [delivery estimate](delivery-estimate.md) targets **3–7 focused development days** for fast v1, conditional on dependency fit, security checks and a real-device route; it separates the **6–10 week hardening horizon**.

This is a scope/order decision, not a passing gate or delivery guarantee. See [implementation status](../implementation-status.md) for actual implemented and tested behavior. The foundation contains Android/Compose modules, domain policies and Room records. Experimental workspace work does not imply a working terminal, complete editor/sync product or validated device behavior.

## Stage 0: reuse selection and security verification

| Issue | Status / implementable scope | Done when |
| --- | --- | --- |
| TW-005 — Select existing terminal and transport | Priority v1 work: integrate a maintained component/library rather than implement a parser, renderer or SSH stack | Exact artifacts fit Android and the UI; licenses/notices, source provenance and native/transitive scope are recorded; essential input/auth tests pass |
| TW-024 — Verify the v1 trust boundary and dependencies | Priority v1 subset: inspect resolved graph and current advisories, host verification, secret handling and cancellation | Applicable findings have an explicit disposition; changed host keys block; no trust-all verifier or secret logging; untested coverage is reported |
| TW-001 — Check incumbent baseline briefly | Keep product judgment grounded without delaying the small build for a broad study | Name the intended user's concrete SSH/history task and what the prototype will improve; broader comparative studies remain later |

Record exact versions and review dates. Recheck relevant upstream advisories and vulnerability records for actual artifacts/transitives. No findings from one scan is not proof of security. Do not add many optional engines/protocols to hedge every possibility; select one working path, with an adapter boundary where useful.

Exit gate: one terminal component and SSH library are selected on evidence, their provenance/licenses/security status are reviewable, and essential Android integration has been demonstrated. A failed candidate means a replacement/revised estimate, not relaxed trust handling. The [technology evaluation](../architecture/technology-evaluation.md) is a shortlist, not an installed-library claim.

## Stage 1: fast v1 — SSH, existing terminal and saved history

| Issue | Implementable v1 scope | Done when |
| --- | --- | --- |
| TW-011 — One supported SSH path | Saved hosts, verified host-key enrollment/change handling, supported auth, real terminal input/output and disconnect | A user connects to a supported test host on an Android device and can work; auth, reachability and changed-key failures are actionable |
| TW-003 — Persist saved context | Reuse Room for non-secret host/context/history records with an explicit schema | Saved records reopen after app restart; credentials are separately designed or deliberately not retained |
| TW-012 — Bounded received-output history | Save received output with honest capture boundaries; retain explicit saved commands only where deliberately implemented | Saved content reopens; caps and deletion are defined; uncaptured/disconnected output is never called complete |
| TW-013 — Explicit reconnection | Reopen app context without silently resubmitting commands | Disconnected state is clear; reconnect is deliberate; an uncertain remote outcome is not automatically retried |
| TW-004 — Basic accessible operation | Labels, focus, readable status, keyboard/IME/paste behavior and essential TalkBack route | Essential actions are tested on a real device; broader terminal/braille claims remain unvalidated until tested |

Exit route: saved host → verify identity → authenticate → existing terminal → deliberate disconnect/reconnect → reopen saved history/context. Include changed-host-key/auth rejection, multiline paste, resize, restart, retention limits and secret-handling checks. Build success and instrumentation compilation alone do not pass this device route.

Do not infer individual shell command boundaries or success from a PTY screen/prompt. Saved history means the content actually retained by the implemented feature. Raw received output, explicit command snippets and verified task results are different capabilities. No remote host bootstrap, dotfile changes or package installation happens silently.

The selected target SDK's [LAN permission contract](../architecture/system-design.md#local-network-access-on-the-selected-target-sdk) belongs in the SSH integration when applicable. Mosh, automatic multiplexer setup, SFTP and a cooperating host runner are optional subsequent decisions, not v1 dependencies.

## Stage 2: harden the narrow SSH product

| Issue | Scope after the v1 route | Done when |
| --- | --- | --- |
| TW-002 — Refine execution/evidence contracts | Keep connection state, sent intent, cached output and verified result distinct | Network changes and missing completion evidence never fabricate success/failure |
| TW-014 — Optional supported shell integration | Add only if user workflows need per-command boundaries/cwd/results | Supported shells are explicit and raw-session fallback stays truthful |
| TW-016 — Terminal accessibility | Precise output navigation/copy, announcement control and tested assistive input | Real TalkBack/braille users can perform the advertised route; no hardware claims without testing |
| TW-017 — Compare actual recovery value | Compare the working product with Termius/Moshi and optional Blink using relevant features enabled | Evidence distinguishes cached received output, server-retained output and session reattachment |
| TW-024 — Complete operational hardening | Device/IME/server matrix, auth/crypto interop, lifecycle/network faults, retention and migrations | Release blockers have passing evidence and residual support limits are documented |
| TW-025/026 — Pilot and choose support scope | Bounded field use, repeated-task evidence and launch/support decisions | Actual reuse/failures/switching reasons are recorded separately from stated preference |

The **6–10 week** estimate concerns this hardened narrow product and is conditional, not a promise to complete all later stages. Reuse still requires maintenance/advisory monitoring and regression checks on upgrades.

## Stage 3: optional project workspace, editor and file round trip

These issues retain their IDs from the broader proposal. They are deferred from the fast-v1 critical path. Existing experiments may continue only when they help a demonstrated workflow; no whole stage is marked passed.

| Issue | Proposed scope | Evidence gate |
| --- | --- | --- |
| TW-006 — Create/import a private project | Read-only SAF source import, staged activation, cancellation, explicit limits | Source unchanged; no partial project advertised as complete; provider errors and quotas tested |
| TW-007 — Safe local save/containment | Expected-base digest check, staged file replacement, path/link containment and recovery | Stale saves/faults preserve saved content and drafts under the declared concurrency contract |
| TW-009 — Editor/project context | Browse/open/edit/save/reopen supported files in one project | [Local acceptance route](../quality/validation-plan.md#first-local-workspace-acceptance-route) passes on device |
| TW-008 — Conflict handling | Base/local/target revisions, merge/save-copy and explicit overwrite choices | Two-sided changes are preserved; no universal compare-and-swap claim for ordinary SFTP |
| TW-015 — Optional SFTP round trip | One supported host/location and negotiated save capabilities | Upload/rename interruptions and the check/rename race are handled honestly |
| TW-010 — Validate workspace value | Compare against actual Acode/Termux/Termius workflows | Repeated advantage beyond incumbent configuration is demonstrated |

The planning defaults for the first local experiment are small UTF-8 text up to 256 KiB and import up to 2,000 entries/50 MiB/depth 32. They are product scope limits to verify, not Android limits or benchmark results. Import does not imply source write-back, export or sync.

## Stage 4: optional backup and recovery

| Issue | Proposed scope | Evidence gate |
| --- | --- | --- |
| TW-018 — Version export format | Included/excluded data, checksums, secrets policy and compatibility | Coverage is clear and actual exports match it |
| TW-019 — Staged verified restore | Separate destination, safe paths, size checks and integrity before activation | Corrupt/truncated/malicious exports do not damage active work; fresh-install restore drill passes |
| TW-020 — Diagnostics | Explain concrete supported recovery actions | Failed checks preserve evidence and never claim a repair succeeded without validation |

Uninstall removes app-private data; no documentation or backup format should imply an independent export already exists.

## Stage 5: independently decide managed local Linux

| Issue | Proposed scope | Evidence gate |
| --- | --- | --- |
| TW-021 — Prove execution/distribution feasibility | Supported packaged tools, external Termux cooperation, PRoot/AVF where available, or staying remote-only | Modern target SDK/channel/device path, ownership, licenses, updates and resource limits are demonstrated |
| TW-022 — App-owned environment repair | Staging, verification, rollback and project separation for one supported environment | Interrupted/failed repair preserves work and a verified recovery route |
| TW-023 — Validate repair demand | Disposable broken-environment study against current recovery practices | Users identify a recurring advantage worth the maintenance cost |

This can add months and may be rejected. It does not block v1 or turn a reused terminal component into a bundled Linux distro. No app can guarantee arbitrary local processes survive Android indefinitely.

## Tracking and scope control

Use `proposed`, `in progress`, `implemented`, `compiled` and `validated on device` separately. Attach exact revisions/check results to completion. Broad work such as an iOS app, arbitrary Docker/systemd compatibility, fleet management, AI command generation or a plugin marketplace needs its own evidence and estimate. Re-evaluate after the fast-v1 route and field use before expanding scope.

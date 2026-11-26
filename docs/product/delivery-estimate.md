# Delivery estimate

Planning judgment revised **26 November 2026** for the user's preferred scope: ship a small Android SSH terminal by reusing existing libraries, then decide whether broader workspace features earn their cost. These are conditional engineering estimates, not measured velocity, an external survey or a delivery guarantee.

## Fast v1

With **one experienced Android developer working full time**, a useful first build is a **3–7 focused development day target**, provided an existing terminal component and SSH library fit the app and pass the checks below. Access to a real Android device and a disposable test SSH host is required. This is a small v1, not completion of the broader product proposal.

| Included in fast v1 | Boundary |
| --- | --- |
| Saved host profiles and one secure SSH route | Explicit host-key enrollment, blocking changed-key behavior, supported authentication and clear connection failures |
| Existing terminal component | Integrate maintained code; do not write a parser/renderer, shell, package manager or Linux distro |
| Basic received-output history and explicit saved commands where implemented | Bound retention and state what was actually captured; no claim of complete output while disconnected or automatic per-command result inference |
| Reopen saved app context | Restore hosts/history; reconnect only through an explicit user action and never replay an unknown command |
| Dependency/security verification | Exact versions, source provenance, licenses/notices, transitive dependencies, relevant advisories and tested trust/auth behavior |

Advanced file editing, SAF workspace import, synchronization, conflict merges, a host runner, Mosh, backup/restore, and managed local Linux **do not gate fast v1**. Existing experimental workspace work may remain in the repository without being a required v1 workflow or a completed user-facing capability.

## Focused implementation sequence

| Allocation within the 3–7 day target | Work and stop condition |
| --- | --- |
| Dependency fit and security review | Select one existing terminal component and one SSH library; inspect exact artifacts, compatibility, licenses, provenance and current advisories. A failed fit/security check changes the candidate or estimate. |
| Small SSH vertical slice | Saved host → verify host key → authenticate → real interactive terminal → disconnect. Use supported library APIs; do not silently install anything on the server. |
| Saved context/history | Persist non-secret host metadata and bounded received output; show disconnected/capture-gap state. Any stored credentials need their own verified storage design. |
| Device verification and fixes | Build/install, authenticate, type/paste/resize, reconnect explicitly, reopen history, and test host-key/auth failures. Prioritize failures that block this route. |

These allocations are an execution order, not independently promised day counts. Authentication/IME integration or an unsuitable library can consume the available range. Report that promptly rather than weakening host verification or describing an untested build as ready.

## Security gate for reused code

Reuse reduces implementation work but does not establish security by itself. Before the first build is shared:

1. Record each selected artifact's exact version/commit, upstream project, distribution source and checksum where available. Resolve the actual dependency graph rather than checking only top-level names.
2. Review upstream advisories and a current vulnerability database for those artifacts/transitives. Record the date, tools/sources, applicable findings, mitigation and unresolved coverage. An empty scanner result is not proof that no vulnerability exists.
3. Check licenses, notices and bundled native/crypto components for the exact selected scope. A repository's headline license is not necessarily every component's license.
4. Test unknown and changed SSH host keys before credentials are sent. Reject trust-all sample verifiers; verify supported algorithms, cancellation and key/passphrase handling on Android.
5. Keep secrets out of saved history/logs by design; use deliberate retention and clipboard controls. Document that echoed remote output can contain secrets and that reconnecting cannot recover uncaptured bytes automatically.

This is a bounded security review and verification record, not a promise that every dependency or the finished app is free of security flaws. Broader review remains part of hardening.

## Subsequent delivery ranges

| Milestone | Planning range | What changes |
| --- | --- | --- |
| Fast reusable-library v1 | **3–7 focused development days**, conditional on fit/security checks | Small real SSH terminal with saved hosts/context and bounded saved history |
| Hardened version of that SSH product | **6–10 weeks total planning horizon** | Broader device/IME/network/lifecycle testing, accessibility fixes, retention/migrations, auth interoperability and a bounded pilot |
| Advanced project workspace/editor/sync product | **Re-estimate separately after v1 use** | Add only workflows users repeatedly need; earlier 3–6 month broad-product planning is not the fast-v1 schedule |
| App-owned managed Linux environment | **Separate feasibility gate; potentially months of additional work** | Modern Android execution/distribution, package ownership, updates and repair can make broad scope a 6–12 month-plus undertaking; it is not required for v1 |

The hardening range is not added to the v1 range. Calendar completion depends on actual developer availability, test hardware, server scope and defect findings. The current environment's lack of a connected Android device limits validation; compiled instrumentation is not a device pass.

Use the [roadmap](roadmap.md) for the revised order and [implementation status](../implementation-status.md) for actual code/checks. Re-estimate after the dependency-fit gate and the first real-device SSH route, using observed blockers rather than lines of generated code.

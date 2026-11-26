# Documentation guide

Research reviewed **25 November 2026**. The repository separates observed competitor behavior, proposed solutions, implemented code, and validation results.

## Reading order

| Document | Purpose |
| --- | --- |
| [Competitive audit](research/competitive-audit.md) | Understand all 12 requested apps and current feature corrections. |
| [Master issue catalog](research/issue-catalog.md) | Find all 33 stable issue IDs, affected products, evidence, severity, usefulness, feasibility, and bounded solutions. |
| [Product requirements](product/product-requirements.md) | Understand users, four product hypotheses, scope, acceptance criteria, and stop conditions. |
| [System design](architecture/system-design.md) | Follow storage ownership, synchronization, task/session lifetimes, journaling, security, and runtime boundaries. |
| [Technology evaluation](architecture/technology-evaluation.md) | Compare native UI, terminal engines, SSH libraries, PTYs, and local execution alternatives. |
| [Roadmap](product/roadmap.md) | Follow the fast reused-library SSH v1, then separate hardening and optional workspace/runtime gates. |
| [Delivery estimate](product/delivery-estimate.md) | See conditional engineering ranges, milestone scope, assumptions and reasons to re-estimate. |
| [Dependency/security review](security/dependency-review.md) | Inspect selected library versions, known fixes, provenance and remaining runtime/device checks. |
| [Third-party notices manifest](security/THIRD_PARTY_NOTICES.md) | Reconcile selected and embedded components with the final packaged notices. |
| [Validation plan](quality/validation-plan.md) | Reproduce competitor workflows and test faults, accessibility, persistence, network changes, and recovery. |
| [Development setup](development.md) | Install tools, build, run checks, and understand module dependencies. |
| [Implementation status](implementation-status.md) | See exactly what exists and what has actually been checked. |

## Architecture decisions

1. [Native Android and workspace foundation](architecture/decisions/0001-native-android-and-workspace-core.md).
2. [Storage and conflict-aware save](architecture/decisions/0002-storage-and-conflict-safe-save.md).
3. [Journal and session lifetime](architecture/decisions/0003-journal-and-session-lifetime.md).
4. [Engine selection gates](architecture/decisions/0004-engine-selection-gates.md).

## Recommended product sequence

Build the small Android SSH v1 with host entry, an existing terminal component, trusted host-key pins and optional bounded received-output history first. A saved-profile/command manager is not part of that slice. Verify selected dependencies and the host/auth trust boundary. Harden that route on real devices, then add project editing/sync or a managed local runtime only when their separate demand and feasibility gates pass. The conditional time ranges are in the [delivery estimate](product/delivery-estimate.md).

The four opportunities are **consistent project context**, **verified recovery**, **durable received-output/task evidence**, and **accessible interaction**. Their shared foundation is workspace ownership and honest state. None requires promising unrestricted mobile OS behavior.

## Maintaining the evidence

Every research claim should link to a direct primary source and identify whether it is a documented constraint, a version-specific report, or an inference. Recheck unstable features and issue status before using them in a release claim. Use stable anchors `issue-01` through `issue-33` in requirements and implementation work. Document new evidence and change a decision deliberately; do not silently rewrite a hypothesis as validated demand.

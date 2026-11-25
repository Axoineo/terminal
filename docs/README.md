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
| [Roadmap](product/roadmap.md) | Turn the proposal into 26 implementation issues and evidence gates. |
| [Validation plan](quality/validation-plan.md) | Reproduce competitor workflows and test faults, accessibility, persistence, network changes, and recovery. |
| [Development setup](development.md) | Install tools, build, run checks, and understand module dependencies. |
| [Implementation status](implementation-status.md) | See exactly what exists and what has actually been checked. |

## Architecture decisions

1. [Native Android and workspace foundation](architecture/decisions/0001-native-android-and-workspace-core.md).
2. [Storage and conflict-aware save](architecture/decisions/0002-storage-and-conflict-safe-save.md).
3. [Journal and session lifetime](architecture/decisions/0003-journal-and-session-lifetime.md).
4. [Engine selection gates](architecture/decisions/0004-engine-selection-gates.md).

## Recommended product sequence

Own the project working copy and task records first. Add permission-based file import/export, then one secure remote terminal and SFTP round trip. Measure interruption recovery and accessible operation against current alternatives. Add a managed local runtime only if its separate feasibility and demand gates pass.

The four opportunities are **consistent project context**, **verified recovery**, **durable received-output/task evidence**, and **accessible interaction**. Their shared foundation is workspace ownership and honest state. None requires promising unrestricted mobile OS behavior.

## Maintaining the evidence

Every research claim should link to a direct primary source and identify whether it is a documented constraint, a version-specific report, or an inference. Recheck unstable features and issue status before using them in a release claim. Use stable anchors `issue-01` through `issue-33` in requirements and implementation work. Document new evidence and change a decision deliberately; do not silently rewrite a hypothesis as validated demand.

# ADR 0003: Durable observations and unknown remote outcomes

Date: 25 November 2026. Status: **Accepted design constraint**; full byte recording and transport are future work.

## Context

An Android process, network connection, remote shell and remote job have different lifetimes. An interrupted SSH write may already have caused a remote side effect. Mosh/tmux improve continuity but do not supply a complete permanent transcript or prove every command finished.

## Decision

Persist task intent and observations independently of live connection objects. Record received completion evidence; otherwise classify a lost session/task outcome as unknown. Do not replay commands automatically. Interactive PTY input has no inferred per-command success state. SSH exec exit status is an optional received channel event, not a reliable post-disconnection query. [RFC 4254](https://www.rfc-editor.org/rfc/rfc4254).

Use Room transactions for structured events. A future output recorder has bounded chunks/queues/quotas, monotonic sequence numbers, durable index ordering and explicit truncation/gap markers. UI rendering is independent of recording throughput. Recovery reconciles incomplete operations; it never fabricates missing output. File-backed chunk and database commits need their own reconciliation because they are not one transaction. [SQLite durability assumptions](https://www.sqlite.org/atomiccommit.html).

Remote multiplexer/runner integration is optional and user-selected. A cooperating runner may retain queryable task IDs and logs; no generic exactly-once claim follows. Cached output is readable offline, with boundaries showing periods the phone could not capture. [Moshi's documented scrollback boundary](https://getmoshi.app/docs/scrolling).

## Consequences

The UI must distinguish reconnecting, process persistence, recorded output and known outcomes. A persistent notification/service cannot guarantee immortality. Stronger recovery requires a defined host contract and tests, not more aggressive Android wake locks. [Foreground services](https://developer.android.com/develop/background-work/services/fgs).

Details: [system design](../system-design.md), [validation plan](../../quality/validation-plan.md).

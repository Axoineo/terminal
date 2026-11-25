# Contributing

Read the [documentation guide](docs/README.md), [implementation status](docs/implementation-status.md), and [development setup](docs/development.md) first.

- Keep proposed behavior, implemented behavior, and measured results distinct.
- Trace product work to a requirement and a research issue where applicable.
- Preserve local drafts and existing files on failed operations. Never infer success from disconnect or replay unknown-result commands automatically.
- Keep platform/protocol I/O behind adapters; test deterministic reliability policies on the JVM.
- Build accessibility into each screen and include device-based assistive-technology checks before claiming support.
- Select dependencies through the documented engine gate and retain exact license notices.

Run repository validation, relevant unit tests, Android compilation, and lint before proposing a change. Run connected tests when persistence or platform behavior changes and a device is available. Record checks that were not run and why. Update the docs when actual scope or assumptions change.

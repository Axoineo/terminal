# Contributing

Terminal Workspace is an early Android SSH terminal built around existing terminal and SSH libraries. Useful contributions make that small workflow dependable: connect securely, use the terminal, disconnect, and reopen deliberately recorded output.

Read the [current implementation status](docs/implementation-status.md) and [development setup](docs/development.md) before changing code. The [roadmap](docs/product/roadmap.md) also contains broader workspace ideas; they are not all v1 requirements.

## Where help is useful

- Reproduce real-phone SSH, network-loss and API 37 local-network permission problems with a disposable test host.
- Improve IME, hardware-keyboard, paste, resize and accessible operation, with device details and reproducible observations.
- Strengthen host-trust, cancellation, retained-output, deletion and database-migration tests.
- Maintain pinned dependencies, upstream-fix evidence, packaged notices and CI tooling.
- Correct documentation when a claim exceeds the code or available evidence.

Keep a bug fix or documentation correction small. For a new authentication method, dependency, protocol or substantial feature, describe the use case and proposed scope in an issue before committing to a large implementation. Local Linux, file editing, synchronization and a custom terminal parser are outside the current v1 slice.

## Report a bug safely

For a suspected vulnerability, use the private reporting instructions in [SECURITY.md](SECURITY.md) before opening a public issue.

An ordinary bug report should include:

1. The source commit or APK version/source; a CI run or APK hash helps identify the build.
2. Android version, device model/ABI, keyboard or IME, and relevant permission/battery settings.
3. Minimal steps, expected behavior and actual behavior. For connection problems, include the server software/version, supported host-key type, and whether the route is LAN, internet, VPN or a network transition.
4. A sanitized error or small reproduction using non-sensitive test data; say what you have already tried.

Review attachments before posting. Passwords, private keys, tokens, raw session output, database files and unredacted `known_hosts`/SSH configuration do not belong in public reports. Output can contain secrets echoed by the server even though typed input is not recorded separately. Use placeholders for usernames, private hostnames, addresses and paths; keep enough structure to reproduce the issue.

## Make a change

The SSH v1 is currently on **`feat/fast-v1-ssh`**, proposed in [PR #1](https://github.com/Axoineo/terminal/pull/1); `main` still contains the earlier scaffold. While that PR is open, base v1 fixes on the preview branch and target it in your pull request. Once it merges, use `main`.

From a clean checkout, for example:

```sh
git fetch origin
git switch -c fix/host-approval-cancel origin/feat/fast-v1-ssh
```

Keep unrelated cleanup out of the patch. A pull request should explain the concrete trigger, resulting behavior, why the approach fits, and the checks actually run, including any checks skipped and their reason.

Preserve these boundaries:

- Reuse the selected libraries for SSH, cryptography and terminal emulation. Keep their I/O behind the existing adapters and off the main thread.
- Host identity must be verified before authentication. A changed key, unsupported server or failing test is not a reason to add trust-all behavior or weaken the algorithm policy.
- Connection state, retained output and command completion are different facts. Do not replay input automatically or infer a command result from a disconnect.
- Keep recording opt-in and bounded, preserve explicit deletion, and exclude credentials/typed-input logging. New persistent data needs a retention and migration decision.
- Include accessible labels, focus and input behavior with UI work. Lint or a launch test does not establish TalkBack, braille or real-IME usability.

## Run checks that match the change

Run Gradle sequentially in one checkout. The [development guide](docs/development.md) explains setup, reports and the complete commands.

| Change | Checks |
| --- | --- |
| Documentation or repository scripts | `python3 scripts/validate_repository.py` and `python3 -m unittest discover -s scripts -p 'test_*.py'` |
| Domain or SSH behavior | `./gradlew :core:domain:test :core:data:testDebugUnitTest` |
| Android, UI, resources or dependencies | `./gradlew :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest :core:data:assembleDebugAndroidTest` |
| Persistence, migration or native/UI lifecycle | `./gradlew :core:data:connectedDebugAndroidTest :app:connectedDebugAndroidTest` with an attached supported emulator/device |
| Runtime dependency changes | Export the resolved runtime inventory, run the OSV check, and update the [dependency review](docs/security/dependency-review.md) and [notices manifest](docs/security/THIRD_PARTY_NOTICES.md) |

The data JVM suite uses a real loopback SSH server. The connected suites cover Room and packaged Android/native behavior; neither substitutes for a physical-phone SSH session. Report the device/API and tested route. Do not claim an unrun device check passed or make every documentation edit depend on device access.

## Data, dependencies and documentation

Room schemas in `core/data/schemas/` are versioned artifacts. Schema changes need an explicit migration and tests with existing nonempty data; do not use destructive migration as routine repair.

Pin dependency versions and use the existing approved repositories. Record exact artifacts/revisions, upstream advisories, transitive/native components and license notices. A clean package scan is one check, not proof of security or publisher identity. Changes to the selected engines need the compatibility and trust-boundary checks described in the [security review](docs/security/dependency-review.md).

Update the relevant user/development docs when behavior or setup changes. Put new verification evidence beside its commit, date and scope in [implementation status](docs/implementation-status.md); preserve earlier evidence instead of relabeling it as a current result.

## License and collaboration

The project-wide license has not been selected. Upstream components retain their own licenses and notices; do not assume a neighboring project's license applies here. Identify the origin and license of copied code or assets so the contribution can be reviewed accurately.

Discuss the behavior and evidence, keep feedback specific, and treat other contributors with respect.

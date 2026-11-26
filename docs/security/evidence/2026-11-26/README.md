# Verified prototype evidence

Source revision: `1ed783d5eb171fb9addedaa7acd2bf94ab953f73`. [CI run 45868354385](https://github.com/Axoineo/terminal/actions/runs/45868354385) passed both jobs on 26 November 2026.

- [99 resolved external binaries and SHA-256 fingerprints](runtime-artifacts.tsv).
- [Live OSV query result](runtime-osv-report.json): 99 packages, zero mapped findings. This does not cover every advisory or vendored code; see the [review](../../dependency-review.md).
- [Runtime dependency graph](runtime-dependencies.txt), [APK permissions](apk-permissions.txt) and [native library names](apk-native-files.txt).
- [Five Android persistence tests](android-data-tests/index.html) and [two Android app/native tests](android-app-tests/index.html), run on Android 8 / API 26 x86_64. These are generated CI reports, including their original supporting files.

The run also passed 20 JVM domain tests, 10 JVM SSH/configuration tests, five Python audit contract tests, Android compilation/lint, 16 KiB ZIP alignment and byte equality for all 15 packaged notice assets. This evidence records a debug prototype, not a production security certification or physical-phone SSH interoperability pass.

Debug APK SHA-256: `779dfc1546843991a8b559f853ff100b57d062b0065b28bdeb5ab979b75a348f`. The binary is available in the CI artifact during its seven-day retention window; it is excluded from Git. Publisher signatures and reproducible source-to-binary correspondence have not been established by this record.

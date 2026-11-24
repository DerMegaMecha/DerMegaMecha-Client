# Prism integration validation report

Date: 2026-11-24

## Current source

The Prism companion implementation and its new automated tests have **not** been compiled or executed on this workstation.

| Validation | Result |
|---|---|
| `.\launcher\gradlew.bat test` | **Blocked** before Gradle could start: Windows Group Policy denied `gradlew.bat` (system message: “Dieses Programm wurde durch eine Gruppenrichtlinie geblockt”). |
| Java/JDK availability | No `java` command or `JAVA_HOME` was available. |
| JUnit test results | Not available; do not treat the new tests as passed. |
| Windows app image / installer | Not rebuilt. |
| Prism executable/version detection | Not exercised against an installed Prism Launcher. |
| Prism import, Microsoft sign-in, Minecraft launch, server join | Not tested. |
| GitHub Actions | No workflow result for this change is available. |
| Official DerMegaMecha pack URL | None was found in the repository. The attempted Modrinth API lookup failed TLS validation locally. Set publisher-only `DERMEGAMECHA_MODPACK_URL` before a tagged build; local `.mrpack` selection is available in source. |

No Group Policy, AppLocker, WDAC, Defender, or TLS checks were bypassed. Existing release binaries were not modified.

## Added automated test coverage (not executed)

- Prism install-path detection, missing/invalid executable, safe argument arrays, missing/unsafe instance IDs, invalid server addresses, and process-start failure.
- Forge/Fabric metadata separation, malicious archive paths, unsupported Minecraft version, missing loader metadata, client-unsupported file filtering, executable file rejection, untrusted download source rejection, and declared-hash mismatch.
- Profile/mod tests remain in the project. The direct OAuth/token service and direct Minecraft process launcher were removed after migrating the active application path to Prism.

## Manual tests still required

1. Build on Windows with JDK 21, run all Gradle tests, build the app image, compile the Inno Setup installer, and verify the resulting installer without overwriting existing release files.
2. Install Prism separately from its official download page; verify auto-detection, configured executable selection, and displayed `--version`.
3. Configure the publisher pack URL, download the official `.mrpack`, and verify client downloads/hashes; separately try a local valid pack, path traversal, unsupported versions/loaders, and corrupted hashes.
4. Finish the import in Prism, obtain the actual instance folder name from Prism, enter it, and confirm the explicit “import completed” checkbox enables Play.
5. In Prism, complete Microsoft sign-in using an account that owns Minecraft Java Edition, then verify actual Forge and Fabric startup as applicable.
6. Configure a real server's published Minecraft and pack versions; test actual connection. The app does not check whether the server is online or inspect Prism's instance metadata.
7. Test a Minecraft account without Java ownership and sign-in cancellation/expiry through Prism; these results belong to Prism and are not authenticated or verified by DerMegaMecha.

## Existing artifacts (historical only)

The existing setup binary and release ZIP predate this Prism integration. The previously recorded installer was `release/DerMegaMecha-Launcher-Setup.exe`, 46,893,186 bytes, SHA-256 `A5EEED79951F549795D4011FF8D6B7038625A054FE8E9748F413DEB16B360262`. That hash identifies the old artifact only; it is **not** a hash or validation result for the current source.

The old installer was unsigned. Unsigned Windows binaries may trigger SmartScreen warnings. Do not distribute the old artifact as the new companion launcher.

# DerMegaMecha Launcher

**Your Minecraft. Your Performance. Your Experience.**

This repository contains a JavaFX Windows launcher in `launcher/` and the DerMegaMecha Forge client at the repository root. Android-only project content has been removed.

## Release candidate

An earlier unsigned per-user installer is available at [release/DerMegaMecha-Launcher-Setup.exe](./release/DerMegaMecha-Launcher-Setup.exe), with its earlier documentation bundle at [release/DerMegaMecha-Launcher-Release-Candidate.zip](./release/DerMegaMecha-Launcher-Release-Candidate.zip). These binaries predate the Prism integration and do not contain this companion workflow; do not treat them as the current candidate. Their previous non-elevated installation test does not guarantee that another organization's application-control policy will permit them.

Unsigned binaries can trigger Windows SmartScreen warnings. Do not bypass school or organization application-control rules. See [INSTALLATION.md](./INSTALLATION.md), [release/RELEASE_MANIFEST.md](./release/RELEASE_MANIFEST.md), and [TEST_REPORT.md](./TEST_REPORT.md) for the hash and exact validation status.

## Build the launcher

Requires JDK 21. From `launcher/`:

```powershell
.\gradlew.bat clean test
.\gradlew.bat packageWindowsAppImage
```

The app image is written to `launcher/build/jpackage/DerMegaMechaLauncher/`. To rebuild the Inno Setup installer, first build the app image, then compile `installer/DerMegaMechaLauncher.iss` with Inno Setup 6.

## Status and limitations

DerMegaMecha is a companion launcher: Prism Launcher handles Microsoft sign-in and Minecraft execution. DerMegaMecha does not use a Microsoft Client ID, access Prism's account data, or claim that a process start confirms authentication or game startup. It validates Modrinth `.mrpack` metadata and declared client-file hashes, then opens Prism's documented import workflow. The user finishes import and selects the Prism instance folder ID.

Official pack download is available only in builds configured by the publisher with `DERMEGAMECHA_MODPACK_URL`; users can instead select a local `.mrpack`. Users are never asked to configure that build setting or a Microsoft application. See [launcher/AUTHENTICATION.md](./launcher/AUTHENTICATION.md) for the Prism boundaries and publisher-only pack URL configuration.

The source has not yet been rebuilt or tested for this Prism integration. The JDK 21 GitHub Actions workflow runs launcher tests, app-image packaging, and installer compilation; no result for this change is available yet. The earlier Forge build could not be verified locally because ForgeGradle dependency setup failed TLS certificate validation for `libraries.minecraft.net`; certificate validation has not been disabled.

The launcher supports Modrinth format version 1 for Minecraft 1.21.1 Forge or Fabric packs. This does not establish that a specific DerMegaMecha pack or server is currently published, available, or compatible. The existing release installer predates this integration and must not be presented as containing it.

## Project policies

- The project is not affiliated with or endorsed by Microsoft or Mojang.
- Do not redistribute third-party assets unless their license permits it.
- Do not disable TLS, application-control, or other security checks to make builds or installs pass.
- See [PRIVACY.md](./PRIVACY.md) and [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md).

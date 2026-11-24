# Installation

> **No current installer is available yet.** The installer currently in `release/` predates the Prism integration and does not contain this workflow. Do not run or distribute it as the current launcher. A current installer must be built and validated first; its output is directed to `launcher/build/installer/`, not over the historical release file.

To build the current launcher, use JDK 21 and follow the build instructions in [README.md](./README.md). After a current build is validated and provided:

1. Install Prism Launcher from its official website. If DerMegaMecha does not detect it, use **Datei auswählen** to select `prismlauncher.exe`.
2. Open Prism and complete Microsoft sign-in there. DerMegaMecha never asks for Microsoft credentials or a Client ID.
3. In DerMegaMecha, choose **Offizielles Modpack herunterladen** if enabled for that build, or choose a local `.mrpack` with **Modpack-Datei prüfen und importieren**. After validation, Prism opens its import flow. Finish the import in Prism; DerMegaMecha does not claim that it completed.
4. In Prism, open the imported instance's folder and use its folder name as the instance ID in DerMegaMecha. Select the matching Forge or Fabric profile, check **I finished the import in Prism and selected this instance**, and save settings. The checkbox records your confirmation; DerMegaMecha cannot inspect that Prism instance.
5. Enter the server address and the Minecraft/modpack versions required by the server. **Server beitreten** is enabled only when those entered values match the validated pack metadata. The launcher does not claim the server is online or inspect Prism's instance contents.
6. Use **Spielen** or **Server beitreten**. Prism handles authentication and the actual game launch. Prism/Minecraft determine whether the instance starts and can connect.

The current implementation supports Modrinth format version 1 for Minecraft 1.21.1 packs using Forge or Fabric. Unsupported versions/loaders are rejected.

The installer and native launcher are unsigned. Windows may show a SmartScreen warning. On a school- or organization-managed PC, follow its application-control rules and ask IT to approve the exact file if blocked; do not disable or work around those controls.

Uninstall from Windows Settings > Installed apps, or use the Start Menu uninstaller. User data is stored separately from application files.

See [TEST_REPORT.md](./TEST_REPORT.md) for verified behavior, known limitations, and tests requiring Prism or a real Minecraft session.

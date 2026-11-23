# DerMegaMecha Launcher

**Your Minecraft. Your Performance. Your Experience.**

The JavaFX desktop launcher source is under `launcher/`; the Forge client project remains at the repository root. Read [`launcher/ACCESSIBILITY_AND_ANDROID.md`](launcher/ACCESSIBILITY_AND_ANDROID.md) for user-friendly setup status, Windows packaging notes, and the Android compatibility assessment.

The launcher now provides a simplified German-first dashboard, English toggle, high-contrast preference, large touch-friendly controls, account connection status, and a direct button to open the managed game folder. It intentionally does not claim installation or gameplay readiness while those systems have not been verified. No playable JAR or signed Windows installer is currently provided.

## Build and run during development

Requires JDK 21 and Gradle. From `launcher/`, run `gradle run` (developers only). A retail installer does not yet exist.

## Windows installer build

On Windows x64 with JDK 21 and WiX Toolset installed, run the Gradle `packageWindowsExe` task. It produces an EXE in `launcher/build/installer/`, creates Start Menu and desktop shortcuts, and supports per-user installation. The EXE has not been built or tested in this environment. A signed Microsoft public-client application ID and verified installation/launch flow remain release blockers.

## Android

No Android APK is included. This launcher uses JavaFX and Windows DPAPI sign-in; Android requires a separate app and runtime integration. Forge 52 / Minecraft 1.21.1 compatibility on Android has not been validated.

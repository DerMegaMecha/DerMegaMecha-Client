# Accessibility, simplicity and platform support

## Launcher experience

The desktop home screen now defaults to German, uses short non-technical messages, has large controls, supports a simple German/English switch and a high-contrast preference, and keeps sign-in work off the UI thread. The game folder is exposed through a friendly button rather than asking users to find it themselves.

This is a user-interface improvement, **not a claim that installation or gameplay is complete**. The setup action remains clearly unavailable until the installation and launch pipeline can verify its output; it does not display fake progress. Windows signing-in currently still depends on the publisher configuring a public Microsoft application ID. The app ID must be integrated into a signed release before a child-friendly no-configuration installer can be distributed.

## Windows packaging

A real Windows installer still needs to be produced on a Windows build machine with JDK 21, JavaFX native dependencies and WiX Toolset installed for `jpackage --type exe`. The repository does not currently contain a signed installer or a finished application icon. Do not download or run an alleged installer from an unofficial source.

## Android assessment

The existing launcher is a JavaFX desktop application and Windows DPAPI credential storage is Windows-specific. Standard Minecraft Java Edition is not a native Android application. Community projects such as PojavLauncher provide a separate Android Java runtime/graphics/input layer, but they are not a supported target for this Windows launcher. Compatibility depends on device/GPU, runtime and specific game/mod versions; Forge 52 / Minecraft 1.21.1 / Java 21 / the DerMegaMecha Forge mod combination has not been validated here. Therefore this repository does **not** claim Android play support, and no deceptive Android APK is provided. If mobile support is pursued, it should be a separate Android application/integration and only list the exact game/loader combinations that have been tested on real devices.

## How to reach the game files

In the launcher, choose **Installationsordner öffnen** (or **Open game folder**) to open the managed instance folder in Windows Explorer. The app stores managed files under `%APPDATA%\\.dermegmecha\\instances\\dermegamecha-1.21.1`. This gives users a direct file-access path without requiring them to browse hidden folders manually.

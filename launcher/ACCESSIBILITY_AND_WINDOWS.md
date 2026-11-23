# Accessibility and Windows release status

## Active platform

DerMegaMecha Launcher is a Windows 10/11 desktop application. There is no Android application or Android build in the active project scope.

## Interface

The existing JavaFX UI provides German-first text, an English toggle, high-contrast styling, and large controls. Installation and launch states are source placeholders until verified profile management is implemented.

## Windows packaging

The repository is not yet proven to generate a public installer. A Windows build machine must produce and test `DerMegaMecha-Launcher-Setup.exe` with JDK 21, JavaFX runtime dependencies, WiX Toolset or Inno Setup, current-user installation, shortcuts, uninstall, and optional launcher Java runtime packaging.

## Authentication

Microsoft sign-in requires a registered public Microsoft desktop client ID. It must use OAuth 2.0 authorization code flow with PKCE and DPAPI-protected refresh credentials. It must not embed a client secret.

## Game data

Managed game data should remain in a per-user application-data directory and use separate `profiles/forge-1.21.1` and `profiles/fabric-1.21.1` directories.

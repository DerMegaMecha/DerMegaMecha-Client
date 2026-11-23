# DerMegaMecha Launcher

**Your Minecraft. Your Performance. Your Experience.**

Windows desktop launcher project for Minecraft Java 1.21.1 / Forge 52.x / Java 21. The launcher source is under `launcher/`; the existing Forge mod project remains at the repository root.

## Current implementation status

The desktop shell and a real Microsoft account flow are implemented: system-browser OAuth authorization-code flow with PKCE and state checking, Microsoft token exchange, Xbox Live and XSTS authentication, Minecraft Services token exchange, Java entitlement verification, profile lookup, and Windows DPAPI-protected refresh-token persistence. No Microsoft password is requested or stored, and tokens are not logged.

**The launcher is not yet a complete Minecraft installer or playable launcher.** The Play button is intentionally disabled: Minecraft metadata/library installation, Forge installation, managed-mod download/verification, profiles, and authenticated process launch remain unimplemented. This repository does not yet provide a distributable launcher installer or client JAR. Do not treat the current application as ready for gameplay.

## Requirements

- Windows 10/11 x64
- JDK 21
- Gradle 8.10.2 or newer
- A Microsoft Entra application registration configured as a public/native client with the Xbox Live sign-in delegated permission. Use your own application ID; no client secret belongs in this project.

## Microsoft app registration

Create a public-client app registration in Microsoft Entra ID, configure the native/desktop redirect flow, and enable public client flows as required by the registration. Use the application ID in a local `launcher/launcher.properties` file:

```properties
microsoft.clientId=YOUR_APPLICATION_ID
```

Never commit this local file or a client secret. Copy `launcher/launcher.properties.example` as a starting point. Sign-in requires an eligible Microsoft account that owns Minecraft Java Edition and can authenticate with Xbox services. Persistent sign-in uses Windows DPAPI and is scoped to the current Windows user account.

## Run the desktop shell

```powershell
cd launcher
Copy-Item launcher.properties.example launcher.properties
# Edit launcher.properties and set microsoft.clientId
java -version  # must report Java 21
./gradlew run
```

If Gradle is installed globally, run `gradle run`. JavaFX runtime dependencies are resolved by Gradle. Sign-in opens the system browser and returns to a loopback callback on the local machine.

## Fair play and distribution

This launcher does not bypass ownership checks or provide offline/cracked login. Minecraft game files are not bundled. Any future installer must fetch game assets from Mojang's official metadata endpoints and respect the Minecraft EULA; Forge and each mod must be obtained from their approved sources and licenses. Only install mods after validating version/loader compatibility and hashes where published.

## License

The repository's MIT license covers this project's original source only. Minecraft, Forge, JavaFX, JNA, and any third-party mods retain their own terms and licenses.

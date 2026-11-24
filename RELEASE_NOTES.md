# Release notes - 1.0.0 (unreleased integration work)

## Changes in source
- Replaced the active Microsoft sign-in/account UI with a Prism Launcher companion workflow. Prism handles authentication and Minecraft execution; DerMegaMecha does not access Prism account data.
- Added Prism detection, configurable executable path, documented `--version`, `--import`, `--launch`, and `--server` process calls, and configurable instance-folder IDs.
- Added Modrinth `.mrpack` format-v1 validation for Minecraft 1.21.1 Forge/Fabric packs, safe archive-path checks, allow-listed HTTPS download sources, and SHA-1/SHA-512/size verification for client files.
- Added server configuration and checks against validated pack metadata. The launcher does not claim that a server is online or that a Prism instance is compatible.
- Added tests for Prism detection/arguments/failures and modpack metadata, path safety, source restrictions, and file hash mismatch.
- Removed the former direct OAuth/token service and direct Minecraft process launcher so this application has no internal Microsoft-token handling path.

## Verification status
- This change has not yet been compiled or tested in this environment.
- No app image or installer has been rebuilt. Existing files in `release/` predate this change and must not be distributed as containing the Prism integration.
- Official Modpack download requires the publisher to configure `DERMEGAMECHA_MODPACK_URL`; no authoritative DerMegaMecha pack URL was found in the workspace.
- Import completion, real Microsoft sign-in through Prism, Minecraft startup, and server joining have not been tested.
- Prism is linked as a separately installed application and is not bundled.
- The installer is unsigned; SmartScreen warnings may appear.

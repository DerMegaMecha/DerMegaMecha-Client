# Release checklist

## Required for the current source

- [ ] Run launcher tests and a clean JDK 21 build.
- [ ] Obtain successful Windows GitHub Actions results for launcher tests, app-image packaging, and Inno Setup compilation.
- [ ] Configure the publisher-only `DERMEGAMECHA_MODPACK_URL` to the authoritative HTTPS `.mrpack` URL before creating a release tag.
- [ ] Confirm the pack is Modrinth format version 1, Minecraft 1.21.1, and uses the intended Forge or Fabric loader; review its dependencies and redistribution permissions.
- [ ] Test Prism installation detection, missing/invalid path handling, version detection, import handoff, launch, server argument validation, and error messages on Windows.
- [ ] Import the pack manually in Prism, confirm the exact Prism instance ID, and exercise both supported Forge/Fabric profiles as applicable.
- [ ] Test actual Microsoft sign-in through Prism, Minecraft ownership handling, real game startup, and server join without inspecting Prism's account files.
- [ ] Verify configured server name/address/version values and that the UI does not claim online status or unverified instance compatibility.
- [ ] Rebuild and verify the app image and per-user installer from the current source; do not overwrite existing release binaries until validation is complete.
- [ ] Update artifact hashes and release manifest only after the new build passes.

## Distribution and policy

- [ ] Keep Prism separately installed; do not bundle its GPL-3.0-only binaries without reviewing redistribution obligations.
- [ ] Review Modrinth pack, mod, configuration, and resource-pack licensing.
- [ ] Test non-elevated installation on clean supported Windows accounts.
- [ ] Review privacy policy and third-party notices for the final shipped dependency set.
- [ ] Do not publish a GitHub Release without explicit approval.

The binaries are unsigned. Code signing is not required for this initial build, but SmartScreen warnings may occur. Do not bypass managed-device application-control policy.

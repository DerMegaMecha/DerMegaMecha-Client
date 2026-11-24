# Authentication and Prism integration

DerMegaMecha does not perform Microsoft OAuth and does not ask users to register an application, enter a Client ID, or provide credentials. Prism Launcher handles Microsoft sign-in, account storage, ownership checks, and Minecraft execution using its own supported configuration. DerMegaMecha only starts Prism through its documented command-line options; it never reads Prism account files or tokens.

The former direct-authentication service, account model, direct Minecraft process launcher, generated Microsoft Client ID resource, and release workflow configuration have been removed. Authentication is delegated only to the separately installed Prism application.

## Publisher-only modpack URL

The release publisher can configure the official DerMegaMecha `.mrpack` download location through either:

- Gradle property `dermegamechaModpackUrl`, or
- Environment variable `DERMEGAMECHA_MODPACK_URL`.

The value must be an HTTPS URL hosted on a supported Modrinth/GitHub/GitLab download domain. The GitHub Actions workflow reads the repository variable `DERMEGAMECHA_MODPACK_URL` and requires it for tagged release builds. This is publisher/build configuration; end users do not set it. Builds without it still allow users to select a local `.mrpack`.

The launcher validates the Modrinth format, supported Minecraft/loader metadata, archive paths, declared SHA-1/SHA-512 hashes, file sizes, and approved HTTPS download sources before requesting Prism import. Prism import completion and installed instance contents are not observable through the documented CLI and are not represented as confirmed by DerMegaMecha.

## Prism behavior and limitations

- Official Prism download page: https://prismlauncher.org/download/
- Documented options used: `--version`, `--import`, `--launch`, and `--server`.
- Prism instance IDs are the instance folder names. Users select the instance ID themselves; DerMegaMecha does not scan Prism's private instance/account configuration.
- `--import` opens Prism's import flow. The application does not assume the import completed or guess the new ID.
- A successful process start means only that Prism was started. It does not confirm Microsoft authentication, Minecraft ownership, game startup, or server compatibility.
- Prism Launcher is GPL-3.0-only. This project links users to the official Prism download and does not bundle Prism binaries.

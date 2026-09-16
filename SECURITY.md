# Security Policy

## Reporting a vulnerability

**Please do not open a public issue for a security problem.**

Use GitHub's private reporting instead: **[Report a vulnerability](https://github.com/wangzhishou/OneBox/security/advisories/new)**. That opens a private advisory visible only to you and the maintainer.

If you cannot use that form, contact [@wangzhishou](https://github.com/wangzhishou) directly.

Please include:

- the affected version (`versionName` / `versionCode`, from Settings → About)
- which flavor you are running (`google`, `foss`, or a Chinese channel) — the dependency sets differ, so behaviour differs
- what an attacker gains, and what access they need to start
- reproduction steps or a proof of concept

## What to expect

OneBox is maintained by one person. Realistic timelines:

- **Acknowledgement**: within 7 days
- **Assessment, and a fix or a mitigation plan**: within 30 days for anything that lets one app or one network peer escalate against another
- **Credit**: you will be named in the release notes and the advisory, unless you ask not to be

## Scope

In scope:

- the Android client in this repository, in all flavors
- the local file-transfer server (`feature/file-transfer`), including its HTTP and WebSocket surface
- credentials or secrets that are actually committed to this repository

**Out of scope, or already known and deliberate:**

- **Public identifiers are not secrets.** `google-services.json`, the WeChat appId / corpId, the Google OAuth client ID, the registered domain names and the download URLs are all intentionally public. They are not leaks, and reports about them will be closed as such.
- **`core/r/src/foss/.../UrlConstantsFlavor.kt` hardcodes a guest access token.** This is deliberate: the F-Droid build has no keystore to inject from, and the token is a low-privilege, read-only guest credential. It is not a finding.
- Release builds are signed with a key that is not in this repository. A build you produce yourself is not an official release.
- Denial of service against the AI backend, or costs incurred through a third-party model API key that you supplied yourself.
- Hardening gaps that require a rooted device or physical access with an unlocked bootloader.

If you are unsure whether something is in scope, report it privately anyway. Asking costs nothing; a public issue about a live bug costs a lot.

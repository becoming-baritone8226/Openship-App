# Security Policy

## Supported versions

This is a pre-1.0 community client. Security fixes land on the default branch only.

## Reporting a vulnerability

Please **do not** open a public GitHub issue for security problems that could expose PATs, instances, or user data.

1. Prefer a private report to the repository owner (GitHub Security Advisories if enabled, or a direct message).
2. Include: affected version/commit, impact, and steps to reproduce.
3. Allow reasonable time for a fix before public disclosure.

## What this app stores

- Instance URL, label, auth mode metadata
- Personal Access Token (PAT), encrypted on Android via EncryptedSharedPreferences / Keystore

The app does not send PATs to any third-party server. All traffic goes to the Openship instance URL you configure.

## Hardening tips for operators

- Use HTTPS when the instance is reachable beyond a trusted LAN.
- Scope PATs to the minimum permissions needed.
- Rotate PATs if a device is lost or a token may have leaked.
- Treat cleartext HTTP as development / LAN-only.

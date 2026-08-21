# Openship-App Context

This file is the working handoff for continuing development. Update it when a
decision changes, a study topic is completed, or an implementation milestone
is reached.

## Current Status

- Phase: Implementation — Base Version v0.1.0 (Slices 1 to 4) 100% Completed & Verified
- Implementation status: Slices 1, 2, 3, 4 fully built and verified on device SM-A556E
- Platform: Android only for the base version
- Working app name: Openship-App
- Package: `com.kareemessam.openship`
- Last updated: 2026-08-21

## Product Goal

Build an Android client for Openship that provides a focused mobile experience
for monitoring self-hosted Openship instances. The first version is read-only
and prioritizes real-time information.

## Base Version Scope

- [x] Add an Openship instance by URL and PAT (Multi-instance supported)
- [x] Discover the instance and determine its auth/deployment mode (`/api/health/env`)
- [x] List projects with deployment statuses (`/api/projects`, `/api/deployments`)
- [x] Show live deployment/build logs with auto-scroll & replay (`/api/deployments/:id/stream`)
- [x] Show live server monitoring statistics with gauges & sparklines (`/api/system/monitor/stream`)

Out of scope for the base version:

- iOS support (deferred to future KMP milestone)
- Desktop support
- Deploy, rollback, restart, or other write actions
- Environment variable editing
- Terminal access
- Push notifications
- Full feature parity with the web dashboard

## Locked Architecture Decisions

- Use Kotlin Multiplatform project structure with `shared/` and `androidApp/`.
- Keep networking, API models, repositories, and business logic in
  `shared/src/commonMain` whenever possible.
- Use the official Kotlin MCP SDK for discrete Openship operations.
- Use Ktor SSE directly for live deployment logs and monitoring streams.
- Share one configured Ktor `HttpClient` between MCP and SSE clients.
- Use PAT authentication with `Authorization: Bearer opsh_pat_...`.
- Store multi-instance PATs and instance configuration securely in Android `EncryptedSharedPreferences`.
- Use Koin 4.x for dependency injection.
- Use Compose Multiplatform and Material 3 for the UI with Bottom Navigation (Projects, Logs, Monitor).
- Treat unknown API JSON fields as expected because Openship has no formal API
  versioning; use tolerant serialization (`ignoreUnknownKeys = true`).
- Reconnect MCP sessions after Android backgrounding and resume deployment log
  streams using sequence tracking (`?since=<seq>`).

## Project Setup Completed

- [x] KMP project created at `/home/kareemessam_me/Desktop/Oblian/Openship-App`
- [x] `androidApp` and `shared` modules exist
- [x] AGP 9.0.1 configured
- [x] Kotlin 2.4.10 configured
- [x] Compose Multiplatform 1.11.1 configured
- [x] Android compile/target SDK 36 configured
- [x] Minimum SDK 24 configured
- [x] JDK 21 toolchain configured
- [x] Git repository initialized
- [x] Beads initialized for task tracking
- [x] `AGENTS.md` created
- [x] `opencode.json` created
- [x] `.editorconfig` created
- [x] `.cursorrules` created
- [x] `.github/copilot-instructions.md` created
- [x] `CONTRIBUTING.md` created
- [x] `STUDY_GUIDE.md` completed
- [x] Superpowers agent skills (7) and KMP agent skills (5) installed in `.agents/skills/`
- [x] Dependencies added to `gradle/libs.versions.toml`, `shared/build.gradle.kts`, `androidApp/build.gradle.kts`
- [x] `network_security_config.xml` and Android permissions configured

## Dependencies Configured

- Ktor Client 3.1.1 (Core, ContentNegotiation, Kotlinx JSON, SSE, Logging, OkHttp Engine)
- kotlinx.serialization 1.8.0
- Kotlin MCP SDK 0.15.0 (`io.modelcontextprotocol:kotlin-sdk-client`)
- Koin 4.0.2 (`koin-core`, `koin-compose`, `koin-compose-viewmodel`, `koin-android`)
- AndroidX Security Crypto 1.1.0-alpha06
- Navigation Compose 2.8.0-alpha10
- OkHttp 4.12.0

## Implementation Slices & Progress

| Slice | Scope | Status |
|---|---|---|
| **Slice 1** | Foundations, Tolerant Models, Encrypted TokenStorage, HttpClientFactory, Connect Screen | ✅ Completed |
| **Slice 2** | MCP Client, OpenshipRepository, Projects List Screen, Multi-Instance Switcher | 🟡 In Progress |
| **Slice 3** | Live Deploy Logs SSE Stream, Base64 Decoder, SeqTracker, Monospace Terminal UI | ⏳ Queued |
| **Slice 4** | Live Server Monitor SSE Stream, Circular Gauges, Sparklines, Cloud Notice | ⏳ Queued |

## Next Actions

1. Implement `McpClient` wrapper connecting to `/api/mcp` using `StreamableHttpClientTransport` with token auth.
2. Implement `ProjectRow` and `ProjectsHome` data models with tolerant deserialization.
3. Implement `OpenshipRepository` coordinating MCP tools (`get_projects_home`) and REST fallbacks.
4. Implement `ProjectsViewModel` and `ProjectsScreen` with pull-to-refresh, status pills, and deployment info.
5. Implement `TopAppBar` with active instance status and instance switcher bottom sheet.

## Working Rules

- Update this file and `README.md` after meaningful progress.
- Keep the base scope read-only unless the scope is explicitly changed.
- Do not assume the Openship API is versioned or stable; tolerate additive
  response changes and pin compatibility to a known server release.
- Never log PATs or include them in screenshots, test output, or commits.
- Prefer tests for parsing, authentication headers, reconnection, and state
  transitions before UI polish.
- Use Beads for durable implementation tasks and dependencies.

## Decision Log

| Date | Decision | Reason |
| --- | --- | --- |
| 2026-08-18 | Android only for base version | Openship already has a desktop app; Android fills the clearest gap |
| 2026-08-18 | Read-only base scope | Reduce maintenance risk and validate demand before write actions |
| 2026-08-18 | MCP for discrete operations | AI-native differentiator and avoids hand-rolling many REST wrappers |
| 2026-08-18 | SSE for live features | Openship already exposes deployment-log and monitoring streams |
| 2026-08-21 | Multi-instance support | Manage VPS, staging, and cloud instances seamlessly with active switcher |
| 2026-08-21 | Bottom Navigation model | Quick one-tap switching between Projects, Logs, and Server Monitor |
| 2026-08-21 | Hybrid transport | REST for discovery, MCP for discrete ops, Ktor SSE for live streams |
| 2026-08-21 | Dark Monospace Terminal | Bounded circular buffer (5,000 lines), touch-aware auto-scroll, search filter |
| 2026-08-21 | Power-efficient lifecycle | Disconnect streams on backgrounding, auto-resume with `?since=lastSeq` on foreground |

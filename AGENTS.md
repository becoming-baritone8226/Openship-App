# Openship-App — Agent Instructions

> **Source of truth for AI agents working in this repository.**
> Read this before making any changes. For full API contract details, see [README.md](README.md).

## Project Overview

**Openship-App** is an unofficial Android client for [Openship](https://github.com/oblien/openship), the open-source self-hostable deployment platform. Built with Kotlin Multiplatform (KMP) and Compose Multiplatform, using the **Model Context Protocol (MCP)** as the primary API layer and **Server-Sent Events (SSE)** for real-time streaming.

- **License**: Apache-2.0
- **Status**: Pre-implementation (base version v0.1.0 — read-only)
- **Relationship**: Unofficial community client, built with the Openship founder's blessing

## Architecture & Protocol Strategy

```
androidApp/ (Android only)          shared/ (KMP)
┌──────────────────────┐            ┌──────────────────────────────┐
│  Compose UI          │            │  commonMain/                 │
│  - Screens           │───────────▶│  - client/ (REST, SSE, MCP)  │
│  - Navigation        │            │  - model/ (data classes)     │
│  - ViewModels        │            │  - repository/               │
│  - Koin DI           │            │  - util/                     │
│  - Lifecycle         │            │                              │
└──────────────────────┘            │  androidMain/                │
                                    │  - platform/ (Keystore,      │
                                    │    OkHttp engine)            │
                                    └──────────────────────────────┘
```

**Phased API design**:
- **Phase 1 (Active — Base v0.1.0)**: Resilient REST endpoints (`/api/health/env`, `/api/projects`, `/api/deployments`, `/api/system/servers`) with parallelized coroutine fetching + **SSE** (Ktor SSE plugin) for real-time streams: build/deploy logs (`/api/deployments/:id/stream`) and host monitoring (`/api/system/monitor/stream`).
- **Phase 2 (Roadmap — v0.2.0+)**: **Model Context Protocol (MCP)** (`/api/mcp`) for AI-native agent tooling and automated operations using the official Kotlin MCP SDK (`io.modelcontextprotocol:kotlin-sdk-client` 0.15.0).
- **One shared `HttpClient`** with infinite timeouts on streaming channels and AES-256 token persistence.

## Key Decisions

1. **Scope**: Connect + Parallel Projects Discovery + Live Build Logs (SSE) + Live Monitoring (SSE with Cloud Mode detection). Read-only for v0.1.0.
2. **Platform**: Android only for base target; KMP architecture allows seamless future `iosMain` target addition.
3. **API layer**: Phase 1 REST + SSE foundation; Phase 2 MCP SDK tool client.

## Tech Stack

| Dependency | Version | Status |
|---|---|---|
| Kotlin | 2.4.10 | ✅ Configured |
| Compose Multiplatform | 1.11.1 | ✅ Configured |
| AGP | 9.0.1 | ✅ Configured |
| compileSdk / targetSdk | 36 | ✅ Configured |
| minSdk | 24 | ✅ Configured |
| JDK toolchain | 21 (Zulu) | ✅ Configured |
| Ktor Client | 3.1.1 | ✅ Configured & Active |
| kotlinx.serialization | 1.8.0 | ✅ Configured & Active |
| Kotlin MCP SDK Client | 0.15.0 | ✅ Configured (Phase 2) |
| Koin | 4.0.2 | ✅ Configured & Active |
| AndroidX Security Crypto | 1.1.0-alpha06 | ✅ Configured & Active |
| OkHttp | 4.12.0 | ✅ Configured & Active |
| Navigation Compose | 2.8.0-alpha10 | ✅ Configured & Active |

> **When adding dependencies**: Add to `gradle/libs.versions.toml` version catalog first, then reference in module `build.gradle.kts`. Never hardcode versions in build scripts.

## Module Structure (Actual)

```
Openship-App/
├── settings.gradle.kts              # includes :androidApp, :shared
├── build.gradle.kts                 # root plugins (apply false)
├── gradle/libs.versions.toml        # version catalog
├── gradle.properties                # Kotlin/Gradle/Android flags
├── androidApp/                      # Android application module
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/com/kareemessam/openship/   # UI, DI, lifecycle
│       └── res/                                # icons, strings, themes
└── shared/                          # KMP shared module
    ├── build.gradle.kts
    └── src/
        ├── commonMain/kotlin/com/kareemessam/openship/shared/  # networking, models, repo
        ├── commonMain/composeResources/                        # shared resources
        └── androidMain/kotlin/com/kareemessam/openship/shared/ # Android-specific impl
```

**Package**: `com.kareemessam.openship` (app) / `com.kareemessam.openship.shared` (shared module)

## API Contract Summary

Base URL: `{instanceUrl}` (self-host: `http://localhost:4000`, cloud: `https://api.openship.io`).

### Auth
- **PAT format**: `Authorization: Bearer opsh_pat_<43-char base64url>`
- **Auth modes**: `none` (zero-auth loopback), `local` (better-auth), `cloud` (PKCE OAuth)
- **Discovery**: `GET /api/health/env` → `{authMode, version, deployMode, ...}` (public, drives login UI)
- **SSE auth**: Authorization header only (no query param token support). Use Ktor SSE plugin (not EventSource).

### Feature 1: Health Check
- `GET /api/health` — public, returns `{status, cloudMode, timestamp}`
- `GET /api/health/env` — public, returns full env info including `authMode`

### Feature 2: List Projects
- `GET /api/projects/home` — tag `project:list`, PAT auth. Returns `{success, projects: ProjectRow[], numbers: {...}}`
- `GET /api/projects` — paginated alternative (`?page=1&perPage=20`)
- **ProjectRow**: `id, name, slug, environmentSlug, isApp, gitProvider/gitOwner/gitRepo/gitBranch, framework, latestDeploymentStatus, activeDeploymentStatus, serviceCount, ...`
- **Deployment status FSM**: `queued → building → deploying → ready | failed | cancelled | reconciling`

### Feature 3: Live Build Logs (SSE)
- `GET /api/deployments/:id/stream` — tag `deployment:read`, PAT auth
- **Replay**: `?since=<seq>` or `Last-Event-ID` header. Monotonic `eventId` per session.
- **Events**: `log` (data is **base64-encoded** — decode before display), `progress`, `service-status`, `install-phase`, `prompt`, `complete`, `cancelled`, `end` (terminal), `error`, `ping`
- **Reconnect**: track last `eventId`, reconnect with `?since=<seq>` for replay

### Feature 4: Live Monitoring (SSE)
- `GET /api/system/monitor/stream?serverId=<id>` — tag `server:read`, PAT auth
- **404 in CLOUD_MODE** (self-host only). Missing serverId → 400.
- **Events**: `stats` every 3s (`{cpu, memTotal, memUsed, memAvail, diskTotal, diskUsed, diskAvail, uptime, load1, load5, load15}` — memory/disk in kB), `error` (non-fatal)
- **No replay** — live-only push. Simple reconnect on foreground.

> **Full API contract with Kotlin data classes**: See [README.md §7](README.md#7-api-contract--4-base-features)

## MCP SDK Integration

Official Kotlin MCP SDK: `io.modelcontextprotocol:kotlin-sdk-client` 0.15.0 (Apache-2.0, maintained with JetBrains).

```kotlin
val httpClient = HttpClient { install(SSE) }
val mcpClient = Client(clientInfo = Implementation("openship-android", "0.1.0"))
val transport = StreamableHttpClientTransport(
    client = httpClient,
    url = "${instanceUrl}/api/mcp",
    requestBuilder = { header("Authorization", "Bearer $pat") }
)
mcpClient.connect(transport)  // initialize + negotiate + session
val tools = mcpClient.listTools().tools  // 186 tools, paginated — cache locally
val result = mcpClient.callTool("projects/list", mapOf("page" to 1))
```

**Key points**:
- One `HttpClient` shared between MCP transport and SSE streams
- Cache tool catalog locally (186 tools, paginated via `nextCursor`)
- Refresh on `notifications/tools/list_changed`
- Reconnect on foreground (don't hold MCP Client across backgrounding)
- Credential/token routes **cannot** opt into MCP — use direct REST for those

## Coding Conventions

### Kotlin Style
- **Official Kotlin code style** (`kotlin.code.style=official` in `gradle.properties`)
- 4-space indent, 120 char max line length (enforced in `.editorconfig`)
- No wildcard imports
- `data class` for all API models with `@Serializable`
- `sealed class` / `sealed interface` for discriminated unions (SSE events, UI state)
- `Result<T>` or sealed class for error handling — no exceptions for expected failures

### Serialization
```kotlin
// Always configure tolerance to API changes
val json = Json {
    ignoreUnknownKeys = true      // Openship API has no versioning — tolerate additive change
    isLenient = true
    encodeDefaults = true
}
```

### Compose UI
- **State hoisting**: composables receive state + callbacks, ViewModels own state
- **Single activity**: `MainActivity` + `OpenshipNavHost` + Compose navigation
- **Material 3**: use `material3` components, dynamic color when available
- **Read state in the lowest scope possible**: defer State reads to avoid unnecessary recomposition

### Networking
- All networking code in `shared/commonMain/client/` — no Android imports
- Platform-specific code (Keystore, OkHttp engine) in `shared/androidMain/platform/`
- Use `expect`/`actual` for platform abstractions, or inject via Koin
- One `HttpClient` instance, shared across MCP and SSE

### Error Handling
- Network errors → sealed `ApiResult<T>` (Success, Error, Loading)
- SSE stream errors → emit to StateFlow, don't crash
- MCP `isError` → map to domain error, don't throw

## Testing

```bash
# Unit tests (shared module, JVM)
./gradlew :shared:allTests

# Android unit tests
./gradlew :androidApp:testDebugUnitTest

# Instrumented tests (device/emulator)
./gradlew :androidApp:connectedAndroidTest
```

- Use `kotlin-test` (already in dependencies) for commonMain tests
- Test SSE parsing with sample event data (no live server needed)
- Test MCP wrapper with mock transport
- Test ViewModels with coroutines test utilities

## Build Commands

```bash
# Build debug APK
./gradlew :androidApp:assembleDebug

# Install on connected device
./gradlew :androidApp:installDebug

# Check project health
./gradlew :androidApp:check

# Clean
./gradlew clean
```

## Agent Guidance

### Before Making Changes
1. **Read this file** (AGENTS.md) — it has the architecture, conventions, and API contract
2. **Check `bd status`** — see if there are tracked tasks for the work
3. **Use code-review-graph MCP tools** for structural exploration (faster than grep/glob)
4. **Check the Openship server source** at `../openship/` for API contract details

### When Adding Features
1. Add models to `shared/commonMain/model/` first
2. Add/extend repository in `shared/commonMain/repository/`
3. Add networking client code in `shared/commonMain/client/`
4. Add platform-specific code in `shared/androidMain/platform/`
5. Add ViewModel in `androidApp/`
6. Add Compose screen in `androidApp/`
7. Wire DI in `androidApp/di/`
8. Add tests in `shared/commonTest/` or `androidApp/test/`

### Common Pitfalls
- **Don't use native `EventSource`** — it can't set `Authorization` headers. Use Ktor SSE plugin.
- **Don't hold MCP Client or SSE streams across backgrounding** — reconnect on foreground.
- **Decode base64 log data** — SSE `log` events have base64-encoded `data` field.
- **Track `eventId`/`seq`** for deploy log resume — pass `?since=<seq>` on reconnect.
- **Handle CLOUD_MODE** — monitoring SSE 404s in cloud mode. Hide monitor tab for cloud instances.
- **Use `ignoreUnknownKeys = true`** — Openship API has no versioning. Tolerate unknown fields.
- **Cleartext traffic** — self-hosted Openship on LAN needs `networkSecurityConfig` with cleartext permit.

### Dependencies Not Yet Added
The following are planned but not yet in `gradle/libs.versions.toml`:
- Ktor Client 3.5.2 (core + OkHttp engine + SSE plugin + serialization)
- kotlinx.serialization 1.11.0
- Kotlin MCP SDK Client 0.15.0
- Koin 4.x (core + android + compose)
- AndroidX Security 1.1.0-alpha06 (EncryptedSharedPreferences)
- OkHttp 4.12.x
- Navigation Compose 2.8.x

When adding these, update `gradle/libs.versions.toml` first, then reference in the appropriate `build.gradle.kts`.

## References

- **Openship server source**: `../openship/` (local path) — API routes, SSE event protocols, auth middleware
- **Full API contract**: [README.md §7](README.md#7-api-contract--4-base-features)
- **MCP SDK**: https://github.com/modelcontextprotocol/kotlin-sdk
- **KMP wizard**: https://kmp.jetbrains.com/
- **Contribution guide**: [CONTRIBUTING.md](CONTRIBUTING.md)

<!-- BEGIN BEADS INTEGRATION v:1 profile:minimal hash:6cd5cc61 -->
## Beads Issue Tracker

This project uses **bd (beads)** for issue tracking. Run `bd prime` to see full workflow context and commands.

### Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --claim  # Claim work
bd close <id>         # Complete work
```

### Rules

- Use `bd` for ALL task tracking — do NOT use TodoWrite, TaskCreate, or markdown TODO lists
- Run `bd prime` for detailed command reference and session close protocol
- Use `bd remember` for persistent knowledge — do NOT use MEMORY.md files

**Architecture in one line:** issues live in a local Dolt DB; sync uses `refs/dolt/data` on your git remote; `.beads/issues.jsonl` is a passive export. See https://github.com/gastownhall/beads/blob/main/docs/SYNC_CONCEPTS.md for details and anti-patterns.

## Agent Context Profiles

The managed Beads block is task-tracking guidance, not permission to override repository, user, or orchestrator instructions.

- **Conservative (default)**: Use `bd` for task tracking. Do not run git commits, git pushes, or Dolt remote sync unless explicitly asked. At handoff, report changed files, validation, and suggested next commands.
- **Minimal**: Keep tool instruction files as pointers to `bd prime`; use the same conservative git policy unless active instructions say otherwise.
- **Team-maintainer**: Only when the repository explicitly opts in, agents may close beads, run quality gates, commit, and push as part of session close. A current "do not commit" or "do not push" instruction still wins.

## Session Completion

This protocol applies when ending a Beads implementation workflow. It is subordinate to explicit user, repository, and orchestrator instructions.

1. **File issues for remaining work** - Create beads for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **Handle git/sync by active profile**:
   ```bash
   # Conservative/minimal/default: report status and proposed commands; wait for approval.
   git status

   # Team-maintainer opt-in only, unless current instructions forbid it:
   git pull --rebase
   git push
   git status
   ```
5. **Hand off** - Summarize changes, validation, issue status, and any blocked sync/commit/push step

**Critical rules:**
- Explicit user or orchestrator instructions override this Beads block.
- Do not commit or push without clear authority from the active profile or the current user request.
- If a required sync or push is blocked, stop and report the exact command and error.
<!-- END BEADS INTEGRATION -->

<!-- BEGIN BEADS CODEX SETUP: generated by bd setup codex -->
## Beads Issue Tracker

Use Beads (`bd`) for durable task tracking in repositories that include it. Use the `beads` skill at `.agents/skills/beads/SKILL.md` (project install) or `~/.agents/skills/beads/SKILL.md` (global install) for Beads workflow guidance, then use the `bd` CLI for issue operations.

### Quick Reference

```bash
bd ready                # Find available work
bd show <id>            # View issue details
bd update <id> --claim  # Claim work
bd close <id>           # Complete work
bd prime                # Refresh Beads context
```

### Rules

- Use `bd` for all task tracking; do not create markdown TODO lists.
- Run `bd prime` when Beads context is missing or stale. Codex 0.129.0+ can load Beads context automatically through native hooks; use `/hooks` to inspect or toggle them.
- Keep persistent project memory in Beads via `bd remember`; do not create ad hoc memory files.

**Architecture in one line:** issues live in a local Dolt DB; sync uses `refs/dolt/data` on your git remote; `.beads/issues.jsonl` is a passive export. See https://github.com/gastownhall/beads/blob/main/docs/SYNC_CONCEPTS.md for details and anti-patterns.
<!-- END BEADS CODEX SETUP -->

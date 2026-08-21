# Openship Android — KMP Client

An unofficial Android client for [Openship](https://github.com/oblien/openship), the open-source self-hostable deployment platform with CI/CD. Built with Kotlin Multiplatform (KMP) and Compose Multiplatform, using the **Model Context Protocol (MCP)** as the primary API layer and **Server-Sent Events (SSE)** for real-time streaming.

> **Status**: In Active Development (Phase 1: Foundations & Slices).
>
> **License**: Apache-2.0 (matches Openship, Kotlin MCP SDK, Ktor, Compose Multiplatform — no conflicts)
>
> **Relationship to Openship**: Unofficial community client. Built with the founder's blessing. Governance (official vs endorsed) decided after the founder sees a working base version.

---

## 🚀 Live Progress & Implementation Status

| Milestone / Slice | Status | Key Deliverables |
|---|---|---|
| **Phase 0: Study & Alignment** | ✅ Completed | Completed study guide, `/grill-me` architectural interview & decisions locked. |
| **Phase 1: Agent Skills Setup** | ✅ Completed | Installed 7 Superpowers skills + 5 KMP agent skills into `.agents/skills/`. |
| **Phase 1: Dependency Setup** | ✅ Completed | Pinned Ktor 3.1.1, MCP SDK 0.15.0, Koin 4.0.2, kotlinx.serialization 1.8.0, Security Crypto, OkHttp, Network Security config. |
| **Slice 1: Foundations & Connect** | ✅ Completed | Tolerant Models, Encrypted TokenStorage, HttpClientFactory, Connect Screen & Discovery probe. |
| **Slice 2: Projects & Switcher** | ✅ Completed | ProjectsRepository, ProjectsViewModel, 1:1 Openship ProjectCards, Multi-Instance Switcher Dropdown, Pull-to-refresh. |
| **Slice 3: Live Deploy Logs** | ✅ Completed | SSE Deploy Stream, Base64 decoding, ANSI color parsing, Stage Stepper, Monospace Terminal, Light/Dark themes. |
| **Slice 4: Live Server Monitor** | ✅ Completed | Real 3s SSE Telemetry Stream, Animated Circular Gauges (CPU, RAM, Disk), Live Rolling Sparklines, Load Averages & Uptime. |

---

## Table of Contents

1. [What is Openship](#1-what-is-openship)
2. [Project Goals & Scope](#2-project-goals--scope)
3. [Key Decisions](#3-key-decisions)
4. [Tech Stack & Dependencies](#4-tech-stack--dependencies)
5. [Project Structure](#5-project-structure)
6. [Architecture: MCP + SSE](#6-architecture-mcp--sse)
7. [API Contract — 4 Base Features](#7-api-contract--4-base-features)
8. [Auth Model](#8-auth-model)
9. [MCP SDK Integration](#9-mcp-sdk-integration)
10. [SSE Implementation](#10-sse-implementation)
11. [Android-Specific Concerns](#11-android-specific-concerns)
12. [Build & Development Setup](#12-build--development-setup)
13. [Roadmap](#13-roadmap)
14. [Risks & Mitigations](#14-risks--mitigations)
15. [References](#15-references)

---

## 1. What is Openship

Openship (v0.6.6) is an open-source, self-hostable deployment platform — think Vercel/Netlify that you run yourself. It provides:

- **Git push to deploy** — connect a GitHub repo, Openship builds and deploys it
- **Multi-service support** — monorepo-aware, route changed files to the right service
- **Live build logs** — real-time SSE stream of build/deploy progress
- **Server monitoring** — real-time CPU, memory, disk, load metrics via SSE
- **Terminal access** — WebSocket PTY into running containers
- **Domains & SSL** — automatic domain routing and certificate management
- **Backups** — scheduled and on-demand backup/restore
- **Multi-instance** — self-hosted (local) or Openship Cloud (SaaS)

**Architecture**: Bun/TypeScript monorepo (turbo). Key apps:
- `apps/api` — Hono REST API, ~30 modules, 433 permission-gated routes, 31 public
- `apps/dashboard` — Next.js 16 web dashboard (what users see in browser)
- `apps/desktop` — Electron shell that spawns API + dashboard locally
- `apps/cli` — CLI tool (`opsh`), 30+ commands
- `packages/core` — shared TypeScript types (portable to Kotlin)

The desktop app is a **thin Electron shell** — it spawns the API as a child process and loads the Next.js dashboard in a browser window. All real functionality lives behind the HTTP API. **This KMP client replaces the Electron shell for Android.**

---

## 2. Project Goals & Scope

### Base Version (v0.1.0) — Read-Only

| Feature | Description |
|---|---|
| **Connect** | Add an Openship instance by URL + Personal Access Token (PAT). Store securely. |
| **List Projects** | View all projects on the connected instance with deployment status. |
| **Live Build Logs** | Real-time SSE stream of build/deploy progress (hero feature). |
| **Live Monitoring** | Real-time SSE stream of server CPU/memory/disk/load metrics. |

All four features are **read-only**. No deploy/rollback/service-control in the base version.

### Why This Scope

- Every surviving third-party client in the deployment-platform space leads with **live logs + monitoring**. Real-time is the differentiator.
- Read-only first earns trust — users and the Openship maintainer can verify the app is safe.
- PAT auth with no middleman server keeps App Store review simple and matches what every successful community client does.
- MCP from the start establishes the AI-native angle that no competitor has.

### Platform

**Android only** for the base version. Openship already has desktop (Electron), so Android fills the actual gap. iOS is deferred but the KMP architecture means adding it later is a new `iosMain` target, not a rewrite.

---

## 3. Key Decisions

These decisions were made during brainstorming and are locked:

| # | Decision | Rationale |
|---|---|---|
| 1 | **Scope**: Connect + List Projects + Live Build Logs + Live Monitoring (read-only) | Real-time is the differentiator. Read-only earns trust first. |
| 2 | **Platform**: Android only (iOS deferred) | Openship has desktop; Android fills the gap. KMP architecture allows iOS later without rewrite. |
| 3 | **API layer**: MCP from the start | AI-native angle stands out. Official Kotlin MCP SDK exists and is production-ready. Cuts integration surface (186 tools via one endpoint vs hand-rolling REST routes). |
| 4 | **Architecture**: MCP for discrete ops + SSE for real-time streams, shared Ktor HttpClient | Clean separation of concerns. One HttpClient for both layers = fewer sockets, consistent auth. |

---

## 4. Tech Stack & Dependencies

### Version Catalog (`gradle/libs.versions.toml`)

All versions are pinned. The MCP SDK and Ktor versions are chosen to match each other (the MCP SDK is built on Ktor 3.5.2).

| Dependency | Version | Purpose |
|---|---|---|
| **Kotlin** | 2.4.10 | Language |
| **Compose Multiplatform** | 1.11.1 | UI framework (Android target now, iOS later) |
| **Ktor Client** | 3.5.2 | HTTP + SSE + WebSocket (shared HttpClient) |
| **kotlinx.serialization** | 1.11.0 | JSON serialization (matches MCP SDK) |
| **Kotlin MCP SDK Client** | 0.15.0 | `io.modelcontextprotocol:kotlin-sdk-client` — MCP Streamable-HTTP client |
| **Koin** | 4.x | Dependency injection (KMP-friendly, no codegen) |
| **AndroidX Security** | 1.1.0-alpha06 | EncryptedSharedPreferences for PAT storage |
| **AndroidX Lifecycle** | 2.8.x | Compose lifecycle integration |
| **Navigation Compose** | 2.8.x | Screen routing |
| **OkHttp** | 4.12.x | SSE engine for Ktor on Android |
| **Android Gradle Plugin** | 8.7.x | Build tooling |
| **compileSdk / targetSdk** | 35 | Android 15 |
| **minSdk** | 26 | Android 8.0 (covers ~95% of devices) |

### Why These Choices

- **Koin over Hilt**: Koin is pure Kotlin, works in `commonMain`, no KSP/KAPT codegen, trivially extends to iOS. Hilt is Android-only.
- **No Room/SQLDelight yet**: The base version stores only instance configs (URL + PAT + label) — a small list. `EncryptedSharedPreferences` with JSON serialization is sufficient. Add SQLDelight when you need project caching, deployment history, or offline support.
- **OkHttp as SSE engine**: Ktor's SSE plugin needs an engine on Android. OkHttp is the standard choice and also handles cleartext policy via `networkSecurityConfig`.

---

## 5. Project Structure

```
openship-android/
├── settings.gradle.kts
├── build.gradle.kts                    # root: plugins, version catalog
├── gradle/libs.versions.toml           # version catalog (single source of truth)
├── gradle.properties                   # Kotlin/MPP flags, Android opts
├── app/                                # Android application module
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml         # networkSecurityConfig for cleartext LAN
│       ├── java/com/openship/android/
│       │   ├── OpenshipApp.kt          # Application class, Koin init
│       │   ├── MainActivity.kt         # single-activity, Compose host
│       │   ├── di/                      # Koin modules (client, repo, vm)
│       │   │   ├── ClientModule.kt     # HttpClient, McpClient, SseClient
│       │   │   ├── RepositoryModule.kt # OpenshipRepository binding
│       │   │   └── ViewModelModule.kt  # ViewModels per screen
│       │   ├── ui/                      # Compose screens + navigation
│       │   │   ├── navigation/
│       │   │   │   └── OpenshipNavHost.kt
│       │   │   ├── theme/
│       │   │   │   ├── Theme.kt
│       │   │   │   ├── Color.kt
│       │   │   │   └── Type.kt
│       │   │   ├── screens/
│       │   │   │   ├── connect/        # Add instance (URL + PAT)
│       │   │   │   ├── projects/       # Project list
│       │   │   │   ├── deploylogs/     # Live build log stream
│       │   │   │   └── monitor/        # Live server metrics
│       │   │   └── components/         # Shared composables
│       │   └── work/                    # WorkManager (push-notif prep, later)
│       └── res/
│           ├── xml/
│           │   └── network_security_config.xml
│           ├── values/
│           │   ├── strings.xml
│           │   └── themes.xml
│           └── mipmap-*/               # App icons
└── shared/                             # KMP shared module
    ├── build.gradle.kts
    └── src/
        ├── commonMain/
        │   └── kotlin/com/openship/shared/
        │       ├── client/             # Networking layer
        │       │   ├── HttpClientFactory.kt    # Shared Ktor HttpClient config
        │       │   ├── McpClient.kt            # MCP wrapper (tool catalog, calls)
        │       │   ├── SseClient.kt            # SSE stream client (logs, monitor)
        │       │   └── AuthInterceptor.kt     # Bearer token injection
        │       ├── model/              # Data classes (kotlinx.serialization)
        │       │   ├── HealthEnv.kt           # /api/health/env response
        │       │   ├── ProjectRow.kt          # Project list item
        │       │   ├── ProjectsHome.kt        # /api/projects/home response
        │       │   ├── DeploymentStatus.kt    # FSM states
        │       │   ├── sse/                    # SSE event types
        │       │   │   ├── DeployStreamEvent.kt  # log/progress/service-status/...
        │       │   │   └── MonitorStats.kt       # cpu/mem/disk/load
        │       │   └── InstanceConfig.kt      # Stored instance (url, label, patRef)
        │       ├── repository/         # Data access layer
        │       │   ├── OpenshipRepository.kt  # Interface
        │       │   └── OpenshipRepositoryImpl.kt
        │       └── util/               # Shared utilities
        │           ├── Base64Decoder.kt       # Decode base64 log lines
        │           └── SeqTracker.kt          # SSE sequence tracking for resume
        └── androidMain/
            └── kotlin/com/openship/shared/
                └── platform/
                    ├── TokenStorage.kt        # Android Keystore / EncryptedSharedPreferences
                    └── PlatformEngine.kt      # OkHttp engine for Ktor
```

### Module Responsibilities

| Module | Role | Android-only? |
|---|---|---|
| `shared/commonMain` | All networking (MCP + SSE), models, repository, utils | No — pure Kotlin, runs on any KMP target |
| `shared/androidMain` | Android-specific: token storage (Keystore), Ktor engine (OkHttp) | Yes |
| `app` | Compose UI, Koin DI wiring, lifecycle, navigation, WorkManager | Yes |

**Why `shared/` + `app/` split**: The `shared` module holds all networking, models, and repository logic in `commonMain` — pure Kotlin, no Android dependencies. When you add iOS later, you create `iosMain` in `shared/` for Keychain storage and a Darwin engine. The existing `commonMain` code runs unchanged.

---

## 6. Architecture: MCP + SSE

```
┌─────────────────────────────────────────────────────────┐
│                     Android App                          │
│                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │  Connect    │  │  Projects   │  │  Deploy     │     │
│  │  Screen     │  │  Screen     │  │  Logs       │     │
│  │             │  │             │  │  Screen     │     │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘     │
│         │                │                │             │
│         └────────────────┼────────────────┘             │
│                          │                              │
│                   ┌──────▼──────┐                       │
│                   │  ViewModel  │  (Koin injected)      │
│                   └──────┬──────┘                       │
│                          │                              │
│          ┌───────────────┼───────────────┐              │
│          │               │               │              │
│   ┌──────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐      │
│   │  McpClient  │ │  SseClient  │ │ TokenStorage│      │
│   │  (discrete  │ │ (streams:   │ │ (Keystore)  │      │
│   │   ops)      │ │  logs, mon) │ │             │      │
│   └──────┬──────┘ └──────┬──────┘ └─────────────┘      │
│          │               │                              │
│          └───────┬───────┘                              │
│                  │                                      │
│          ┌───────▼───────┐                              │
│          │  Ktor         │  (ONE shared HttpClient)     │
│          │  HttpClient   │  SSE plugin installed        │
│          └───────┬───────┘                              │
│                  │                                      │
└──────────────────┼──────────────────────────────────────┘
                   │
                   ▼
          Openship API (self-hosted or cloud)
          http://localhost:4000  |  https://api.openship.io
```

### MCP vs SSE — When to Use Which

| Concern | MCP | SSE |
|---|---|---|
| **Discrete operations** (health check, list projects, trigger deploy) | ✅ `client.callTool(name, args)` | ❌ |
| **Real-time streams** (build logs, monitoring metrics) | ❌ | ✅ Ktor SSE plugin |
| **Auth** | PAT via `requestBuilder` header injection | PAT via `Authorization` header |
| **Transport** | Streamable-HTTP JSON-RPC at `/api/mcp` | HTTP GET with `text/event-stream` |
| **Reconnection** | Reconnect on foreground; re-handshake | `?since=<seq>` for deploy logs; live-only for monitor |
| **Shared resources** | Uses the same Ktor HttpClient | Uses the same Ktor HttpClient |

**Key principle**: One `HttpClient` instance shared between MCP transport and SSE streams. Install the SSE plugin once. This means fewer sockets, consistent auth, and consistent TLS/cleartext config.

---

## 7. API Contract — 4 Base Features

Base URL: `{apiOrigin}` (self-host: `http://localhost:4000`, cloud: `https://api.openship.io`).

All API routes are registered in `apps/api/src/app.ts:169-186` in the Openship repo.

### Feature 1: Health Check (Discovery / Login Probe)

Use this when the user enters an instance URL to validate connectivity and determine the auth mode.

#### `GET /api/health` — Quick liveness check
- **Auth**: None (public)
- **Response**: `200 OK`
```json
{
  "status": "ok",
  "cloudMode": false,
  "timestamp": "2026-08-18T13:22:37.965Z"
}
```

#### `GET /api/health/env` — Full environment discovery
- **Auth**: None (public, rate-limited)
- **Use this as the app's discovery/login probe.** The `authMode` field drives the login UI.
- **Response**: `200 OK`
```json
{
  "selfHosted": true,
  "deployMode": "desktop",       // desktop | docker | bare | cloud
  "isServerHost": true,
  "hostControlEnabled": true,
  "version": "0.6.6",
  "authMode": "none",            // none | local | cloud  ← drives login UI
  "productMode": "...",
  "teamMode": false,
  "migrationTargetUrl": null,
  "migrationInProgress": false,
  "cloudAuthUrl": "https://cloud.openship.io",
  "cloudApiUrl": "https://api.openship.io",
  "machineName": "dev-box",
  "hostDomain": "localhost"
}
```

**Login UI logic**:
- `authMode === "none"` → zero-auth (loopback desktop). PAT not required, but app should still support it for remote access.
- `authMode === "local"` → better-auth (email/OTP, session cookies). App uses PAT for API access.
- `authMode === "cloud"` → PKCE OAuth against Openship Cloud. App uses PAT for API access.

### Feature 2: List Projects

#### `GET /api/projects/home` — Primary endpoint (what the dashboard uses)
- **Auth**: PAT (Bearer token) or session cookie
- **Permission tag**: `project:list`
- **Response**: `200 OK`
```json
{
  "success": true,
  "projects": [ProjectRow, ...],
  "numbers": {
    "total_projects": 5,
    "total_active_projects": 3,
    "total_deployments": 42,
    "total_success_deployments": 38
  },
  "otherOrgs": [],
  "cloudPartial": false
}
```

#### `GET /api/projects` — Paginated alternative
- **Auth**: PAT or cookie
- **Permission tag**: `project:list`
- **Query params**: `?page=1&perPage=20`
- **Response**:
```json
{
  "data": [ProjectRow, ...],
  "total": 5,
  "page": 1,
  "perPage": 20
}
```

#### ProjectRow shape (key fields for the UI)

```kotlin
@Serializable
data class ProjectRow(
    val id: String,                    // "proj_..."
    val name: String,
    val slug: String,
    val environmentSlug: String,       // "production" | "preview"
    val isApp: Boolean,
    val appTemplateId: String? = null,
    val gitProvider: String? = null,
    val gitOwner: String? = null,
    val gitRepo: String? = null,
    val gitBranch: String? = null,
    val framework: String? = null,
    val packageManager: String? = null,
    val buildCommand: String? = null,
    val startCommand: String? = null,
    val activeDeploymentId: String? = null,
    val latestDeploymentId: String? = null,
    val latestDeploymentStatus: String? = null,   // see DeploymentStatus
    val activeDeploymentStatus: String? = null,   // see DeploymentStatus
    val activeVersion: String? = null,
    val awaitingDecision: Boolean = false,
    val serviceCount: Int = 0,
    val hasMultipleServices: Boolean = false,
    val favicon: String? = null,
    val deployTarget: String? = null,
    val serverId: String? = null,
    val serverName: String? = null,
    val runtimeMode: String? = null,
    val resources: ProjectResources? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ProjectResources(
    val production: ResourceLimits? = null,
    val build: ResourceLimits? = null,
    val sleepMode: String? = null,
    val port: Int? = null
)

@Serializable
data class ResourceLimits(
    val cpuCores: Double? = null,
    val memoryMb: Int? = null
)
```

#### Deployment status FSM

```
queued → building → deploying → ready
                              → failed
                              → cancelled
                              → reconciling (on resume)
```

Status values: `success` | `failed` | `building` | `queued` | `cancelled` | `reconciling`

#### Related endpoints (for future use)

- `GET /api/deployments?projectId=…&page=…&perPage=…&environment=production|preview` — deployment history (perPage max 100)
- `GET /api/deployments/:id` → `{data: presentDeployment}` — single deployment detail
- `GET /api/deployments/:id/logs?tail=N` → `{data: LogEntry[]}` — historical logs (non-streaming)

### Feature 3: Live Build/Deploy Logs — SSE

#### `GET /api/deployments/:id/stream` — Real-time build log stream
- **Auth**: PAT (Bearer token in `Authorization` header) or session cookie
- **Permission tag**: `deployment:read`
- **Not localOnly** → works for remote mobile clients (not restricted to loopback)
- **Headers**: `Accept: text/event-stream`, `Authorization: Bearer opsh_pat_...`
- **Query params**: `?since=<seq>` for replay/resume. Alternatively, `Last-Event-ID` header.

#### Replay/Resume Model

- Server maintains a per-session TTL cache keyed by deployment ID
- Each event has a monotonic `eventId` (= sequence number `seq`)
- On (re)connect with `?since=<seq>`, server replays all entries with `seq > sinceSeq`
- Replay order: logs → final progress → service-status → install-phase → prompt → terminal events (complete/cancelled/end) if finished
- If no session exists → `event: error, data: {"error":"Session not found"}`

#### Event Protocol

All `data:` payloads are JSON. SSE events use `event:` field for typing.

| Event type | `event:` field | Data shape | Notes |
|---|---|---|---|
| **log** | (default, no event field) or `log` | `{type:"log", data:"<base64>", eventId:<seq>, step, stepStatus, level, serviceName, serviceId}` | **`data` is base64-encoded log text** — decode before display |
| **progress** | `progress` | `{type:"progress", …step metadata}` | Build step progress |
| **service-status** | `service-status` | `{type:"service-status", serviceName, serviceId, status:"pending\|building\|built\|deploying\|running\|failed", error?, containerId?, hostPort?}` | Per-service status |
| **install-phase** | `install-phase` | `{type:"install-phase", …}` | Installation phase marker |
| **prompt** | `prompt` | `{type:"prompt", …}` | Awaits user decision (edge 80/443, port conflict). **Replayed on reconnect.** |
| **complete** | `complete` | `{type:"complete", status:"ready\|…", …portCheck results?}` | Build completed |
| **cancelled** | `cancelled` | `{type:"cancelled", message:"Build cancelled"}` | Build was cancelled |
| **end** | `end` | `{type:"end", status:<final>}` | **Terminal event** — server closes stream |
| **error** | `error` | `{error: "<msg>"}` | Error event |
| **ping** | (heartbeat) | `{}` | Keep-alive, ignore for display |

#### Kotlin model for deploy stream events

```kotlin
@Serializable
sealed class DeployStreamEvent {
    @Serializable
    data class Log(
        val data: String,          // base64-encoded — decode before display
        val eventId: Long,
        val step: String? = null,
        val stepStatus: String? = null,
        val level: String? = null,
        val serviceName: String? = null,
        val serviceId: String? = null
    ) : DeployStreamEvent()

    @Serializable
    data class Progress(
        val step: String? = null,
        val stepStatus: String? = null
    ) : DeployStreamEvent()

    @Serializable
    data class ServiceStatus(
        val serviceName: String,
        val serviceId: String,
        val status: String,        // pending|building|built|deploying|running|failed
        val error: String? = null,
        val containerId: String? = null,
        val hostPort: Int? = null
    ) : DeployStreamEvent()

    @Serializable
    data class Complete(
        val status: String,
        val portCheck: JsonElement? = null
    ) : DeployStreamEvent()

    @Serializable
    data class Cancelled(
        val message: String
    ) : DeployStreamEvent()

    @Serializable
    data class End(
        val status: String
    ) : DeployStreamEvent()

    @Serializable
    data class Error(
        val error: String
    ) : DeployStreamEvent()

    @Serializable
    object Ping : DeployStreamEvent()
}
```

#### How the dashboard consumes it (reference)

The Openship dashboard (`apps/dashboard/src/hooks/useSSEConnection.ts:402-403`) uses **plain `fetch` GET** with `Accept: text/event-stream` + `credentials: "include"` — NOT native `EventSource` (because EventSource can't set custom headers like `Authorization`). The SSE client is in `apps/dashboard/src/lib/sseClient.ts:79-98` (`connectToSSE`).

**For KMP**: Use Ktor's SSE plugin with the same approach — HTTP GET with `Accept: text/event-stream` and `Authorization: Bearer` header. This is the fetch-equivalent and works for PAT auth.

### Feature 4: Live Monitoring — SSE

#### `GET /api/system/monitor/stream?serverId=<serverId>` — Real-time server metrics
- **Auth**: PAT (Bearer token) or session cookie
- **Permission tag**: `server:read`
- **404s in CLOUD_MODE** — self-hosted only. App should handle this gracefully (hide monitor tab for cloud instances).
- **Missing `serverId`** → `400 Bad Request`
- **Headers**: `Accept: text/event-stream`, `Authorization: Bearer opsh_pat_...`

#### Event Protocol

| Event type | `event:` field | Data shape | Frequency |
|---|---|---|---|
| **stats** | `stats` | `{cpu, memTotal, memUsed, memAvail, diskTotal, diskUsed, diskAvail, uptime, load1, load5, load15}` | Every **3 seconds** (POLL_INTERVAL=3000) |
| **error** | `error` | `{error: "<msg>"}` | On failed sample (stream keeps going) |

**No replay/sequence** — this is live-only push. Stream ends on client abort.

**Memory and disk values are in kB (kilobytes).**

#### Kotlin model for monitor stats

```kotlin
@Serializable
data class MonitorStats(
    val cpu: Double,          // percentage, 0-100
    val memTotal: Long,       // kB
    val memUsed: Long,        // kB
    val memAvail: Long,       // kB
    val diskTotal: Long,      // kB
    val diskUsed: Long,       // kB
    val diskAvail: Long,      // kB
    val uptime: Long,         // seconds
    val load1: Double,        // 1-minute load average
    val load5: Double,        // 5-minute load average
    val load15: Double        // 15-minute load average
)
```

#### Server-level vs project-level

This stream is **server-level** (whole machine) stats, not per-project. Per-project CPU/memory/traffic lives in:
- `GET /api/analytics/resources` (REST, tag `analytics:read`)
- `GET /api/analytics/usage/history` (REST, tag `analytics:read`)

These are REST endpoints, not SSE — use MCP or direct HTTP for them in future phases.

#### How the dashboard consumes it (reference)

`apps/dashboard/src/hooks/useMonitorStream.ts:1-60` — fetch-based SSE with `AbortController` + incremental buffer parse, `credentials: include`, disabled when tab hidden, `reconnect()` helper.

---

## 8. Auth Model

### Personal Access Token (PAT)

PAT is the simplest universal auth for a third-party client. The Openship CLI and MCP endpoint both use it.

- **Format**: `opsh_pat_<43-char base64url secret>`
- **Header**: `Authorization: Bearer opsh_pat_...`
- **Parsed by**: `apps/api/src/lib/bearer.ts` (`parseBearerToken`, regex `/^bearer\s+(.+)$/i`)
- **Scoped PATs**: filter by grant (`resourceType:resourceId:permissions`)
- **Bound tokens**: pin `X-Organization-Id` header

### Auth Modes (`OPENSHIP_AUTH_MODE`)

| Mode | Description | App behavior |
|---|---|---|
| `none` | Zero-auth, loopback only (desktop) | PAT not required locally, but app supports it for remote |
| `local` | better-auth (email/OTP, session cookies, organizations) | App uses PAT for API access |
| `cloud` | PKCE OAuth against Openship Cloud | App uses PAT for API access |

### Token Storage on Android

- **EncryptedSharedPreferences** (AndroidX Security `1.1.0-alpha06`) for PAT storage
- Backed by Android Keystore — master key generated per-device, AES-256
- Store: instance URL, label, PAT (encrypted), authMode, serverId (for monitoring)
- No middleman server — PAT stays on device

### SSE + Token

**Important**: The API's SSE routes accept **only** the `Authorization` header or cookies — there is **no `?token=` query parameter support** in `apps/api/src/middleware/auth.ts`. Native `EventSource` (browser API) can't carry PAT headers. The dashboard works around this with fetch-based SSE + `credentials: include`.

**For KMP**: Use Ktor's SSE plugin (HTTP GET with headers) — this is the fetch equivalent and supports `Authorization: Bearer` headers natively. No workaround needed.

---

## 9. MCP SDK Integration

### Official Kotlin MCP SDK

- **Repo**: `modelcontextprotocol/kotlin-sdk`
- **Version**: 0.15.0 (Jul 28, 2026)
- **Maintained with JetBrains**, official, Apache-2.0
- **KMP targets**: JVM, Native (iOS, watchOS, tvOS, macOS), JS, Wasm. Android consumes JVM variant.
- **Stack**: Ktor client 3.5.2, kotlinx.serialization 1.11.0, Kotlin 2.4.x — identical to our stack
- **Artifact**: `io.modelcontextprotocol:kotlin-sdk-client` on Maven Central
- **Transports**: Streamable HTTP (client+server), stdio, legacy SSE, WebSocket, ChannelTransport

### Streamable-HTTP Client Pattern

The MCP endpoint is at `{apiOrigin}/api/mcp`. It's a Streamable-HTTP JSON-RPC endpoint with PAT auth and per-call permission re-checking. Openship has 186 MCP tools opted in via `mcp: { description }` on routes.

```kotlin
// 1. Create shared HttpClient (used for both MCP and SSE)
val httpClient = HttpClient {
    install(SSE)
    // OkHttp engine configured in androidMain
}

// 2. Create MCP Client
val mcpClient = Client(
    clientInfo = Implementation(
        name = "openship-android",
        version = "0.1.0"
    )
)

// 3. Create transport with PAT auth
val transport = StreamableHttpClientTransport(
    client = httpClient,
    url = "${instanceUrl}/api/mcp",
    requestBuilder = {
        header("Authorization", "Bearer $pat")
    }
)

// 4. Connect — performs initialize handshake + version negotiation + session capture
mcpClient.connect(transport)

// 5. List tools (paginated — cache locally)
val tools = mcpClient.listTools().tools  // 186 tools, paginated via nextCursor

// 6. Call a tool
val result = mcpClient.callTool(
    name = "projects/list",  // or whatever the Openship MCP tool name is
    arguments = mapOf("page" to 1, "perPage" to 20)
)
// result.content — raw content
// result.structuredContent — parsed JSON (if server provides)
// result.isError — boolean
```

### MCP Client Wrapper (`shared/commonMain/client/McpClient.kt`)

A thin wrapper (~100-200 LOC) that provides:
- Token injection via `requestBuilder`
- Tool catalog caching (call `listTools()` once, cache locally, refresh on `notifications/tools/list_changed`)
- Typed wrappers for ~10 tool families (health, projects, deployments, etc.)
- Reconnect on foreground (lifecycle handling)
- 404-session-expiry handling

### MCP Tools to Use for Base Features

> **Note**: Exact MCP tool names need to be confirmed by calling `listTools()` against a running Openship instance. The routes opt in via `mcp: { description }` in the route definition. The tool name is typically derived from the route path.

| Base Feature | MCP Tool (expected) | Fallback REST |
|---|---|---|
| Health check | `health` or `health/env` | `GET /api/health/env` |
| List projects | `projects/list` or `projects/home` | `GET /api/projects/home` |
| (Future) Deploy | `projects/deploy` | `POST /api/projects/:id/deploy` |
| (Future) Rollback | `deployments/rollback` | `POST /api/deployments/:id/rollback` |

**Important**: Credential/token routes **cannot** opt into MCP — they're excluded for security. Use direct REST for those (not needed in base version).

### Proven in Android (Google precedent)

**Google AI Edge Gallery** app uses exactly this pattern: `StreamableHttpClientTransport` with `requestBuilder` for auth headers, `client.connect()`, `client.listTools()`. Apache-2.0. Other consumers: AAswordman/Operit (Android), rikkahub, JetBrains/compose-hot-reload.

### Android Pitfalls (All Known, None Blocking)

1. **Cleartext policy** — self-hosted Openship is often plain `http://` on LAN. Android blocks cleartext by default. Fix: `networkSecurityConfig` with `usesCleartextTraffic="true"` for dev builds, or domain-specific cleartext permits for production.

2. **Token storage** — PAT in Android Keystore/EncryptedSharedPreferences. The SDK is stateless about credentials — inject via `requestBuilder`.

3. **Lifecycle** — Don't hold MCP Client across backgrounding. Reconnect on foreground. Session 404-handling is your responsibility.

4. **186 tools ≠ 186 round-trips** — `tools/list` is paginated. Cache the tool catalog + JSON schemas locally. Refresh on `notifications/tools/list_changed` notification.

5. **One HttpClient** — The SDK shares your Ktor client. Install SSE once. Use the same instance for MCP transport AND direct SSE streams. Fewer sockets, consistent auth.

---

## 10. SSE Implementation

### Ktor SSE Plugin

Ktor 3.5.2 has a first-party SSE plugin in `commonMain`. It handles:
- HTTP GET with `Accept: text/event-stream`
- Custom headers (including `Authorization: Bearer`)
- Incremental buffer parsing
- Auto-reconnection (configurable)
- Heartbeat/keep-alive handling

```kotlin
// Shared HttpClient with SSE plugin
val httpClient = HttpClient {
    install(SSE)
}

// Deploy log stream
val deployStream = httpClient.sse(
    urlString = "${instanceUrl}/api/deployments/${deploymentId}/stream?since=${lastSeq}",
    request = {
        header("Authorization", "Bearer $pat")
        header("Accept", "text/event-stream")
    }
) {
    incoming.collect { event ->
        when (event.event) {
            null, "log" -> {
                val logData = Json.decodeFromString<DeployStreamEvent.Log>(event.data)
                val decodedText = Base64.decode(logData.data)  // decode base64 log line
                // emit to UI
            }
            "service-status" -> { /* ... */ }
            "complete" -> { /* terminal — close stream */ }
            "end" -> { /* terminal — close stream */ }
            "error" -> { /* show error */ }
            else -> { /* ping, progress, etc. */ }
        }
    }
}

// Monitor stream
val monitorStream = httpClient.sse(
    urlString = "${instanceUrl}/api/system/monitor/stream?serverId=${serverId}",
    request = {
        header("Authorization", "Bearer $pat")
        header("Accept", "text/event-stream")
    }
) {
    incoming.collect { event ->
        when (event.event) {
            "stats" -> {
                val stats = Json.decodeFromString<MonitorStats>(event.data)
                // emit to UI — updates every 3s
            }
            "error" -> { /* non-fatal, stream continues */ }
        }
    }
}
```

### Sequence Tracking for Deploy Log Resume

```kotlin
class SeqTracker {
    private var lastSeq: Long = 0L

    fun update(seq: Long) {
        if (seq > lastSeq) lastSeq = seq
    }

    fun resumeParam(): String = lastSeq.toString()
}
```

On reconnect, pass `?since=${seqTracker.resumeParam()}` to get replay of missed events.

### Reconnection Strategy

| Stream | Reconnect? | Resume? | Strategy |
|---|---|---|---|
| Deploy logs | Yes | Yes (`?since=<seq>`) | Track last `eventId`, reconnect with `?since` |
| Monitor | Yes | No (live-only) | Simple reconnect, no replay |

### Mobile Lifecycle Considerations

- **Backgrounding kills sockets** on iOS (and aggressively on Android). When the app goes to background:
  - Cancel SSE streams (save `lastSeq` for deploy logs)
  - Disconnect MCP client
- **On foreground**:
  - Reconnect MCP client (re-handshake)
  - Reconnect SSE streams (deploy logs with `?since`, monitor fresh)
- **Push notifications** (future): When backgrounded, you can't receive SSE. Push notifications for "deploy finished/failed" require a separate mechanism (FCM). This is Phase 1.5 / Phase 2.

---

## 11. Android-Specific Concerns

### Cleartext Traffic (Self-Hosted LAN)

Self-hosted Openship often runs on plain `http://` on a LAN IP. Android blocks cleartext by default.

**Fix**: `res/xml/network_security_config.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Dev: allow all cleartext (for local Openship instances) -->
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

Reference in `AndroidManifest.xml`:
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ... >
```

> **Production consideration**: For production, use domain-specific cleartext permits instead of a blanket `true`. Only allow cleartext for user-added instance URLs that are RFC1918 (private LAN) addresses.

### Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<!-- Future: push notifications -->
<!-- <uses-permission android:name="android.permission.POST_NOTIFICATIONS" /> -->
```

### Token Storage (EncryptedSharedPreferences)

```kotlin
// shared/androidMain/platform/TokenStorage.kt
class AndroidTokenStorage(context: Context) : TokenStorage {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "openship_instances",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveInstance(config: InstanceConfig) {
        prefs.edit {
            putString(config.id, Json.encodeToString(config))
        }
    }

    fun loadInstances(): List<InstanceConfig> {
        return prefs.all.values.mapNotNull { json ->
            Json.decodeFromString<InstanceConfig>(json as String)
        }
    }

    fun deleteInstance(id: String) {
        prefs.edit { remove(id) }
    }
}
```

### Ktor Engine (OkHttp on Android)

```kotlin
// shared/androidMain/platform/PlatformEngine.kt
fun createAndroidEngine(): HttpClientEngineFactory<*> = OkHttp
```

In `commonMain`, use `expect`/`actual` or Koin to inject the engine.

---

## 12. Build & Development Setup

### Prerequisites

- **JDK 21+** (Kotlin 2.4.10 requires JDK 21 for Gradle)
- **Android Studio** (latest stable, with Kotlin Multiplatform plugin)
- **Android SDK** (compileSdk 35, minSdk 26)
- **An Openship instance** for testing:
  - Self-hosted: `git clone https://github.com/oblien/openship && cd openship && bun install && bun dev`
  - Or use Openship Cloud: `https://api.openship.io`

### Getting a PAT from Openship

1. Start Openship (`bun dev` in the openship repo)
2. Open the dashboard at `http://localhost:3001`
3. Go to Settings → API Tokens → Create Token
4. Copy the `opsh_pat_...` token

Or via CLI:
```bash
opsh login --token <existing-token> --api-url http://localhost:4000
opsh tokens create --name "android-dev" --permissions "project:list,deployment:read,server:read"
```

### Creating the Project

```bash
# From the parent directory of openship/
mkdir openship-android
cd openship-android

# Initialize Gradle project
gradle init --type kotlin --project-name openship-android

# Or use the KMP wizard: https://kmp.jetbrains.com/
# Select: Android only, Compose Multiplatform, Ktor, kotlinx.serialization
```

### Version Catalog (`gradle/libs.versions.toml`)

```toml
[versions]
kotlin = "2.4.10"
compose-multiplatform = "1.11.1"
ktor = "3.5.2"
kotlinx-serialization = "1.11.0"
mcp-sdk = "0.15.0"
koin = "4.0.0"
androidx-security = "1.1.0-alpha06"
androidx-lifecycle = "2.8.7"
navigation-compose = "2.8.5"
okhttp = "4.12.0"
agp = "8.7.3"

[libraries]
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-sse = { module = "io.ktor:ktor-client-plugins-sse", version.ref = "ktor" }
ktor-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
mcp-sdk-client = { module = "io.modelcontextprotocol:kotlin-sdk-client", version.ref = "mcp-sdk" }
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-androidx-compose", version.ref = "koin" }
androidx-security-crypto = { module = "androidx.security:security-crypto", version.ref = "androidx-security" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "androidx-lifecycle" }
navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation-compose" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
```

### Build & Run

```bash
# Build debug APK
./gradlew :app:assembleDebug

# Install on connected device/emulator
./gradlew :app:installDebug

# Run unit tests (shared module)
./gradlew :shared:testDebugUnitTest

# Run instrumented tests
./gradlew :app:connectedAndroidTest
```

### Testing Against a Local Openship Instance

1. Start Openship: `cd ../openship && bun dev`
2. Verify API is up: `curl http://localhost:4000/api/health` → `{"status":"ok",...}`
3. Get a PAT (see above)
4. In the Android app:
   - Add instance: URL = `http://10.0.2.2:4000` (Android emulator maps host localhost to 10.0.2.2)
   - Enter PAT
   - The app should discover the instance via `GET /api/health/env`
5. For a physical device on the same LAN: use your machine's LAN IP (e.g., `http://192.168.1.100:4000`)

---

## 13. Roadmap

### Phase 1 — Base Version (v0.1.0) — Read-Only

| Feature | Status | API |
|---|---|---|
| Connect (add instance by URL + PAT) | Planned | `GET /api/health/env` (discovery) |
| List projects | Planned | MCP `projects/list` or `GET /api/projects/home` |
| Live build/deploy logs | Planned | SSE `GET /api/deployments/:id/stream` |
| Live monitoring | Planned | SSE `GET /api/system/monitor/stream` |

### Phase 1.5 — Polish

| Feature | Description |
|---|---|
| Push notifications | Deploy finished/failed, service down (FCM) |
| Multi-instance | Switch between multiple Openship instances |
| Dark/light theme | Material 3 dynamic color |
| Widget | Home screen widget for monitoring stats |

### Phase 2 — Actions

| Feature | API |
|---|---|
| Deploy / rollback / redeploy | MCP `projects/deploy`, `deployments/rollback` |
| Service control (start/stop/restart) | MCP `services/control` |
| Environment variables (view/edit) | MCP `projects/env` |
| Domains & SSL (view) | MCP `domains/list` |

### Phase 3 — Power User

| Feature | API |
|---|---|
| Live terminal (WebSocket PTY) | `POST /api/terminal/ticket` → `WS /api/terminal/ws/:serverId` |
| Push-to-deploy management | MCP `webhooks/config` |
| Backups (trigger/restore) | MCP `backups/trigger` + SSE `backup.sse.ts` |
| Per-project analytics | `GET /api/analytics/resources`, `GET /api/analytics/usage/history` |

### Phase 4 — Expansion

| Feature | Description |
|---|---|
| iOS target | Add `iosMain` to `shared/`, Compose Multiplatform iOS |
| Desktop target | JVM desktop via Compose Multiplatform |
| MCP tool explorer | Browse all 186 MCP tools, call any tool from the app |
| AI chat | Natural language → MCP tool calls (on-device or cloud LLM) |

---

## 14. Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| **MCP SDK 0.x churn** | Breaking API changes land monthly | Pin to 0.15.0. Revisit per release. Wrapper isolates SDK API. |
| **Openship API instability** | No official API versioning — additive change only, but breaking changes possible | Pin client to a specific Openship release. Version the client SDK. Tolerate unknown fields (kotlinx.serialization with `ignoreUnknownKeys = true`). |
| **Coolify precedent** | Coolify v4.2 broke all third-party clients in one release | Monitor Openship releases. Test against new versions before updating. |
| **Maintenance burden** | Solo dev, one app store, one platform (Android) | Keep scope small. Read-only first. Don't chase feature parity with the web dashboard. |
| **MCP server contract unknowns** | Whether `/api/mcp` is sessionful or stateless; 404-session-expiry behavior | Test early. SDK handles both modes. Implement reconnect logic. |
| **Cleartext on LAN** | Android blocks cleartext by default | `networkSecurityConfig` with cleartext permit. Consider domain-specific permits for production. |
| **Backgrounding kills SSE** | User misses deploy completion while app is backgrounded | Push notifications (Phase 1.5). Save `lastSeq` for deploy log resume on foreground. |
| **186 tools pagination** | `tools/list` is paginated — not all tools in one response | Cache tool catalog locally. Handle `nextCursor` pagination. Refresh on `notifications/tools/list_changed`. |

---

## 15. References

### Openship

- **Repo**: https://github.com/oblien/openship
- **Local path**: `/home/kareemessam_me/Desktop/Oblian/openship`
- **API routes**: `apps/api/src/app.ts:169-186`
- **Health routes**: `apps/api/src/modules/health/health.routes.ts`
- **Auth middleware**: `apps/api/src/middleware/auth.ts`
- **Bearer token parsing**: `apps/api/src/lib/bearer.ts`
- **MCP module**: `apps/api/src/modules/mcp/`
- **SSE client (dashboard)**: `apps/dashboard/src/lib/sseClient.ts:79-98`
- **Deploy SSE hook**: `apps/dashboard/src/hooks/useSSEConnection.ts:402-403`
- **Monitor SSE hook**: `apps/dashboard/src/hooks/useMonitorStream.ts:1-60`
- **API client (dashboard)**: `apps/dashboard/src/lib/api/client.ts` + `endpoints.ts`
- **Shared types**: `packages/core/` (portable to Kotlin)
- **Dev commands**: `bun dev` (API + dashboard), `bun run test`, `bun clean`

### Kotlin MCP SDK

- **Repo**: https://github.com/modelcontextprotocol/kotlin-sdk
- **Version**: 0.15.0 (Jul 28, 2026)
- **Artifact**: `io.modelcontextprotocol:kotlin-sdk-client` (Maven Central)
- **KMP targets**: JVM, Native (iOS/watchOS/tvOS/macOS), JS, Wasm
- **Stack**: Ktor 3.5.2, kotlinx.serialization 1.11.0, Kotlin 2.4.x
- **Transports**: Streamable HTTP, stdio, legacy SSE, WebSocket, ChannelTransport
- **Maturity**: Official conformance suite, integration tests vs TypeScript SDK, binary-compatibility validator, Detekt/Kover CI

### KMP / Compose Multiplatform

- **Compose Multiplatform**: 1.11.1 (Jun 2026), 1.12.0-rc01 (Aug 2026)
- **Kotlin**: 2.4.10
- **Ktor Client**: 3.5.2 (Jul 2026) — HTTP + SSE + WebSocket in `commonMain`
- **kotlinx.serialization**: 1.11.0
- **KMP wizard**: https://kmp.jetbrains.com/

### Precedent (Android apps using MCP SDK)

- **Google AI Edge Gallery** — `StreamableHttpClientTransport` + `requestBuilder` auth, Apache-2.0
- **AAswordman/Operit** — Android MCP client
- **rikkahub** — migrated from hand-rolled to official SDK

### Deployment Platform Mobile Client Landscape

| Project | Official app? | Community clients |
|---|---|---|
| Coolify | No | 5+ unofficial, all small |
| Portainer | No | Portarius (230★, archived), Yomo (third-party) |
| Gitea/Forgejo | No | igitea, Anvil (unofficial) |
| CapRover | No | CapRover Mobile (dead since 2020) |
| Dokku | No | none |
| **Railway** | **Yes** (official, June 2026) | — built by Railway themselves |

**Key insight**: No community mobile client in this space has ever become official. Plan for a respected unofficial client. "Official" is a bonus, not a plan.

---

## Quick Start Checklist

- [ ] Create project structure (see Section 5)
- [ ] Set up `gradle/libs.versions.toml` (see Section 12)
- [ ] Configure `shared/build.gradle.kts` with KMP targets (Android only)
- [ ] Configure `app/build.gradle.kts` with Compose, Android manifest, network security config
- [ ] Implement `HttpClientFactory` in `shared/commonMain` (Ktor + SSE plugin)
- [ ] Implement `TokenStorage` in `shared/androidMain` (EncryptedSharedPreferences)
- [ ] Implement `McpClient` wrapper in `shared/commonMain` (connect, listTools, callTool)
- [ ] Implement `SseClient` in `shared/commonMain` (deploy logs + monitor streams)
- [ ] Implement `OpenshipRepository` interface + impl in `shared/commonMain`
- [ ] Create data classes in `shared/commonMain/model/` (HealthEnv, ProjectRow, SSE events)
- [ ] Set up Koin DI in `app/di/`
- [ | Build Connect screen (URL + PAT input, health/env discovery)
- [ ] Build Projects screen (MCP listTools + callTool for projects)
- [ ] Build Deploy Logs screen (SSE stream with base64 decode + seq tracking)
- [ ] Build Monitor screen (SSE stream with 3s stats updates)
- [ ] Test against local Openship instance (`bun dev` → `http://10.0.2.2:4000`)
- [ ] Open GitHub Discussion on oblien/openship with a working demo

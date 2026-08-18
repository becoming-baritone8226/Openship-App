# Contributing to Openship-App

Thank you for your interest in contributing to Openship-App! This is an unofficial community client for [Openship](https://github.com/oblien/openship), built with the founder's blessing.

## Project Status

We're currently in the pre-implementation phase for the base version (v0.1.0) — a read-only Android client with four features: Connect, List Projects, Live Build Logs, and Live Monitoring.

## Getting Started

### Prerequisites

- **JDK 21+** (project uses Zulu 21 toolchain, auto-provisioned by Gradle)
- **Android Studio** (latest stable, with Kotlin Multiplatform plugin)
- **Android SDK** (compileSdk 36, minSdk 24)
- **An Openship instance** for testing:
  ```bash
  git clone https://github.com/oblien/openship
  cd openship && bun install && bun dev
  # API: http://localhost:4000, Dashboard: http://localhost:3001
  ```

### Setup

```bash
git clone <this-repo>
cd Openship-App
./gradlew :androidApp:assembleDebug   # verify build works
```

### Getting a PAT for Testing

1. Start Openship (`bun dev`)
2. Open `http://localhost:3001` → Settings → API Tokens → Create Token
3. Copy the `opsh_pat_...` token
4. In the Android app, connect to `http://10.0.2.2:4000` (emulator) or your LAN IP (physical device)

## Architecture

Read [AGENTS.md](AGENTS.md) for the full architecture guide. Summary:

- **`shared/`** — KMP module. `commonMain` has all networking (MCP + SSE), models, repository. `androidMain` has platform-specific code (Keystore, OkHttp).
- **`androidApp/`** — Android app. Compose UI, ViewModels, Koin DI, navigation.
- **MCP** for discrete operations (health, list projects). **SSE** for real-time streams (logs, monitoring).
- **One shared Ktor HttpClient** for both layers.

## Development Workflow

### 1. Pick or Create a Task

We use [Beads](https://github.com/kareemessamessam/bd) for task tracking:

```bash
bd ready                    # see tasks ready to work on
bd create "description" -t task -p 2   # create a new task
bd update <id> --claim      # claim a task
```

### 2. Implement

Follow the coding conventions in [AGENTS.md](AGENTS.md):

- All networking code in `shared/commonMain/` — no Android imports
- `@Serializable` data classes with `Json { ignoreUnknownKeys = true }`
- `sealed class` for SSE events and UI state
- State hoisting in Compose (composables receive state + callbacks)
- Add dependencies to `gradle/libs.versions.toml` first

### 3. Test

```bash
./gradlew :shared:allTests             # unit tests
./gradlew :androidApp:testDebugUnitTest # Android unit tests
./gradlew :androidApp:connectedAndroidTest # instrumented tests (device)
```

### 4. Verify Build

```bash
./gradlew :androidApp:assembleDebug     # must pass
./gradlew :androidApp:check             # lint + tests
```

### 5. Close Task

```bash
bd close <id>
```

## Coding Standards

- **Kotlin official code style** (enforced via `.editorconfig`)
- 4-space indent, 120 char max line length
- No wildcard imports
- `data class` for models, `sealed class` for discriminated unions
- `Result<T>` or sealed class for error handling — no exceptions for expected failures
- All API models must use `@Serializable` with `ignoreUnknownKeys = true`

## Pull Request Guidelines

1. **One change per PR** — keep PRs focused
2. **Conventional commits** — `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`
3. **Test your changes** — at minimum, the build must pass
4. **Reference the task** — include `Closes #<id>` or the Beads task ID
5. **Describe the why** — explain the reasoning, not just the what

## Project Structure

```
shared/src/commonMain/kotlin/com/kareemessam/openship/shared/
├── client/          # HttpClientFactory, McpClient, SseClient
├── model/           # Data classes (HealthEnv, ProjectRow, SSE events)
├── repository/      # OpenshipRepository interface + impl
└── util/            # Base64Decoder, SeqTracker

androidApp/src/main/kotlin/com/kareemessam/openship/
├── di/              # Koin modules
├── ui/              # Compose screens + navigation + theme
└── work/            # WorkManager (future: push notifications)
```

## Questions?

- **Architecture/API**: Read [README.md](README.md) for the full API contract
- **Agent instructions**: Read [AGENTS.md](AGENTS.md)
- **Openship server**: Source at `../openship/` or https://github.com/oblien/openship

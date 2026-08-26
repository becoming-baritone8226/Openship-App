# Contributing

Thanks for helping with this **unofficial** [Openship](https://github.com/oblien/openship) Android client.

## Status

- **v0.1** — Connect, projects, live deploy logs, server monitor (done)
- **v0.2** — MCP client, deployment history, redeploy, rollback (done)
- Next work is tracked in-repo (Beads if you use it) and GitHub Issues after publish

## Prerequisites

- JDK 21+
- Android Studio (recent stable) + Android SDK (compileSdk 36, minSdk 24)
- A local or remote Openship instance for manual testing:

```bash
git clone https://github.com/oblien/openship
cd openship && bun install && bun dev
# API: http://localhost:4000
```

## Setup

```bash
git clone <this-repo>
cd Openship-App
./gradlew :androidApp:assembleDebug
```

### PAT for testing

1. Openship dashboard → **Settings → API Tokens → Create**
2. Emulator URL: `http://10.0.2.2:4000`
3. Physical device: LAN IP or USB with `adb reverse` (install tasks reverse `4000` / `20000`)

Never commit real PATs, `local.properties`, or keystores.

## Layout

| Module | Role |
|---|---|
| `shared/commonMain` | REST, SSE, MCP, models, ViewModels, Compose UI |
| `shared/androidMain` | OkHttp engine, encrypted token storage |
| `androidApp` | Application entry, manifest, resources |

Details: [AGENTS.md](AGENTS.md), [CONTEXT.md](CONTEXT.md).

## Workflow

1. One focused change per PR
2. Conventional commits: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`
3. Keep networking out of Android-only packages when it belongs in `commonMain`
4. Prefer `ignoreUnknownKeys = true` on API JSON
5. Confirm every write action in UI; degrade cleanly if MCP tools are missing

### Checks

```bash
./gradlew :androidApp:assembleDebug
./gradlew :shared:allTests
./gradlew :androidApp:testDebugUnitTest
```

## Coding bar

- Official Kotlin style (`.editorconfig`)
- No wildcard imports
- `@Serializable` models; sealed types for UI/SSE state
- Dependencies only via `gradle/libs.versions.toml`

## Scope guardrails

Please open an issue before large features. Especially:

- Blind env-var replace / secret display
- Local folder deploy packaging
- Generic “call any MCP tool” explorers without safety UX

## License

By contributing, you agree your contributions are licensed under the [Apache License 2.0](LICENSE).

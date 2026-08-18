# Openship-App — Study Guide

> Topics to understand before building. Organized by priority — start from the top.
> Each topic has: **what to study**, **why it matters for this project**, and **resources**.

---

## Priority Legend

- 🔴 **Critical** — you cannot build the base version without this
- 🟡 **Important** — needed for a quality implementation, not a hard blocker
- 🟢 **Nice to have** — deepens understanding, can learn while building

---

## 1. Kotlin Multiplatform (KMP) 🔴

### What to study
- KMP project structure: `commonMain`, `androidMain`, `iosMain` source sets
- `expect`/`actual` declarations — declaring platform-specific APIs in commonMain, implementing in platform source sets
- How KMP differs from Kotlin/JVM (no reflection, no JVM stdlib in commonMain)
- Dependency resolution in `commonMain` vs platform source sets
- Gradle KMP plugin configuration (`kotlin { android { ... } }` block)

### Why it matters
Your entire networking layer (MCP client, SSE client, HTTP client, models, repository) lives in `shared/commonMain`. Platform-specific code (Keystore token storage, OkHttp engine) goes in `shared/androidMain` via `expect`/`actual` or Koin injection. When you add iOS later, you only write `iosMain` — the commonMain code is untouched.

### Key concepts to master
- **Source set hierarchy**: `commonMain` → `androidMain` → `androidHostTest` / `androidDeviceTest`
- **`expect`/`actual`**: 
  ```kotlin
  // commonMain
  expect class TokenStorage() { fun save(token: String); fun load(): String? }
  // androidMain
  actual class TokenStorage actual constructor() { /* EncryptedSharedPreferences impl */ }
  ```
- **Multiplatform dependencies**: use `commonMain.dependencies { implementation(...) }` — the dependency must be KMP-compatible (Ktor, kotlinx.serialization, Koin all are)

### Resources
- [KMP documentation](https://kotlinlang.org/docs/multiplatform.html)
- [KMP tutorial — create your first app](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [expect/actual declarations](https://kotlinlang.org/docs/multiplatform-expect-actual.html)
- [KMP project structure](https://kotlinlang.org/docs/multiplatform-discover-project.html)

---

## 2. Compose Multiplatform (CMP) 🔴

### What to study
- Compose basics: `@Composable`, `remember`, `mutableStateOf`, `State<T>`
- State hoisting pattern — stateless composables receive state + callbacks
- `Modifier` chain ordering (order matters!)
- Side effects: `LaunchedEffect`, `DisposableEffect`, `rememberCoroutineScope`
- Material 3 components: `Scaffold`, `TopAppBar`, `NavigationBar`, `Card`, `LazyColumn`
- Navigation with Navigation Compose (`NavHost`, `composable()`, route arguments)
- Single-activity architecture
- Lifecycle integration: `LocalLifecycleOwner`, `LifecycleResumeEffect`

### Why it matters
Every screen in the app is a Compose composable. You need state hoisting to keep composables testable and reusable. Navigation Compose routes between Connect → Projects → Deploy Logs → Monitor screens. Side effects handle SSE stream lifecycle (start on screen enter, cancel on screen exit).

### Key concepts to master
- **State hoisting**:
  ```kotlin
  // ✅ Good — stateless, testable
  @Composable
  fun ConnectScreen(state: ConnectState, onUrlChange: (String) -> Unit, onConnect: () -> Unit)
  
  // ❌ Bad — owns state, hard to test
  @Composable
  fun ConnectScreen() { var url by remember { mutableStateOf("") } }
  ```
- **`LaunchedEffect` for SSE streams**:
  ```kotlin
  LaunchedEffect(deploymentId) {
      // Start SSE stream when screen enters composition
      // Automatically cancelled when screen leaves composition
      sseClient.streamDeployLogs(deploymentId).collect { event -> ... }
  }
  ```
- **`LazyColumn` for project list**:
  ```kotlin
  LazyColumn { items(projects) { project -> ProjectCard(project, onClick = ...) } }
  ```

### Resources
- [Compose Multiplatform documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform.html)
- [Jetpack Compose basics](https://developer.android.com/jetpack/compose/documentation#basics)
- [State hoisting](https://developer.android.com/jetpack/compose/state#state-hoisting)
- [Side effects in Compose](https://developer.android.com/jetpack/compose/side-effects)
- [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- [Material 3 components](https://developer.android.com/jetpack/androidx/releases/material3)

---

## 3. Ktor Client 3.5.2 🔴

### What to study
- `HttpClient` creation and configuration
- Engine selection — OkHttp for Android, Darwin for iOS (future)
- Plugin system: `install(ContentNegotiation)`, `install(SSE)`, `install(Auth)`
- **SSE plugin** — `httpClient.sse() { }` block, `incoming.collect { event -> }`, `ServerSseEvent` shape (`event`, `data`, `id`, `retry`)
- Content negotiation with kotlinx.serialization
- Request configuration: headers, query parameters, auth
- Coroutines integration — all Ktor calls are suspend functions
- Connection pooling and socket lifecycle

### Why it matters
Ktor is your entire networking layer. One `HttpClient` instance is shared between MCP transport and SSE streams. The SSE plugin handles build log streaming and monitoring. You need to configure headers (Authorization: Bearer PAT) on SSE requests, which native EventSource can't do.

### Key concepts to master
- **HttpClient with SSE + JSON**:
  ```kotlin
  val httpClient = HttpClient {
      install(SSE)
      install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
      engine { /* OkHttp config on Android */ }
  }
  ```
- **SSE request with auth header**:
  ```kotlin
  httpClient.sse(
      urlString = "$baseUrl/api/deployments/$id/stream?since=$lastSeq",
      request = { header("Authorization", "Bearer $pat") }
  ) {
      incoming.collect { event ->
          // event.event = "log" | "service-status" | "complete" | ...
          // event.data = JSON string
          // event.id = sequence number
      }
  }
  ```
- **Engine per-platform**:
  ```kotlin
  // androidMain
  actual fun createEngine(): HttpClientEngineFactory<*> = OkHttp
  // iosMain (future)
  actual fun createEngine(): HttpClientEngineFactory<*> = Darwin
  ```

### Resources
- [Ktor Client documentation](https://ktor.io/docs/client.html)
- [Ktor SSE plugin](https://ktor.io/docs/client-sse.html)
- [Ktor ContentNegotiation](https://ktor.io/docs/client-serialization.html)
- [Ktor engines](https://ktor.io/docs/http-client-engines.html)
- [Ktor HttpClient configuration](https://ktor.io/docs/http-client-configuration.html)

---

## 4. kotlinx.serialization 🔴

### What to study
- `@Serializable` annotation on data classes
- `@SerialName` for mapping JSON field names to Kotlin property names
- Nullable fields with defaults (`val foo: String? = null`)
- `Json` configuration: `ignoreUnknownKeys`, `isLenient`, `encodeDefaults`
- Sealed class serialization with `@SerialName` for discriminators
- Polymorphic serialization (if needed for SSE event types)
- `Json.decodeFromString<T>()` and `Json.encodeToString()`

### Why it matters
Every API response and SSE event is JSON. The Openship API has no versioning — fields can be added at any time. `ignoreUnknownKeys = true` is mandatory. SSE events are typed via the `event:` field, so you'll use sealed classes with `@SerialName` discriminators or manual parsing based on the event type.

### Key concepts to master
- **Tolerant JSON config** (critical for this project):
  ```kotlin
  val json = Json {
      ignoreUnknownKeys = true   // API has no versioning — new fields will appear
      isLenient = true
      encodeDefaults = true
  }
  ```
- **Sealed class for SSE events**:
  ```kotlin
  @Serializable
  sealed class DeployStreamEvent {
      @Serializable @SerialName("log") data class Log(val data: String, val eventId: Long) : DeployStreamEvent()
      @Serializable @SerialName("complete") data class Complete(val status: String) : DeployStreamEvent()
      // ...
  }
  ```
- **Nullable with defaults** (API fields may be absent):
  ```kotlin
  @Serializable data class ProjectRow(val id: String, val name: String, val gitRepo: String? = null)
  ```

### Resources
- [kotlinx.serialization documentation](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serialization-guide.md)
- [JSON serialization](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/basic-serialization.md)
- [Sealed class serialization](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#sealed-classes)
- [Json configuration](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md)

---

## 5. Model Context Protocol (MCP) 🔴

### What to study
- **MCP protocol basics**: what it is, why it exists, client-server model
- **JSON-RPC 2.0** — the transport protocol MCP uses (request, response, notification)
- **MCP lifecycle**: `initialize` → `initialized` → operations → `shutdown`
- **Tools**: `tools/list` (paginated), `tools/call` (with arguments), `notifications/tools/list_changed`
- **Streamable-HTTP transport** — how it differs from stdio, session management, SSE responses
- **Kotlin MCP SDK**: `Client`, `StreamableHttpClientTransport`, `requestBuilder` for auth
- **Tool arguments and results**: `CallToolResult.content`, `.structuredContent`, `.isError`

### Why it matters
MCP is your primary API layer. Instead of hand-rolling 186 REST routes, you call MCP tools via one endpoint (`/api/mcp`). The Kotlin SDK handles the JSON-RPC protocol, session negotiation, and tool discovery. You inject the PAT via `requestBuilder`. This is the AI-native angle that makes your client stand out.

### Key concepts to master
- **Client connection pattern**:
  ```kotlin
  val mcpClient = Client(clientInfo = Implementation("openship-android", "0.1.0"))
  val transport = StreamableHttpClientTransport(
      client = httpClient,
      url = "$baseUrl/api/mcp",
      requestBuilder = { header("Authorization", "Bearer $pat") }
  )
  mcpClient.connect(transport)  // initialize handshake + session
  ```
- **Tool discovery and caching**:
  ```kotlin
  val tools = mcpClient.listTools().tools  // paginated via nextCursor
  // Cache locally. Refresh on notifications/tools/list_changed.
  ```
- **Tool invocation**:
  ```kotlin
  val result = mcpClient.callTool("projects/list", mapOf("page" to 1))
  // result.isError → handle error
  // result.structuredContent → parse as JSON
  ```

### Resources
- [MCP specification](https://modelcontextprotocol.io/specification)
- [MCP architecture](https://modelcontextprotocol.io/docs/learn/architecture)
- [Kotlin MCP SDK](https://github.com/modelcontextprotocol/kotlin-sdk)
- [Streamable HTTP transport](https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#streamable-http)
- [MCP Tools](https://modelcontextprotocol.io/specification/2025-06-18/server/tools)
- [JSON-RPC 2.0 spec](https://www.jsonrpc.org/specification)

---

## 6. Server-Sent Events (SSE) Protocol 🟡

### What to study
- SSE wire format: `event: <type>\ndata: <payload>\n\n`
- Multiple `data:` lines concatenate with `\n`
- `id:` field for event IDs (used for reconnection via `Last-Event-ID`)
- `retry:` field for reconnection delay
- How SSE differs from WebSocket (one-way, HTTP-based, auto-reconnect in browser)
- Reading SSE manually (incremental buffer parsing) — useful for understanding what the Ktor plugin does
- Reconnection strategies: `Last-Event-ID` header vs `?since=<seq>` query param

### Why it matters
Two of your four features (build logs, monitoring) are SSE streams. The Openship deploy log stream supports replay via `?since=<seq>` — you track the last `eventId` and pass it on reconnect to get missed events. The monitoring stream is live-only (no replay). Understanding the wire format helps you debug parsing issues.

### Key concepts to master
- **SSE event structure**:
  ```
  event: service-status
  data: {"type":"service-status","serviceName":"web","status":"building"}

  ```
- **Sequence tracking for resume**:
  ```kotlin
  class SeqTracker {
      private var lastSeq = 0L
      fun update(seq: Long) { if (seq > lastSeq) lastSeq = seq }
      fun resumeParam() = lastSeq.toString()
  }
  // Reconnect: GET /api/deployments/:id/stream?since=${tracker.resumeParam()}
  ```
- **Base64 decoding** — SSE `log` events have base64-encoded `data` field:
  ```kotlin
  val decodedText = Base64.decode(logEvent.data).decodeToString()
  ```

### Resources
- [SSE specification (HTML5)](https://html.spec.whatwg.org/multipage/server-sent-events.html)
- [MDN: Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)
- [SSE reconnection logic](https://html.spec.whatwg.org/multipage/server-sent-events.html#the-eventsource-interface)

---

## 7. Android Security: Keystore & EncryptedSharedPreferences 🟡

### What to study
- Android Keystore system — what it is, how it stores cryptographic keys
- `MasterKey` — generating a master key in the Keystore
- `EncryptedSharedPreferences` — AES-256 encrypted SharedPreferences
- Key alias management and key rotation
- Security best practices: never log tokens, clear on uninstall, don't share between apps

### Why it matters
You store the user's PAT (Personal Access Token) on the device. It must be encrypted at rest. `EncryptedSharedPreferences` is backed by the Android Keystore — the master key never leaves the secure enclave. This is the standard approach for storing secrets on Android.

### Key concepts to master
- **EncryptedSharedPreferences setup**:
  ```kotlin
  val masterKey = MasterKey.Builder(context)
      .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
      .build()
  val prefs = EncryptedSharedPreferences.create(
      context, "openship_instances", masterKey,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
  )
  ```
- **Storing instance configs**: serialize `InstanceConfig` to JSON, store as string value

### Resources
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore)
- [EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)
- [AndroidX Security crypto guide](https://developer.android.com/privacy-and-security/data/securely-store-sensitive-data)

---

## 8. Android Networking: Cleartext & Network Security Config 🟡

### What to study
- Android's default cleartext traffic ban (since API 28)
- `network_security_config.xml` — domain-specific and base-config rules
- `usesCleartextTraffic` manifest attribute
- When cleartext is legitimate (LAN development, self-hosted services on private IPs)
- RFC 1918 private IP ranges (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)

### Why it matters
Self-hosted Openship often runs on plain `http://` on a LAN IP. Android blocks cleartext by default. You need a `network_security_config.xml` that permits cleartext (at least for dev, ideally domain-specific for production). The Android emulator maps host localhost to `10.0.2.2`.

### Key concepts to master
- **network_security_config.xml**:
  ```xml
  <network-security-config>
      <base-config cleartextTrafficPermitted="true">
          <trust-anchors>
              <certificates src="system" />
              <certificates src="user" />
          </trust-anchors>
      </base-config>
  </network-security-config>
  ```
- **Manifest reference**:
  ```xml
  <application android:networkSecurityConfig="@xml/network_security_config" ...>
  ```
- **Emulator localhost**: `http://10.0.2.2:4000` maps to host machine's `localhost:4000`

### Resources
- [Network security configuration](https://developer.android.com/privacy-and-security/security-config)
- [Android cleartext traffic](https://developer.android.com/privacy-and-security/security-config#CleartextTrafficPermitted)

---

## 9. Koin Dependency Injection 🟡

### What to study
- Koin module DSL: `module { single { } factory { } }`
- KMP-compatible DI — Koin works in `commonMain`
- Scoping: `single` (app lifetime) vs `factory` (new instance each time) vs `scoped` (session)
- ViewModel injection: `koinViewModel()` in Compose
- Android integration: `startKoin()` in Application class
- Module organization: separate modules for networking, repository, viewmodels

### Why it matters
Koin wires your dependencies: HttpClient → McpClient + SseClient → OpenshipRepository → ViewModels → Compose screens. It's pure Kotlin, works in commonMain, and extends to iOS without codegen. Hilt is Android-only and requires KSP/KAPT.

### Key concepts to master
- **Module definition**:
  ```kotlin
  val clientModule = module {
      single { HttpClient { install(SSE); install(ContentNegotiation) { json() } } }
      single { McpClient(get(), get()) }  // get() resolves HttpClient
      single { SseClient(get()) }
  }
  val repoModule = module { single<OpenshipRepository> { OpenshipRepositoryImpl(get(), get()) } }
  val vmModule = module { viewModel { ProjectsViewModel(get()) } }
  ```
- **Compose integration**:
  ```kotlin
  @Composable fun ProjectsScreen() {
      val vm: ProjectsViewModel = koinViewModel()
      // ...
  }
  ```

### Resources
- [Koin documentation](https://insert-koin.io/docs/quickstart/android/)
- [Koin with Compose](https://insert-koin.io/docs/reference/koin-compose/)
- [Koin with KMP](https://insert-koin.io/docs/reference/koin-multiplatform/)

---

## 10. Kotlin Coroutines & Flow 🟡

### What to study
- `suspend` functions and coroutine builders (`launch`, `async`)
- `Flow` — cold streams, `collect`, operators (`map`, `filter`, `catch`, `onCompletion`)
- `StateFlow` — hot streams for UI state
- `SharedFlow` — hot streams for events (one-shot UI events like navigation)
- `CoroutineScope` and `CoroutineExceptionHandler`
- Cancellation: `cancel()`, `isActive`, cooperative cancellation
- `Flow` lifecycle in Compose: `collectAsState()`, `collectAsStateWithLifecycle()`

### Why it matters
SSE streams are `Flow<ServerSseEvent>`. MCP calls are `suspend` functions. UI state is `StateFlow<UiState>`. The entire app is coroutine-based. You need to understand cancellation to properly stop SSE streams when the screen leaves composition.

### Key concepts to master
- **SSE as Flow**:
  ```kotlin
  fun streamDeployLogs(id: String, since: Long): Flow<DeployStreamEvent> = flow {
      httpClient.sse(urlString = "...", request = { header("Authorization", "Bearer $pat") }) {
          incoming.collect { event -> emit(parseEvent(event)) }
      }
  }
  ```
- **StateFlow for UI state**:
  ```kotlin
  class ProjectsViewModel(private val repo: OpenshipRepository) : ViewModel() {
      private val _state = MutableStateFlow<UiState>(UiState.Loading)
      val state: StateFlow<UiState> = _state.asStateFlow()
  }
  ```
- **Lifecycle-aware collection**:
  ```kotlin
  val state by vm.state.collectAsStateWithLifecycle()
  ```

### Resources
- [Kotlin Coroutines guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Asynchronous Flow](https://kotlinlang.org/docs/flow.html)
- [StateFlow and SharedFlow](https://kotlinlang.org/docs/stateflow-and-sharedflow.html)
- [collectAsStateWithLifecycle](https://developer.android.com/topic/libraries/architecture/lifecycle)

---

## 11. Openship API Contract 🟡

### What to study
- Read the [README.md §7](README.md#7-api-contract--4-base-features) API contract section thoroughly
- Explore the Openship server source at `../openship/`:
  - `apps/api/src/app.ts:169-186` — route registration
  - `apps/api/src/modules/health/health.routes.ts` — health endpoints
  - `apps/api/src/middleware/auth.ts` — auth middleware (PAT, cookies, no query token)
  - `apps/api/src/lib/bearer.ts` — PAT parsing
  - `apps/api/src/modules/mcp/` — MCP module
  - `apps/dashboard/src/hooks/useSSEConnection.ts` — how dashboard consumes deploy SSE
  - `apps/dashboard/src/hooks/useMonitorStream.ts` — how dashboard consumes monitor SSE
  - `apps/dashboard/src/lib/sseClient.ts` — dashboard SSE client implementation
  - `apps/dashboard/src/lib/api/endpoints.ts` — hand-maintained route table

### Why it matters
You're building against a real API with no OpenAPI spec. Understanding the exact request/response shapes, SSE event protocols, and auth flow is essential. The dashboard source code is your reference implementation — it shows exactly how to consume each endpoint.

### Key things to verify by running Openship
1. Start Openship: `cd ../openship && bun dev`
2. `curl http://localhost:4000/api/health` — verify health response shape
3. `curl http://localhost:4000/api/health/env` — verify env discovery response
4. Create a PAT in the dashboard (`http://localhost:3001` → Settings → API Tokens)
5. `curl -H "Authorization: Bearer opsh_pat_..." http://localhost:4000/api/projects/home` — verify project list
6. Trigger a deployment and watch the SSE stream:
   ```bash
   curl -N -H "Authorization: Bearer opsh_pat_..." \
     -H "Accept: text/event-stream" \
     http://localhost:4000/api/deployments/<id>/stream
   ```
7. Watch the monitor stream:
   ```bash
   curl -N -H "Authorization: Bearer opsh_pat_..." \
     -H "Accept: text/event-stream" \
     "http://localhost:4000/api/system/monitor/stream?serverId=<id>"
   ```
8. Call the MCP endpoint to discover tool names:
   ```bash
   # Initialize
   curl -X POST http://localhost:4000/api/mcp \
     -H "Authorization: Bearer opsh_pat_..." \
     -H "Content-Type: application/json" \
     -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test","version":"0.1.0"}}}'
   
   # List tools (after initialize)
   curl -X POST http://localhost:4000/api/mcp \
     -H "Authorization: Bearer opsh_pat_..." \
     -H "Content-Type: application/json" \
     -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
   ```

### Resources
- [README.md §7 — API Contract](README.md#7-api-contract--4-base-features)
- [README.md §8 — Auth Model](README.md#8-auth-model)
- [README.md §9 — MCP SDK Integration](README.md#9-mcp-sdk-integration)
- Openship server source: `../openship/`

---

## 12. Android Lifecycle & Background Handling 🟡

### What to study
- Activity lifecycle: `onCreate` → `onStart` → `onResume` → `onPause` → `onStop` → `onDestroy`
- Compose lifecycle: `LocalLifecycleOwner`, `LifecycleResumeEffect`, `DisposableEffect`
- Process death and state restoration (`rememberSaveable`, `SavedStateHandle`)
- Background execution limits (Android 8+) — no long-running network in background
- WorkManager for deferred background work (future: push notifications)

### Why it matters
When the app goes to background, SSE sockets die and MCP sessions expire. You must:
1. Cancel SSE streams on `onPause`/`onStop` (save `lastSeq` for deploy logs)
2. Disconnect MCP client on background
3. Reconnect both on `onResume` (MCP re-handshake, SSE with `?since=<seq>` for deploy logs, fresh for monitor)
4. Handle process death — restore instance configs from EncryptedSharedPreferences

### Key concepts to master
- **`DisposableEffect` for cleanup**:
  ```kotlin
  DisposableEffect(Unit) {
      val job = scope.launch { sseClient.streamDeployLogs(id).collect { ... } }
      onDispose { job.cancel() }  // Cancel SSE when screen leaves
  }
  ```
- **`LifecycleResumeEffect` for reconnect**:
  ```kotlin
  LifecycleResumeEffect(Unit) {
      reconnectMcpAndSse()
      onPauseOrDispose { disconnectAll() }
  }
  ```

### Resources
- [Activity lifecycle](https://developer.android.com/guide/components/activities/activity-lifecycle)
- [Compose lifecycle](https://developer.android.com/jetpack/compose/lifecycle)
- [Process death and state](https://developer.android.com/topic/libraries/architecture/saving-states)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)

---

## 13. Git & Conventional Commits 🟢

### What to study
- Conventional Commits format: `type(scope): description`
  - `feat(connect): add instance URL validation`
  - `fix(sse): handle base64 decode error`
  - `docs: update README with API contract`
  - `refactor(repo): extract repository interface`
- Beads workflow: `bd ready` → `bd create` → `bd update --claim` → `bd close`

### Why it matters
The project uses conventional commits and Beads for task tracking. Clean commit history makes PRs easier to review and changelog generation automatic.

### Resources
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Beads documentation](https://github.com/kareemessamessam/bd)

---

## 14. Base64 Decoding 🟢

### What to study
- Base64 encoding/decoding in Kotlin Multiplatform
- `kotlin.io.encoding.Base64` (available in commonMain since Kotlin 1.8)
- URL-safe vs standard Base64 (PAT uses base64url, SSE log data uses standard base64)

### Why it matters
SSE `log` events have a base64-encoded `data` field. You must decode it before displaying to the user. The PAT itself is base64url-encoded but you send it as-is in the header — no decoding needed.

### Key concepts
```kotlin
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
val decodedLogLine = Base64.decode(logEvent.data).decodeToString()
```

### Resources
- [kotlin.io.encoding.Base64](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.io.encoding/-base64/)

---

## Study Plan (Suggested Order)

### Week 1 — Foundations (Critical)
1. **KMP** (§1) — understand source sets, expect/actual, module structure
2. **Compose Multiplatform** (§2) — composables, state hoisting, side effects, navigation
3. **kotlinx.serialization** (§4) — @Serializable, Json config, sealed classes
4. **Kotlin Coroutines & Flow** (§10) — suspend, Flow, StateFlow, cancellation

### Week 2 — Networking (Critical)
5. **Ktor Client** (§3) — HttpClient, SSE plugin, ContentNegotiation, engines
6. **MCP** (§5) — protocol, Streamable-HTTP, Kotlin SDK, tool discovery
7. **SSE Protocol** (§6) — wire format, reconnection, sequence tracking
8. **Openship API Contract** (§11) — read README, explore server source, curl endpoints

### Week 3 — Android Specifics (Important)
9. **Android Security** (§7) — Keystore, EncryptedSharedPreferences
10. **Android Networking** (§8) — cleartext, network security config
11. **Android Lifecycle** (§12) — background handling, reconnect strategy
12. **Koin** (§9) — DI modules, ViewModel injection, KMP compatibility

### While Building (Nice to have)
13. **Git & Conventional Commits** (§13) — as you commit
14. **Base64** (§14) — when you implement log decoding

---

## Hands-On Exercises

Before building the actual app, try these mini-exercises:

### Exercise 1: KMP + Ktor HTTP GET
Create a minimal KMP project with a `commonMain` function that does `httpClient.get("https://jsonplaceholder.typicode.com/posts/1")` and prints the result. This validates your Ktor + engine setup.

### Exercise 2: SSE Parsing
Write a `commonMain` function that connects to a public SSE endpoint (e.g., `https://httpbin.org/stream`) using Ktor's SSE plugin and prints each event. This validates your SSE setup.

### Exercise 3: MCP Client
Write a `commonMain` function that connects to a public MCP server (or a local test server) using the Kotlin MCP SDK, lists tools, and calls one. This validates your MCP SDK setup.

### Exercise 4: EncryptedSharedPreferences
Write an Android function that stores and retrieves a string using EncryptedSharedPreferences. This validates your token storage setup.

### Exercise 5: Compose Screen with State
Write a Compose screen with a text input, a button, and a text display. The button triggers a suspend function (simulated network call) and the display shows the result. Use state hoisting and `LaunchedEffect`. This validates your Compose + coroutines setup.

### Exercise 6: Curl the Openship API
Start Openship (`cd ../openship && bun dev`), create a PAT, and curl all four base endpoints (health, health/env, projects/home, deployments/:id/stream, system/monitor/stream). This gives you hands-on familiarity with the actual API responses.

---

## Checklist

- [ ] Understand KMP source sets and expect/actual (§1)
- [ ] Can write a Compose composable with state hoisting (§2)
- [ ] Can configure Ktor HttpClient with SSE + JSON (§3)
- [ ] Can define @Serializable data classes with tolerant Json (§4)
- [ ] Understand MCP protocol and Kotlin SDK connection pattern (§5)
- [ ] Understand SSE wire format and reconnection (§6)
- [ ] Can set up EncryptedSharedPreferences (§7)
- [ ] Can configure network_security_config.xml (§8)
- [ ] Can define Koin modules and inject ViewModels (§9)
- [ ] Understand Flow, StateFlow, and cancellation (§10)
- [ ] Have curled all four Openship base endpoints (§11)
- [ ] Understand background handling and reconnect strategy (§12)
- [ ] Know conventional commit format (§13)
- [ ] Can decode base64 in commonMain (§14)

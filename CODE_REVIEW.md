# Openship-App — Phase 2 Code Review

> **Date**: 2026-08-24
> **Scope**: All 57 Kotlin files across `shared/`, `androidApp/`, and tests
> **Reviewer**: Expert KMP Architect audit
> **Branch**: `master` @ `6c9efc4`

---

## Table of Contents

- [Critical Issues](#-critical-issues)
  - [C1. MCP Client Never Connected](#c1-mcp-client-never-connected--phase-2-redeployrollback-is-dead-at-runtime)
  - [C2. No collectAsStateWithLifecycle](#c2-no-collectasstatewithlifecycle--flows-collect-in-background)
  - [C3. Synchronous Disk I/O on Main Thread](#c3-synchronous-disk-io-on-main-thread)
  - [C4. SSE Flows Have No Reconnect](#c4-sse-flows-have-no-reconnect--streams-die-silently)
  - [C5. SeqTracker Not Thread-Safe](#c5-seqtracker-not-thread-safe)
  - [C6. Project Name Encoding Corrupts Names with Underscores](#c6-project-name-encoding-corrupts-names-with-underscores)
  - [C7. Unbounded Log List — OOM on Long Builds](#c7-unbounded-log-list--oom-on-long-builds)
  - [C8. No Lifecycle-Aware SSE/MCP Management](#c8-no-lifecycle-aware-ssemcp-management)
- [Warnings & Improvements](#-warnings--improvements)
  - [W1. N+1 HTTP Query Pattern](#w1-n1-http-query-pattern-in-projectsrepository)
  - [W2. No Error Type Hierarchy](#w2-no-error-type-hierarchy--all-errors-are-strings)
  - [W3. HttpClient Logging in Production](#w3-httpclient-logging-in-production)
  - [W4. UI State Classes Not Annotated for Compose Stability](#w4-ui-state-classes-not-annotated-for-compose-stability)
  - [W5. Computed Properties Recomputed Every Access](#w5-filteredprojects--filteredlogs-computed-properties-recomputed-every-access)
  - [W6. Theme State Not Persisted](#w6-theme-state-not-persisted)
  - [W7. LocalThemeMode Default Creates MutableState](#w7-localthememode-default-creates-mutablestate-in-compositionlocalof)
  - [W8. ConnectViewModel Has No PAT Format Validation](#w8-connectviewmodel-has-no-pat-format-validation)
  - [W9. Manual Date Parsing](#w9-manual-date-parsing--fragile-and-non-reusable)
  - [W10. MonitorRepository String contains for Type Detection](#w10-monitorrepositorystreamserverstats-uses-string-contains-for-type-detection)
  - [W11. Wildcard Imports Violate Project Conventions](#w11-wildcard-imports-violate-project-conventions)
  - [W12. InstanceConfig.pat Serialized in JSON](#w12-instanceconfigpat-serialized-in-json)
- [Structural Strengths](#-structural-strengths)
- [Summary Priority Matrix](#summary-priority-matrix)

---

## 🔴 Critical Issues

### C1. MCP Client Never Connected — Phase 2 Redeploy/Rollback Is Dead at Runtime

**Files**: `shared/commonMain/di/SharedModules.kt`, `shared/commonMain/viewmodel/ProjectsViewModel.kt`

**Problem**: `McpClient` is a Koin `single` but **nothing ever calls `connect()`**. `ProjectsViewModel.refreshRedeployAvailability()` calls `isRedeployAvailable()` which checks `isConnected` — always `false`. The entire Phase 2 feature set (redeploy, rollback) silently degrades to "unavailable" forever.

**Root cause**: No lifecycle hook bridges instance activation → MCP connection.

**Fix**: Introduce a `McpConnectionManager` that connects when an instance becomes active and disconnects on background/switch. Wire it into the ViewModels that need MCP.

```kotlin
// shared/commonMain/client/McpConnectionManager.kt
package com.kareemessam.openship.shared.client

import com.kareemessam.openship.shared.model.InstanceConfig
import com.kareemessam.openship.shared.storage.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class McpConnectionManager(
    private val mcpClient: McpClient,
    private val tokenStorage: TokenStorage,
) {
    private val _connectionState = MutableStateFlow<McpState>(McpState.Disconnected)
    val connectionState: StateFlow<McpState> = _connectionState.asStateFlow()

    fun connectActive() {
        val instance = tokenStorage.getActiveInstance() ?: return
        if (mcpClient.isConnected) return
        _connectionState.value = McpState.Connecting
        mcpClient.connect(instance)
            .onSuccess { _connectionState.value = McpState.Connected }
            .onFailure { _connectionState.value = McpState.Failed(it) }
    }

    fun disconnect() {
        mcpClient.disconnect()
        _connectionState.value = McpState.Disconnected
    }
}

sealed interface McpState {
    data object Disconnected : McpState
    data object Connecting : McpState
    data object Connected : McpState
    data class Failed(val error: Throwable) : McpState
}
```

```kotlin
// shared/commonMain/di/SharedModules.kt — add binding
single { McpConnectionManager(get(), get()) }
```

```kotlin
// shared/commonMain/App.kt — connect on launch + reconnect on foreground
@Composable
fun App(tokenStorage: TokenStorage = koinInject()) {
    val connectionManager: McpConnectionManager = koinInject()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Connect when app starts if an instance is active
    LaunchedEffect(Unit) { connectionManager.connectActive() }

    // Reconnect on foreground, disconnect on background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> connectionManager.connectActive()
                Lifecycle.Event.ON_STOP -> connectionManager.disconnect()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // ... rest of App
}
```

**Checklist**:
- [ ] Create `McpConnectionManager.kt`
- [ ] Add `McpState` sealed interface
- [ ] Register `McpConnectionManager` in `sharedModule`
- [ ] Add `DisposableEffect` lifecycle observer in `App.kt`
- [ ] Verify `isRedeployAvailable()` returns `true` after connecting
- [ ] Test: connect to instance → redeploy button appears

---

### C2. No `collectAsStateWithLifecycle` — Flows Collect in Background

**Files**: All screens — `ProjectsScreen.kt`, `DeployLogsScreen.kt`, `ServerMonitorScreen.kt`, `ConnectScreen.kt`, `DeploymentHistoryScreen.kt`, `MainDashboardScreen.kt`

**Problem**: Every screen uses `collectAsState()` instead of `collectAsStateWithLifecycle()`. SSE streams keep collecting when the app is backgrounded — wasting battery, holding sockets open (contradicts the README's "cancel SSE on background" design), and risking memory leaks.

**Fix**: Replace all occurrences. The dependency `androidx-lifecycle-runtimeCompose` is already in the catalog.

```kotlin
// Before (every screen)
val state by viewModel.state.collectAsState()

// After
val state by viewModel.state.collectAsStateWithLifecycle()
```

```kotlin
// If you need a specific min active state:
val state by viewModel.state.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.STARTED)
```

This single change enforces the "cancel SSE on background" requirement from the README at the Compose layer — the flow collection pauses when the lifecycle drops below `STARTED`, and the SSE `incoming.collect` inside the flow gets cancelled automatically.

**Checklist**:
- [ ] `ConnectScreen.kt` — replace `collectAsState()` → `collectAsStateWithLifecycle()`
- [ ] `ProjectsScreen.kt` — replace
- [ ] `DeployLogsScreen.kt` — replace
- [ ] `ServerMonitorScreen.kt` — replace
- [ ] `DeploymentHistoryScreen.kt` — replace
- [ ] `MainDashboardScreen.kt` — replace
- [ ] Verify `androidx-lifecycle-runtimeCompose` is in `shared/build.gradle.kts` commonMain deps
- [ ] Test: background app during SSE stream → stream pauses; foreground → stream resumes

---

### C3. Synchronous Disk I/O on Main Thread

**Files**: `App.kt:28`, `ProjectsViewModel.kt:init`, `MonitorViewModel.kt:init`

**Problem**: `EncryptedSharedPreferences` does disk I/O. These calls run on the main thread:

```kotlin
// App.kt — called during composition
startDestination = if (tokenStorage.getActiveInstance() != null) ...

// ProjectsViewModel.kt — called in init block
init { loadInstancesAndProjects() }  // calls tokenStorage.loadInstances() synchronously

// MonitorViewModel.kt — same pattern
init { loadServersAndStartMonitoring() }
```

On a cold start or after process death, EncryptedSharedPreferences can take 50-200ms. This causes jank or ANRs on slower devices.

**Fix**: Make `TokenStorage` suspend-based, or wrap calls in `Dispatchers.IO`. The cleanest KMP approach is to make the interface suspend and provide a loading state:

```kotlin
// shared/commonMain/storage/TokenStorage.kt
interface TokenStorage {
    suspend fun saveInstance(config: InstanceConfig)
    suspend fun loadInstances(): List<InstanceConfig>
    suspend fun getActiveInstance(): InstanceConfig?
    suspend fun setActiveInstance(id: String)
    suspend fun deleteInstance(id: String)
    suspend fun clearAll()
}
```

```kotlin
// shared/androidMain/platform/AndroidTokenStorage.kt
class AndroidTokenStorage(context: Context) : TokenStorage {

    private val prefs: EncryptedSharedPreferences by lazy {
        // ... existing MasterKey + EncryptedSharedPreferences setup
    }

    // All methods now run on Dispatchers.IO (caller's responsibility via suspend)
    override suspend fun loadInstances(): List<InstanceConfig> = withContext(Dispatchers.IO) {
        prefs.all.entries
            .filter { it.key != activeInstanceKey }
            .mapNotNull { (key, value) ->
                runCatching { json.decodeFromString<InstanceConfig>(value as String) }.getOrNull()
            }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun getActiveInstance(): InstanceConfig? = withContext(Dispatchers.IO) {
        val activeId = prefs.getString(activeInstanceKey, null)
        activeId?.let { id ->
            prefs.getString(id, null)?.let { json.decodeFromString<InstanceConfig>(it) }
        } ?: loadInstances().firstOrNull()
    }

    override suspend fun saveInstance(config: InstanceConfig) = withContext(Dispatchers.IO) {
        prefs.edit().putString(config.id, json.encodeToString(config)).apply()
        if (config.isDefault || getActiveInstance() == null) {
            setActiveInstance(config.id)
        }
    }

    override suspend fun setActiveInstance(id: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(activeInstanceKey, id).apply()
    }

    override suspend fun deleteInstance(id: String) = withContext(Dispatchers.IO) {
        val wasActive = prefs.getString(activeInstanceKey, null) == id
        prefs.edit().remove(id).apply()
        if (wasActive) {
            val remaining = loadInstances()
            if (remaining.isNotEmpty()) {
                setActiveInstance(remaining.first().id)
            } else {
                prefs.edit().remove(activeInstanceKey).apply()
            }
        }
    }

    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }
}
```

```kotlin
// App.kt — use a loading state instead of synchronous read
@Composable
fun App(tokenStorage: TokenStorage = koinInject()) {
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val hasInstance = withContext(Dispatchers.IO) { tokenStorage.getActiveInstance() != null }
        startDestination = if (hasInstance) Screen.Dashboard.route else Screen.Connect.route
    }

    if (startDestination == null) {
        // Splash/loading state
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    // ... NavHost with startDestination
}
```

```kotlin
// ProjectsViewModel.kt — launch in init, not synchronous
init { viewModelScope.launch { loadInstancesAndProjects() } }

private suspend fun loadInstancesAndProjects() {
    val instances = tokenStorage.loadInstances()  // now suspend
    val active = tokenStorage.getActiveInstance()
    // ... update state
}
```

```kotlin
// MonitorViewModel.kt — same pattern
init { viewModelScope.launch { loadServersAndStartMonitoring() } }

private suspend fun loadServersAndStartMonitoring() {
    val instances = tokenStorage.loadInstances()
    // ... update state
}
```

**Checklist**:
- [ ] Change `TokenStorage` interface to suspend functions
- [ ] Update `AndroidTokenStorage` — wrap all methods in `withContext(Dispatchers.IO)`
- [ ] Update `App.kt` — add loading state, use `LaunchedEffect` for `getActiveInstance()`
- [ ] Update `ProjectsViewModel.init` — wrap in `viewModelScope.launch`
- [ ] Update `MonitorViewModel.init` — wrap in `viewModelScope.launch`
- [ ] Update `ConnectViewModel.connect()` — already in `viewModelScope.launch`, just add `suspend`
- [ ] Update `DeploymentHistoryViewModel` — `LaunchedEffect` in `AppNavHost` already async
- [ ] Update all `TokenStorage` callers in ViewModels to use suspend
- [ ] Test: cold start → no jank on splash → correct start destination

---

### C4. SSE Flows Have No Reconnect — Streams Die Silently

**Files**: `DeployLogsRepository.kt`, `MonitorRepository.kt`

**Problem**: Both SSE flows terminate on exception. `DeployLogsViewModel.retry()` is manual. `MonitorRepository.streamServerStats` has an **empty catch block** — stream errors produce zero signal to the UI.

```kotlin
// MonitorRepository.kt — current: errors swallowed
catch { }  // stream ended silently, UI frozen on last stats forever
```

**Fix**: Add exponential backoff reconnect with a max attempt count, and surface errors to the UI:

```kotlin
// shared/commonMain/client/MonitorRepository.kt
fun streamServerStats(
    instance: InstanceConfig,
    serverId: String,
): Flow<MonitorStatsDto> = flow {
    var retryDelayMs = 1000L  // start at 1s
    val maxRetryDelayMs = 30_000L  // cap at 30s
    var consecutiveErrors = 0
    val maxConsecutiveErrors = 5

    while (consecutiveErrors < maxConsecutiveErrors) {
        try {
            val baseUrl = DiscoveryService.normalizeUrl(instance.url)
            httpClient.sse(
                urlString = "$baseUrl/api/system/monitor/stream?serverId=$serverId",
                request = {
                    header("Authorization", "Bearer ${instance.pat}")
                    timeout { requestTimeout = HttpTimeoutConfig.INFINITE_TIMEOUT_MS }
                },
            ).incoming.collect { event ->
                val data = event.data ?: return@collect
                if (data.contains("\"cpu\"")) {
                    val stats = tolerantJson.decodeFromString<MonitorStatsDto>(data)
                    emit(stats)
                    consecutiveErrors = 0  // reset on successful emission
                    retryDelayMs = 1000L   // reset backoff
                }
            }
            // Stream ended normally (server closed) — try to reconnect
            consecutiveErrors++
        } catch (e: CancellationException) {
            throw e  // don't swallow cancellation
        } catch (e: Exception) {
            consecutiveErrors++
        }

        if (consecutiveErrors < maxConsecutiveErrors) {
            delay(retryDelayMs)
            retryDelayMs = (retryDelayMs * 2).coerceAtMost(maxRetryDelayMs)
        }
    }
    // After max retries, emit a sentinel or throw — let the UI show "connection lost"
}.flowOn(Dispatchers.IO)
```

```kotlin
// shared/commonMain/client/DeployLogsRepository.kt — same pattern, but preserve seqTracker for resume
fun streamDeployLogs(
    instance: InstanceConfig,
    deploymentId: String,
    seqTracker: SeqTracker,
): Flow<DeployStreamEvent> = flow {
    var retryDelayMs = 1000L
    val maxRetryDelayMs = 15_000L
    var consecutiveErrors = 0

    while (consecutiveErrors < 5) {
        try {
            val baseUrl = DiscoveryService.normalizeUrl(instance.url)
            val resumeSeq = seqTracker.getResumeParam()
            val url = if (resumeSeq != "0") {
                "$baseUrl/api/deployments/$deploymentId/stream?since=$resumeSeq"
            } else {
                "$baseUrl/api/deployments/$deploymentId/stream"
            }

            httpClient.sse(
                urlString = url,
                request = {
                    header("Authorization", "Bearer ${instance.pat}")
                    header("Accept", "text/event-stream")
                    timeout {
                        requestTimeout = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                        socketTimeout = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                    }
                },
            ).incoming.collect { event ->
                val parsed = parseDeployEvent(event)
                if (parsed is DeployStreamEvent.Log && parsed.eventId > 0) {
                    seqTracker.update(parsed.eventId)
                }
                emit(parsed)
                consecutiveErrors = 0
                retryDelayMs = 1000L
            }
            consecutiveErrors++  // stream ended normally
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            consecutiveErrors++
        }

        if (consecutiveErrors < 5) {
            delay(retryDelayMs)
            retryDelayMs = (retryDelayMs * 2).coerceAtMost(maxRetryDelayMs)
        }
    }
    emit(DeployStreamEvent.Error(
        error = "connection_lost",
        message = "Stream disconnected after multiple retries"
    ))
}.flowOn(Dispatchers.IO)
```

**Checklist**:
- [ ] Rewrite `MonitorRepository.streamServerStats` with reconnect loop + backoff
- [ ] Rewrite `DeployLogsRepository.streamDeployLogs` with reconnect loop + backoff
- [ ] Ensure `CancellationException` is rethrown, not swallowed
- [ ] Add `flowOn(Dispatchers.IO)` to both flows
- [ ] Surface terminal error to UI after max retries
- [ ] Test: kill server mid-stream → UI shows error after retries exhausted
- [ ] Test: deploy log reconnect resumes from last `eventId` (no duplicate lines)

---

### C5. SeqTracker Not Thread-Safe

**File**: `shared/commonMain/util/SeqTracker.kt`

**Problem**: The flow collector updating `lastSeq` may run on a different dispatcher than the one reading it for resume. No synchronization → lost updates, duplicate log lines on reconnect.

```kotlin
// Current — race condition
class SeqTracker {
    var lastSeq: Long = 0L private set
    fun update(seq: Long) { if (seq > lastSeq) lastSeq = seq }
}
```

**Fix Option A** (preferred — `kotlinx-atomicfu`):

```kotlin
// shared/commonMain/util/SeqTracker.kt
import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic

class SeqTracker {
    private val _lastSeq = atomic(0L)

    val lastSeq: Long get() = _lastSeq.value

    fun update(seq: Long) {
        // CAS loop: only update if seq is greater
        while (true) {
            val current = _lastSeq.value
            if (seq <= current) break
            if (_lastSeq.compareAndSet(current, seq)) break
        }
    }

    fun getResumeParam(): String = _lastSeq.value.toString()

    fun reset() { _lastSeq.value = 0L }
}
```

> Add `org.jetbrains.kotlinx:atomicfu` to `commonMain` dependencies in `gradle/libs.versions.toml`.

**Fix Option B** (no extra dependency — `@Volatile` + `synchronized`):

```kotlin
// shared/commonMain/util/SeqTracker.kt
class SeqTracker {
    @Volatile
    private var _lastSeq: Long = 0L

    val lastSeq: Long get() = _lastSeq

    @Synchronized
    fun update(seq: Long) {
        if (seq > _lastSeq) _lastSeq = seq
    }

    fun getResumeParam(): String = _lastSeq.toString()

    @Synchronized
    fun reset() { _lastSeq = 0L }
}
```

**Checklist**:
- [ ] Choose approach (atomicfu vs synchronized)
- [ ] If atomicfu: add dependency to `gradle/libs.versions.toml` + `shared/build.gradle.kts`
- [ ] Rewrite `SeqTracker` class
- [ ] Test: rapid concurrent updates → `lastSeq` always equals max submitted value
- [ ] Test: reconnect after concurrent updates → no duplicate log lines

---

### C6. Project Name Encoding Corrupts Names with Underscores

**Files**: `shared/commonMain/ui/navigation/Screen.kt`, `AppNavHost.kt`

**Problem**:

```kotlin
// Screen.kt — current
fun createRoute(projectId: String, deploymentId: String, projectName: String): String {
    return "$projectId/$deploymentId/${projectName.replace("/", "_")}"
}

// AppNavHost.kt — reversal
val projectName = args.projectName.replace("_", "/")
```

A project named `my_awesome_project` becomes `my/awesome/project` on the other side. This breaks navigation and display.

**Fix**: Use `URLEncoder` / `URLDecoder`:

```kotlin
// shared/commonMain/ui/navigation/Screen.kt
import java.net.URLEncoder

fun createRoute(projectId: String, deploymentId: String, projectName: String): String {
    return "$projectId/$deploymentId/${URLEncoder.encode(projectName, "UTF-8")}"
}
```

```kotlin
// shared/commonMain/ui/navigation/AppNavHost.kt
import java.net.URLDecoder

val projectName = URLDecoder.decode(args.projectName, "UTF-8")
```

> For pure KMP without `java.net`, use `kotlin.io.encoding.Base64` or add a `expect fun urlEncode/Decode` pair. But since the current target is Android-only, `java.net` is fine.

**Checklist**:
- [ ] Update `Screen.Logs.createRoute` — use `URLEncoder.encode`
- [ ] Update `Screen.DeploymentHistory.createRoute` — use `URLEncoder.encode`
- [ ] Update `AppNavHost.kt` Logs route — use `URLDecoder.decode`
- [ ] Update `AppNavHost.kt` DeploymentHistory route — use `URLDecoder.decode`
- [ ] Test: project name with underscores (`my_awesome_project`) → round-trips correctly
- [ ] Test: project name with slashes (`org/repo`) → round-trips correctly
- [ ] Test: project name with spaces and special chars → round-trips correctly

---

### C7. Unbounded Log List — OOM on Long Builds

**File**: `shared/commonMain/viewmodel/DeployLogsViewModel.kt`

**Problem**: `logs: List<LogItem>` grows indefinitely. A 30-minute build with verbose output can produce 50,000+ lines. Each `LogItem` holds an `AnnotatedString` (parsed ANSI). This will OOM on low-end devices and cause severe recomposition.

**Fix**: Cap the list with a sliding window:

```kotlin
// shared/commonMain/viewmodel/DeployLogsViewModel.kt

companion object {
    private const val MAX_LOG_ITEMS = 2_000  // ~2000 lines, enough for most builds
}

// In the Log event handler:
is DeployStreamEvent.Log -> {
    val decoded = decodeBase64ToString(event.data)
    val lines = decoded.split("\n").filter { it.isNotBlank() }
    val newItems = lines.map { line ->
        LogItem(
            id = logCounter++,
            rawText = line,
            parsedText = AnsiParser.parse(line),
            step = event.step,
            level = event.level,
            serviceName = event.serviceName,
        )
    }

    _state.update { current ->
        val combined = current.logs + newItems
        // Trim oldest if over cap — keep the most recent MAX_LOG_ITEMS
        val trimmed = if (combined.size > MAX_LOG_ITEMS) {
            combined.takeLast(MAX_LOG_ITEMS)
        } else {
            combined
        }
        current.copy(
            logs = trimmed,
            currentStage = determineStage(event.step) ?: current.currentStage,
        )
    }
}
```

> **Ponytail note**: If you need full log history for search, write raw lines to a ring buffer file and only keep parsed `AnnotatedString` for the visible window. But for v0.1.0, a 2000-line cap is sufficient — add file-backed history when users report missing lines.

**Checklist**:
- [ ] Add `MAX_LOG_ITEMS` constant to `DeployLogsViewModel` companion object
- [ ] Update Log event handler to trim list with `takeLast(MAX_LOG_ITEMS)`
- [ ] Test: simulate 5000+ log lines → list stays at 2000, no OOM
- [ ] Test: auto-scroll still works after trimming
- [ ] Test: search still works on visible (trimmed) logs

---

### C8. No Lifecycle-Aware SSE/MCP Management

**Files**: All ViewModels with `streamJob: Job?`

**Problem**: The README specifies "Cancel SSE on background, reconnect on foreground." Currently, SSE streams are only cancelled in `onCleared()` (ViewModel destruction). Backgrounding the app keeps streams alive, draining battery and holding sockets.

**Fix**: Use `LifecycleResumeEffect` or `DisposableEffect` in screens that host SSE streams:

```kotlin
// shared/commonMain/ui/screens/logs/DeployLogsScreen.kt
@Composable
fun DeployLogsScreen(
    state: DeployLogsUiState,
    onBack: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onAutoScrollChanged: (Boolean) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeployLogsViewModel = koinViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Pause stream on background, resume on foreground
    LifecycleResumeEffect(Unit) {
        viewModel.resumeStream()
        onPauseOrDispose { viewModel.pauseStream() }
    }

    // ... rest of screen
}
```

```kotlin
// DeployLogsViewModel.kt — add pause/resume
private var savedSeq: Long = 0L

fun pauseStream() {
    streamJob?.cancel()
    streamJob = null
    _state.update { it.copy(isStreaming = false) }
}

fun resumeStream() {
    if (streamJob?.isActive == true) return
    val current = _state.value
    if (current.deploymentId.isNotBlank()) {
        startStream(current.deploymentId, current.projectName)
    }
}
```

```kotlin
// ServerMonitorScreen.kt — same pattern
@Composable
fun ServerMonitorScreen(...) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LifecycleResumeEffect(Unit) {
        viewModel.resumeStream()
        onPauseOrDispose { viewModel.pauseStream() }
    }
    // ...
}
```

```kotlin
// MonitorViewModel.kt — add pause/resume
fun pauseStream() {
    streamJob?.cancel()
    streamJob = null
    _state.update { it.copy(isStreaming = false) }
}

fun resumeStream() {
    if (streamJob?.isActive == true) return
    val current = _state.value
    if (current.activeServer != null && !current.isCloudMode) {
        startStreaming(current.activeServer)
    }
}
```

**Checklist**:
- [ ] Add `pauseStream()` / `resumeStream()` to `DeployLogsViewModel`
- [ ] Add `pauseStream()` / `resumeStream()` to `MonitorViewModel`
- [ ] Add `LifecycleResumeEffect` to `DeployLogsScreen`
- [ ] Add `LifecycleResumeEffect` to `ServerMonitorScreen`
- [ ] Test: open deploy logs → background app → stream pauses → foreground → stream resumes from last seq
- [ ] Test: open monitor → background app → stream pauses → foreground → stream resumes
- [ ] Verify battery usage drops when backgrounded

---

## 🟡 Warnings & Improvements

### W1. N+1 HTTP Query Pattern in ProjectsRepository

**File**: `shared/commonMain/client/ProjectsRepository.kt`

**Problem**: `getProjects` fetches all projects, then makes a **separate** `GET /api/deployments?projectId=X` for each project with an `activeDeploymentId`. That's 1 + N HTTP calls on every refresh.

**Fix**: The API has `GET /api/projects/home` which returns enriched project data. Use it as the primary endpoint, fall back to the N+1 pattern only if `home` is unavailable:

```kotlin
// shared/commonMain/client/ProjectsRepository.kt
suspend fun getProjects(instance: InstanceConfig): Result<List<ProjectSummary>> = runCatching {
    val baseUrl = DiscoveryService.normalizeUrl(instance.url)
    val response = httpClient.get("$baseUrl/api/projects/home") {
        header("Authorization", "Bearer ${instance.pat}")
    }

    if (response.status.isSuccess()) {
        val homeResponse = response.body<ProjectsApiResponse>()
        // /api/projects/home includes deployment status inline — no N+1 needed
        homeResponse.data.map { it.toProjectSummary() }
    } else {
        // Fallback: paginated + per-project deployment fetch
        fetchProjectsPaginated(baseUrl, instance.pat)
    }
}

private suspend fun fetchProjectsPaginated(
    baseUrl: String,
    pat: String,
): List<ProjectSummary> = coroutineScope {
    val response = httpClient.get("$baseUrl/api/projects") {
        header("Authorization", "Bearer $pat")
    }
    val projectsResponse = response.body<ProjectsApiResponse>()

    // Batch: fetch all deployments in parallel (still N calls, but parallel)
    projectsResponse.data.map { project ->
        async {
            if (project.activeDeploymentId.isNullOrBlank()) {
                project.toFallbackSummary()
            } else {
                fetchProjectWithDeployment(baseUrl, pat, project)
            }
        }
    }.awaitAll()
}
```

> The current `async { }.awaitAll()` pattern does parallelize the N calls, which mitigates latency. But it's still N HTTP requests. Prefer `/api/projects/home` when available.

**Checklist**:
- [ ] Verify `/api/projects/home` response shape (check Openship server source)
- [ ] Add `toProjectSummary()` extension on `ProjectDto` for home endpoint
- [ ] Add `toFallbackSummary()` extension on `ProjectDto` for paginated fallback
- [ ] Implement `fetchProjectsPaginated` with existing parallel `async` pattern
- [ ] Test: home endpoint available → single HTTP call
- [ ] Test: home endpoint unavailable → falls back to paginated + parallel deployment fetch

---

### W2. No Error Type Hierarchy — All Errors Are Strings

**Files**: All repositories and ViewModels

**Problem**: Every error is a `String` message. The UI can't distinguish "no internet" from "401 unauthorized" from "500 server error." This means you can't show "Check your connection" vs "Your token expired" vs "Server error, try again."

**Fix**: Introduce a sealed error hierarchy:

```kotlin
// shared/commonMain/model/ApiError.kt
sealed interface ApiError {
    val message: String

    data class Network(override val message: String = "No internet connection") : ApiError
    data class Unauthorized(override val message: String = "Authentication failed. Check your PAT.") : ApiError
    data class NotFound(override val message: String = "Resource not found") : ApiError
    data class ServerError(val code: Int, override val message: String) : ApiError
    data class McpError(val toolName: String, override val message: String) : ApiError
    data class Unknown(override val message: String) : ApiError
}
```

```kotlin
// Example usage in DiscoveryService
fun discoverInstance(rawUrl: String): Result<HealthEnv> = runCatching {
    val baseUrl = normalizeUrl(rawUrl)
    val response = httpClient.get("$baseUrl/api/health/env")
    when {
        response.status == HttpStatusCode.Unauthorized ->
            throw ApiException(ApiError.Unauthorized())
        !response.status.isSuccess() ->
            throw ApiException(ApiError.ServerError(response.status.value, "HTTP ${response.status.value}"))
        else -> response.body<HealthEnv>()
    }
}
```

> **Ponytail note**: Don't over-engineer this. If the current `String` error messages are working for Phase 2, add the sealed class when you need to show different UI for different error types (e.g., "reconnect" button for network errors vs "re-login" for auth errors). The sealed class above is the minimum useful shape.

**Checklist**:
- [ ] Create `ApiError.kt` sealed interface
- [ ] Create `ApiException(val error: ApiError)` wrapper
- [ ] Update `DiscoveryService` to throw typed errors
- [ ] Update `ProjectsRepository` to throw typed errors
- [ ] Update `DeploymentsRepository` to throw typed errors
- [ ] Update `McpClient` to throw `ApiError.McpError`
- [ ] Update ViewModels to map `ApiError` → user-facing message
- [ ] Update screens to show different UI for different error types
- [ ] Test: 401 → "Check your PAT" message
- [ ] Test: network error → "No internet" message

---

### W3. HttpClient Logging in Production

**File**: `shared/commonMain/client/HttpClientFactory.kt`

**Problem**:

```kotlin
install(Logging) {
    level = LogLevel.HEADERS
    logger = Logger.DEFAULT  // println
}
```

Headers (including redacted auth) are logged via `println` in **release** builds. No `BuildConfig.DEBUG` guard.

**Fix**: Gate logging on debug builds:

```kotlin
// shared/commonMain/client/HttpClientFactory.kt
install(Logging) {
    level = if (isDebugBuild()) LogLevel.HEADERS else LogLevel.NONE
    logger = object : Logger {
        override fun log(message: String) {
            // Redact Bearer tokens
            val sanitized = message.replace(
                Regex("Bearer [A-Za-z0-9_\\-]+"),
                "Bearer [REDACTED]",
            )
            // Use platform logger, not println
            platformLog(sanitized)
        }
    }
}
```

```kotlin
// shared/commonMain/util/PlatformLog.kt — expect
expect fun isDebugBuild(): Boolean
expect fun platformLog(message: String)
```

```kotlin
// shared/androidMain/util/PlatformLog.android.kt — actual
import com.kareemessam.openship.shared.BuildConfig

actual fun isDebugBuild(): Boolean = BuildConfig.DEBUG
actual fun platformLog(message: String) = android.util.Log.d("OpenshipHttp", message)
```

**Checklist**:
- [ ] Create `PlatformLog.kt` expect declaration in `commonMain/util/`
- [ ] Create `PlatformLog.android.kt` actual in `androidMain/util/`
- [ ] Update `HttpClientFactory.create()` to use `isDebugBuild()` guard
- [ ] Replace `println` with `platformLog`
- [ ] Test: debug build → headers logged
- [ ] Test: release build → no HTTP logging

---

### W4. UI State Classes Not Annotated for Compose Stability

**Files**: All `*UiState` data classes

**Problem**: `ConnectUiState`, `ProjectsUiState`, `DeployLogsUiState`, `MonitorUiState`, `DeploymentHistoryUiState` are plain `data class` with `List` fields. Compose treats `List` as **unstable**, so any composable receiving these states will recompose on every parent recomposition even if the state hasn't meaningfully changed.

**Fix**: Annotate with `@Immutable` (all fields are `val`, no mutable state, lists are replaced not mutated):

```kotlin
// shared/commonMain/viewmodel/ConnectViewModel.kt
@Immutable
data class ConnectUiState(
    val url: String = "http://10.0.2.2:4000",
    val label: String = "My Openship Server",
    val pat: String = "",
    val isProbing: Boolean = false,
    val discoveredEnv: HealthEnv? = null,
    val probeError: String? = null,
    val isConnecting: Boolean = false,
    val connectError: String? = null,
    val isSuccess: Boolean = false,
)
```

```kotlin
// shared/commonMain/viewmodel/ProjectsViewModel.kt
@Immutable
data class ProjectsUiState(
    val activeInstance: InstanceConfig? = null,
    val allInstances: List<InstanceConfig> = emptyList(),
    val projects: List<ProjectSummary> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val redeployAvailable: Boolean = false,
    val redeployTarget: ProjectSummary? = null,
    val redeployLoading: Boolean = false,
    val redeployError: String? = null,
    val redeployResultDeploymentId: String? = null,
)
```

```kotlin
// shared/commonMain/viewmodel/DeployLogsViewModel.kt
@Immutable
data class DeployLogsUiState(
    val projectName: String = "",
    val deploymentId: String = "",
    val isStreaming: Boolean = false,
    val logs: List<LogItem> = emptyList(),
    val searchQuery: String = "",
    val autoScroll: Boolean = true,
    val currentStage: BuildStage = BuildStage.CLONE,
    val finalStatus: String? = null,
    val error: String? = null,
)

@Immutable
data class LogItem(
    val id: Long,
    val rawText: String,
    val parsedText: AnnotatedString,
    val step: String? = null,
    val level: String? = null,
    val serviceName: String? = null,
)
```

```kotlin
// shared/commonMain/viewmodel/MonitorViewModel.kt
@Immutable
data class MonitorUiState(
    val activeInstance: InstanceConfig? = null,
    val allInstances: List<InstanceConfig> = emptyList(),
    val activeServer: ServerItemDto? = null,
    val allServers: List<ServerItemDto> = emptyList(),
    val currentStats: MonitorStatsDto? = null,
    val cpuHistory: List<Float> = emptyList(),
    val memHistory: List<Float> = emptyList(),
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val isCloudMode: Boolean = false,
    val error: String? = null,
)
```

```kotlin
// shared/commonMain/viewmodel/DeploymentHistoryViewModel.kt
@Immutable
data class DeploymentHistoryUiState(
    val isLoading: Boolean = false,
    val deployments: List<DeploymentDto> = emptyList(),
    val selectedDeploymentId: String? = null,
    val error: String? = null,
    val projectId: String = "",
    val instance: InstanceConfig? = null,
    val activeDeploymentId: String? = null,
    val rollbackAvailable: Boolean = false,
    val rollbackTarget: DeploymentDto? = null,
    val rollbackLoading: Boolean = false,
    val rollbackError: String? = null,
    val rollbackResultDeploymentId: String? = null,
)
```

**Checklist**:
- [ ] Add `@Immutable` to `ConnectUiState`
- [ ] Add `@Immutable` to `ProjectsUiState`
- [ ] Add `@Immutable` to `DeployLogsUiState`
- [ ] Add `@Immutable` to `LogItem`
- [ ] Add `@Immutable` to `MonitorUiState`
- [ ] Add `@Immutable` to `DeploymentHistoryUiState`
- [ ] Verify all fields are `val` (no `var`, no mutable collections)
- [ ] Test: recomposition counts drop (use Layout Inspector)

---

### W5. `filteredProjects` / `filteredLogs` Computed Properties Recomputed Every Access

**Files**: `ProjectsUiState`, `DeployLogsUiState`

**Problem**:

```kotlin
// Current — runs filter on every property read
val filteredProjects: List<ProjectSummary>
    get() = projects.filter { ... }
```

In Compose, reading this property in a composable creates a snapshot read, but the filter itself runs on **every recomposition** even if `projects` and `searchQuery` haven't changed.

**Fix**: Move filtering to `derivedStateOf` in the composable, or compute in the ViewModel:

```kotlin
// Option A: derivedStateOf in the composable (preferred for UI-only filtering)
@Composable
fun ProjectsScreen(state: ProjectsUiState, ...) {
    val displayedProjects by remember(state.projects, state.searchQuery, selectedFilter) {
        derivedStateOf {
            state.projects
                .filter { it.matchesSearch(state.searchQuery) }
                .filter { it.matchesTab(selectedFilter) }
        }
    }
    // Use displayedProjects.value
}
```

```kotlin
// Option B: compute in ViewModel and expose as StateFlow
// In ProjectsViewModel:
private val _filteredProjects = MutableStateFlow<List<ProjectSummary>>(emptyList())
val filteredProjects: StateFlow<List<ProjectSummary>> = _filteredProjects.asStateFlow()

// Update in fetchProjects and onSearchQueryChanged:
private fun updateFilteredProjects() {
    _filteredProjects.value = _state.value.projects
        .filter { it.matchesSearch(_state.value.searchQuery) }
}
```

**Checklist**:
- [ ] Remove `filteredProjects` computed property from `ProjectsUiState`
- [ ] Remove `filteredLogs` computed property from `DeployLogsUiState`
- [ ] Choose approach (derivedStateOf in composable vs StateFlow in ViewModel)
- [ ] If derivedStateOf: update `ProjectsScreen` and `DeployLogsScreen`
- [ ] If StateFlow: update `ProjectsViewModel` and `DeployLogsViewModel`
- [ ] Test: search filtering still works
- [ ] Test: recomposition counts drop when search query changes but projects don't

---

### W6. Theme State Not Persisted

**File**: `shared/commonMain/App.kt`

**Problem**:

```kotlin
themeModeState = remember { mutableStateOf(ThemeMode.DARK) }
```

Resets to `DARK` on every app launch. Users who switch to light mode lose their preference.

**Fix**: Persist to `SharedPreferences` (non-encrypted is fine for theme):

```kotlin
// shared/commonMain/App.kt
@Composable
fun App(tokenStorage: TokenStorage = koinInject()) {
    val context = LocalContext.current  // Android-only; for KMP use expect/actual
    val themePrefs = remember { context.getSharedPreferences("openship_ui", Context.MODE_PRIVATE) }

    val themeModeState = remember {
        mutableStateOf(
            ThemeMode.valueOf(themePrefs.getString("theme_mode", ThemeMode.DARK.name)!!)
        )
    }

    // Persist on change
    LaunchedEffect(themeModeState.value) {
        themePrefs.edit().putString("theme_mode", themeModeState.value.name).apply()
    }

    OpenshipTheme(themeModeState) { /* ... */ }
}
```

> For KMP purity, use `expect fun getThemePreference(): ThemeMode` / `expect fun saveThemePreference(mode: ThemeMode)` with platform-specific actuals.

**Checklist**:
- [ ] Add theme persistence (SharedPreferences on Android)
- [ ] Load theme on app start
- [ ] Save theme on change via `LaunchedEffect`
- [ ] Test: switch to light mode → kill app → relaunch → light mode persists
- [ ] Test: switch to dark mode → kill app → relaunch → dark mode persists

---

### W7. `LocalThemeMode` Default Creates MutableState in compositionLocalOf

**File**: `shared/commonMain/ui/theme/OpenshipTheme.kt`

**Problem**:

```kotlin
val LocalThemeMode = compositionLocalOf { mutableStateOf(ThemeMode.DARK) }
```

A `MutableState` as a `compositionLocalOf` default is unusual — it creates a new `MutableState` per read if no provider is set. This can lead to independent state instances that don't share updates.

**Fix**: Use `staticCompositionLocalOf` with a non-state default, or provide the state explicitly (which `OpenshipTheme` already does):

```kotlin
// If the theme mode is always provided by OpenshipTheme, use staticCompositionLocalOf
// with a sentinel default that will fail loudly if accessed without a provider:
val LocalThemeMode = staticCompositionLocalOf<MutableState<ThemeMode>> {
    error("LocalThemeMode not provided. Wrap your content in OpenshipTheme.")
}
```

> Since `OpenshipTheme` always provides it via `CompositionLocalProvider`, the default is never used in practice. But `staticCompositionLocalOf` + `error` is the correct pattern — it avoids unnecessary recompositions when the provider value changes (theme changes are rare and affect the whole tree).

**Checklist**:
- [ ] Change `LocalThemeMode` from `compositionLocalOf` to `staticCompositionLocalOf`
- [ ] Change default from `mutableStateOf(ThemeMode.DARK)` to `error("...")`
- [ ] Verify `OpenshipTheme` still provides it correctly
- [ ] Test: theme toggle still works
- [ ] Test: no crash when accessing theme within `OpenshipTheme`

---

### W8. ConnectViewModel Has No PAT Format Validation

**File**: `shared/commonMain/viewmodel/ConnectViewModel.kt`

**Problem**: Only checks `isEmpty()`. Doesn't validate the `opsh_pat_` prefix or minimum length. Users get cryptic 401 errors instead of a clear "invalid token format" message.

**Fix**:

```kotlin
// shared/commonMain/viewmodel/ConnectViewModel.kt
private fun validatePat(pat: String, authMode: String): String? {
    if (authMode == "none") return null  // No PAT needed
    if (pat.isBlank()) return "Personal Access Token is required"
    if (!pat.startsWith("opsh_pat_")) return "Token must start with 'opsh_pat_'"
    if (pat.length < 52) return "Token appears to be too short (expected opsh_pat_ + 43 characters)"
    return null
}

fun connect() {
    val patError = validatePat(state.value.pat, state.value.discoveredEnv?.authMode ?: "local")
    if (patError != null) {
        _state.update { it.copy(connectError = patError) }
        return
    }
    // ... existing connect logic
}
```

**Checklist**:
- [ ] Add `validatePat()` private function
- [ ] Call it in `connect()` before launching coroutine
- [ ] Test: empty PAT with auth mode "local" → "required" error
- [ ] Test: PAT without `opsh_pat_` prefix → "must start with" error
- [ ] Test: PAT too short → "too short" error
- [ ] Test: valid PAT → proceeds to connect
- [ ] Test: auth mode "none" → no PAT validation

---

### W9. Manual Date Parsing — Fragile and Non-Reusable

**File**: `shared/commonMain/viewmodel/DeploymentHistoryViewModel.kt`

**Problem**: `parseIsoToMillis` uses substring extraction at fixed positions. Works for strict ISO-8601 but breaks on any format variation. The `daysFromCivil` algorithm is correct but unnecessary if you use a library.

**Fix**: Add `kotlinx-datetime` (the idiomatic KMP datetime library):

```kotlin
// gradle/libs.versions.toml
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version = "0.6.0" }
```

```kotlin
// shared/build.gradle.kts — commonMain dependencies
implementation(libs.kotlinx.datetime)
```

```kotlin
// shared/commonMain/viewmodel/DeploymentHistoryViewModel.kt
import kotlinx.datetime.Instant

internal fun parseIsoToMillis(iso: String): Long = runCatching {
    Instant.parse(iso).toEpochMilliseconds()
}.getOrDefault(0L)

internal fun formatRelativeAge(iso: String, nowMillis: Long): String {
    val then = parseIsoToMillis(iso)
    if (then == 0L) return iso
    val diffMinutes = (nowMillis - then) / 60_000
    return when {
        diffMinutes < 1 -> "just now"
        diffMinutes < 60 -> "${diffMinutes}m ago"
        diffMinutes < 1440 -> "${diffMinutes / 60}h ago"
        else -> "${diffMinutes / 1440}d ago"
    }
}
```

> This deletes ~60 lines of manual parsing code. `kotlinx-datetime` is ~200KB, multiplatform, and handles all ISO-8601 edge cases including timezone offsets and fractional seconds.

**Checklist**:
- [ ] Add `kotlinx-datetime` to `gradle/libs.versions.toml`
- [ ] Add `implementation(libs.kotlinx.datetime)` to `shared/build.gradle.kts` commonMain
- [ ] Replace `parseIsoToMillis` with `Instant.parse(iso).toEpochMilliseconds()`
- [ ] Delete `daysFromCivil` private function
- [ ] Delete manual substring parsing code
- [ ] Test: ISO-8601 with timezone offset → correct millis
- [ ] Test: ISO-8601 with fractional seconds → correct millis
- [ ] Test: invalid date string → returns 0L (no crash)

---

### W10. `MonitorRepository.streamServerStats` Uses String `contains` for Type Detection

**File**: `shared/commonMain/client/MonitorRepository.kt`

**Problem**:

```kotlin
if (data.contains("\"cpu\"")) {
    val stats = tolerantJson.decodeFromString<MonitorStatsDto>(data)
    emit(stats)
}
```

This is fragile — any event data containing the string `"cpu"` (e.g., an error message mentioning CPU) would be parsed as stats.

**Fix**: Parse the event type explicitly:

```kotlin
// shared/commonMain/client/MonitorRepository.kt
incoming.collect { event ->
    val data = event.data ?: return@collect
    val eventType = event.event ?: ""

    when (eventType) {
        "stats" -> {
            val stats = tolerantJson.decodeFromString<MonitorStatsDto>(data)
            emit(stats)
        }
        "error" -> {
            // Surface non-fatal errors to UI
            val errorPayload = tolerantJson.decodeFromString<JsonObject>(data)
            val errorMsg = errorPayload["message"]?.jsonPrimitive?.contentOrNull ?: "Unknown server error"
            // Emit a special marker or log — don't crash the stream
        }
        else -> { /* ignore unknown events */ }
    }
}
```

**Checklist**:
- [ ] Replace `data.contains("\"cpu\"")` with `event.event` type check
- [ ] Handle `"stats"` event type explicitly
- [ ] Handle `"error"` event type — surface to UI
- [ ] Ignore unknown event types
- [ ] Test: stats event → parsed correctly
- [ ] Test: error event → surfaced to UI, stream continues
- [ ] Test: unknown event type → ignored, stream continues

---

### W11. Wildcard Imports Violate Project Conventions

**Files**: Multiple — `ProjectCard.kt`, `ProjectsScreen.kt`, `DeployLogsScreen.kt`, `ServerMonitorScreen.kt`, etc.

**Problem**: `AGENTS.md` states "No wildcard imports" but multiple files use `import androidx.compose.foundation.layout.*` and `import androidx.compose.material.icons.filled.*`.

**Fix**: Run the IDE "Optimize Imports" action or use a ktlint/detekt rule to enforce. This is a mechanical fix — no code logic changes needed.

**Checklist**:
- [ ] Run "Optimize Imports" in IDE on all files
- [ ] Or configure ktlint/detekt with `no-wildcard-imports` rule
- [ ] Verify all wildcard imports are expanded to specific imports
- [ ] Build passes after import cleanup

---

### W12. `InstanceConfig.pat` Serialized in JSON

**File**: `shared/commonMain/model/InstanceConfig.kt`, `shared/androidMain/platform/AndroidTokenStorage.kt`

**Problem**: The PAT is a field in `InstanceConfig` which gets JSON-serialized and stored in `EncryptedSharedPreferences`. While encrypted at rest, the PAT is in the serialized JSON string in memory. If the JSON is ever logged or leaked, the PAT is exposed.

**Fix**: Exclude `pat` from `toString()` and use a custom serializer that redacts it in logs:

```kotlin
@Serializable
data class InstanceConfig(
    val id: String,
    val label: String,
    val url: String,
    @SerialName("pat") val pat: String,
    val authMode: String = "local",
    val version: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long = 0L,
) {
    // Safe to log — pat is redacted
    override fun toString(): String =
        "InstanceConfig(id=$id, label=$label, url=$url, pat=${if (pat.isBlank()) "empty" else "[REDACTED]"}, authMode=$authMode, version=$version)"
}
```

> The PAT still needs to be in the serialized JSON for storage (EncryptedSharedPreferences handles encryption). The `toString()` override prevents accidental logging. For full separation, store the PAT in a separate encrypted key and keep `InstanceConfig` PAT-free, but that's a larger refactor.

**Checklist**:
- [ ] Override `toString()` in `InstanceConfig` to redact PAT
- [ ] Audit all log statements that might print `InstanceConfig`
- [ ] Test: `instanceConfig.toString()` → PAT is `[REDACTED]`
- [ ] Consider: separate PAT storage key for full separation (future refactor)

---

## 🟢 Structural Strengths

### S1. Excellent `expect/actual` Discipline

The project uses `expect/actual` only for truly platform-specific concerns (`PlatformEngine`, `TimeUtil`) and uses **interface + platform implementation** for `TokenStorage`. This is the correct KMP pattern — interfaces are more flexible than `expect/actual` for dependency injection and testing. The `androidPlatformModule` binding in Koin is clean and idiomatic.

### S2. Well-Structured Sealed Class for SSE Events

`DeployStreamEvent` is a `@Serializable sealed class` with `@SerialName` per subtype. This is the correct Kotlin pattern for discriminated unions. The `when` expressions in the ViewModel are exhaustive (compiler-enforced), and adding a new event type is a safe, compiler-guided change.

### S3. Proper Tolerant JSON Configuration

```kotlin
val tolerantJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    coerceInputValues = true
}
```

This is exactly right for an unversioned API. `ignoreUnknownKeys` handles additive API changes, `coerceInputValues` handles nulls for non-nullable fields, and `isLenient` handles minor format quirks. This is a best-practice configuration for a client talking to a server you don't control.

### S4. Correct SSE Auth Approach (Ktor SSE Plugin, Not EventSource)

The README and code correctly identify that native `EventSource` can't set `Authorization` headers, and use the Ktor SSE plugin instead. This is a common pitfall that was avoided. The `httpClient.sse(urlString, request = { header(...) })` pattern is the correct approach.

### S5. Good Theme Architecture

`OpenshipCustomColors` is `@Immutable` with a `staticCompositionLocalOf` provider. The dark/light token objects (`OpenshipColors.Dark` / `OpenshipColors.Light`) are clean, and the `OpenshipAppTheme.colors` accessor is `@ReadOnlyComposable`. This is the correct Compose theming pattern — immutable color tokens via composition local with a read-only accessor.

### S6. Defensive Fallbacks in Project Mapping

`ProjectsRepository.getProjects` has sensible fallbacks for missing fields (slug from name, framework "custom", gitRepo "owner/repo", branch "main", port 8080). The per-project catch block defaults to `READY` / "Active" instead of crashing. This is the right approach for a client dealing with an API that may return incomplete data.

### S7. Correct Base64 Log Decoding

The SSE `log` event data is base64-encoded per the API contract. `DeployLogsViewModel` correctly decodes with `decodeBase64ToString()` before display, and the utility has a graceful fallback to the original string on decode failure. This is defensive and correct.

### S8. Good Test Coverage for MCP Layer

`McpClientTest` (8 tests) and `DeployActionsRepositoryTest` (12 tests) cover the core Phase 2 logic: tool availability checks, error mapping, deployment ID extraction from multiple response shapes. The `internal` visibility on `McpClient.client`/`catalog` enables test substitution without exposing public API.

### S9. Clean Koin Module Structure

The separation between `sharedModule` (platform-agnostic) and `androidPlatformModule` (Android-specific bindings) is correct. This allows adding `iosPlatformModule` later without touching `sharedModule`. The `viewModel { }` bindings are idiomatic for Koin-Compose.

### S10. ANSI Parser is Thorough

`AnsiParser` handles the common ANSI color codes (30-37, 90-97, bold, reset) with a regex-based approach. The `buildAnnotatedString` with `SpanStyle` per color segment is the correct Compose approach for terminal-style colored text. The default color fallback and no-escape fast path are good performance touches.

---

## Summary Priority Matrix

| Priority | Issue | Effort | Impact | Status |
|----------|-------|--------|--------|--------|
| 🔴 C1 | MCP Client never connected | Medium | Phase 2 features completely broken | [ ] |
| 🔴 C2 | No `collectAsStateWithLifecycle` | Low | Battery drain, memory leaks | [ ] |
| 🔴 C3 | Sync disk I/O on main thread | Medium | Jank, potential ANRs | [ ] |
| 🔴 C4 | SSE no reconnect | Medium | Streams die silently | [ ] |
| 🔴 C5 | SeqTracker not thread-safe | Low | Duplicate logs on reconnect | [ ] |
| 🔴 C6 | Project name encoding | Low | Navigation corruption | [ ] |
| 🔴 C7 | Unbounded log list | Low | OOM on long builds | [ ] |
| 🔴 C8 | No lifecycle SSE management | Medium | Battery drain, contradicts design | [ ] |
| 🟡 W1 | N+1 HTTP queries | Medium | Slow refresh, battery drain | [ ] |
| 🟡 W2 | No error type hierarchy | Medium | Can't differentiate error UI | [ ] |
| 🟡 W3 | Production HTTP logging | Low | Information leak, perf | [ ] |
| 🟡 W4 | UI state not `@Immutable` | Low | Unnecessary recompositions | [ ] |
| 🟡 W5 | Computed properties recompute | Low | CPU waste on recomposition | [ ] |
| 🟡 W6 | Theme not persisted | Low | UX annoyance | [ ] |
| 🟡 W7 | `LocalThemeMode` default | Low | Subtle state bug | [ ] |
| 🟡 W8 | No PAT format validation | Low | Cryptic 401 errors | [ ] |
| 🟡 W9 | Manual date parsing | Low | Fragile, replace with kotlinx-datetime | [ ] |
| 🟡 W10 | String `contains` type detection | Low | Misparse risk | [ ] |
| 🟡 W11 | Wildcard imports | Low | Convention violation | [ ] |
| 🟡 W12 | PAT in serialized JSON | Low | Security hygiene | [ ] |

### Recommended Fix Order

1. **C1** — showstopper, Phase 2 features don't work without it
2. **C2** — one-line fix per file, high impact
3. **C6** — one-line fix, prevents navigation corruption
4. **C5** — small class rewrite, prevents duplicate logs
5. **C7** — small addition, prevents OOM
6. **C3** — medium refactor (suspend TokenStorage), prevents ANRs
7. **C4** — medium rewrite (reconnect logic), prevents silent failures
8. **C8** — medium addition (lifecycle hooks), battery + design compliance
9. **W4** — one annotation per class, recomposition perf
10. **W3** — small addition, security
11. **W9** — add dependency, delete 60 lines, robustness
12. Remaining warnings — as time permits

# 🚀 Openship-App — Complete App Flow & Features

> A visual walkthrough of how the app works from launch to every screen and feature.

---

## 📖 User Journey — Start to Finish

```
┌──────────────────────────────────────────────────────────────────┐
│                    🟢 1. LAUNCH APP                              │
│                    Splash / Welcome Screen                       │
└──────────────────────────┬───────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                    🔌 2. CONNECT INSTANCE                        │
│                    (First time or adding new server)             │
└──────────────────────────┬───────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                    📊 3. MAIN DASHBOARD                          │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │   [ ⊞ Projects ]                    [ 📈 Monitoring ]   │    │
│  │              Bottom Navigation Bar                        │    │
│  └──────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─────────────────────────┐    ┌────────────────────────────┐   │
│  │   Tab 1: Projects       │    │  Tab 2: Monitoring          │   │
│  │   (Slice 2)             │    │  (Slice 4)                  │   │
│  └───────────┬─────────────┘    └────────────┬───────────────┘   │
│              │                               │                   │
│              ▼                               ▼                   │
│     ┌───────────────────┐         ┌─────────────────────┐       │
│     │ Tap project card   │         │  Live telemetry     │       │
│     │ to view logs       │         │  stream starts      │       │
│     └─────────┬─────────┘         └─────────────────────┘       │
│              │                                                   │
│              ▼                                                   │
│     ┌───────────────────┐                                        │
│     │  Deploy Logs      │  (Slice 3)                             │
│     │  Live Terminal    │                                         │
│     └───────────────────┘                                        │
└──────────────────────────────────────────────────────────────────┘
```

---

## 🔌 Screen 1: Connect Instance

The first screen users see. Connects to any self-hosted Openship instance.

```
┌────────────────────────────────────────────────────────────────┐
│ ◯ OpenShip                                      [ ☀️/☾ ]      │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ 🔴 🟡 🟢                                                │  │
│  │                                                          │  │
│  │ Connect Server                                           │  │
│  │ Connect to your self-hosted Openship instance to         │  │
│  │ monitor deployments and servers in real time.            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                                                          │  │
│  │  INSTANCE URL                                            │  │
│  │  ┌────────────────────────────────────────────┐ ┌──────┐ │  │
│  │  │ http://192.168.1.112:4000                  │ │  🔄  │ │  │
│  │  └────────────────────────────────────────────┘ └──────┘ │  │
│  │                                                          │  │
│  │  [ 🏠 Wi-Fi (192.168.1.112:4000) ] [ 🔌 USB (localhost) ]│  │
│  │                                                          │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │ Openship v0.6.7 · Docker · none    [ 🟢 Online ]  │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  │                                                          │  │
│  │  SERVER LABEL                                            │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │ My Openship Server                                 │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  │                                                          │  │
│  │  PERSONAL ACCESS TOKEN (PAT)                             │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │ opsh_pat_•••••••••••••••••••••••••••••••••••••••  │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  │                                                          │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │              Connect Instance                      │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

### What happens behind the scenes:
```
User taps "Probe" or selects a preset
        │
        ▼
GET /api/health/env
        │
        ├── Returns: { authMode, version, deployMode, ... }
        │
        ├── If authMode == "none" → PAT field hidden (zero-auth)
        │
        └── Shows live discovery pill with version & status

User taps "Connect Instance"
        │
        ├── Validates PAT format (opsh_pat_<43 chars>)
        ├── Encrypts credentials with AES-256-GCM MasterKey
        ├── Stores in EncryptedSharedPreferences
        └── Navigates to Main Dashboard
```

### Features:
- **Two quick presets**: Wi-Fi LAN and USB/localhost — one tap fills URL + probes
- **Live probe**: Detects Openship version, auth mode, deploy mode
- **Smart PAT handling**: Shows/hides based on auth requirement
- **AES-256 encrypted storage**: Never stores credentials in plaintext
- **Multi-instance support**: Connect to multiple servers, switch anytime

---

## 📊 Screen 2: Main Dashboard — Projects Tab

The main screen after connecting. Lists all deployed projects with live status.

```
┌────────────────────────────────────────────────────────────────┐
│ ◯ OpenShip                    [ ☀️/☾ ]    [ 🟢 Localhost ▾ ]  │
├────────────────────────────────────────────────────────────────┤
│ Projects                                                       │
│ 1 deployed service                  [ 🖥️ Switcher ▾ ]         │
│                                                                │
│ [ All (1) ]  [ Active ]  [ Building ]                          │
│                                                                │
│ ┌────────────────────────────────────────────────────────────┐ │
│ │ 🔴 🟡 🟢                                          [ Ready ] │ │
│ │                                                            │ │
│ │ ┌──────┐  Personal-Finance-Tracker-API                     │ │
│ │ │  🍃  │  Springboot · Production                          │ │
│ │ └──────┘                                                   │ │
│ │                                                            │ │
│ │ [ ⑂ master · feat: initial spring boot api... ]            │ │
│ │ 🟢 live at localhost:20000                         Logs →  │ │
│ └────────────────────────────────────────────────────────────┘ │
├────────────────────────────────────────────────────────────────┤
│    [ ⊞ Projects ]                [ 📈 Monitoring ]             │
└────────────────────────────────────────────────────────────────┘
```

### What happens behind the scenes:
```
Screen loads / Pull-to-refresh triggered
        │
        ├── GET /api/projects/home  (parallel fetch)
        ├── GET /api/deployments    (parallel fetch)
        │
        ├── Merges into ProjectSummary domain model
        │
        └── Displays list with:
            ├── Framework squircle icon
            ├── Git branch pill
            ├── Domain/SSL badge
            ├── Pulsing status badge
            └── Tap → navigates to Deploy Logs
```

### Features:
- **Segmented filter control**: All / Active / Building
- **Real-time search**: Filter by name, repo, or framework
- **Pull-to-refresh**: Swipe down for latest data
- **Rich project cards**:
  - Framework icon: 🍃 Spring, 🐳 Docker, ▲ Next.js, ⚛️ React, 🐍 Python
  - Git branch pill with commit message
  - Domain badge with endpoint URL
  - Pulsing status badge (Ready / Building / Failed)
  - Mac-style window dots (🔴🟡🟢)
- **Instance switcher**: Dropdown to switch between connected servers

### Deployment Status Flow:
```
queued ──→ building ──→ deploying ──→ ready
                │
                ├──→ failed
                │
                └──→ cancelled

ready ──→ reconciling ──→ ready
```

---

## 💻 Screen 3: Live Developer Terminal (Deployment Logs)

The hero feature — real-time streaming build and deployment logs.

```
┌────────────────────────────────────────────────────────────────┐
│ ←  Personal-Finance-Tracker-API           [ ↓ ] [ 🟢 Live ]  │
│     deployment-id-12345                        [ ☀️/☾ ]       │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  ● Clone  →  ● Install  →  ● Build  →  ○ Deploy  →  ○ Ready  │
│                                                                │
│  ┌──────────────────────────────────┐ ┌──────────────────┐    │
│  │ 🔍 Search terminal logs...       │ │ 42 lines         │    │
│  └──────────────────────────────────┘ └──────────────────┘    │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ 🔴 🟡 🟢              terminal · container logs          │  │
│  │──────────────────────────────────────────────────────────│  │
│  │    1  $ npm install                                     │  │
│  │    2  added 342 packages in 12s                         │  │
│  │    3                                                    │  │
│  │    4  $ npm run build                                   │  │
│  │    5  > personal-finance-api@1.0.0 build               │  │
│  │    6  > tsc && node dist/index.js                       │  │
│  │    7  ✓ Build completed in 8.2s                        │  │
│  │    8                                                    │  │
│  │    9  $ docker build -t api:latest .                   │  │
│  │   10  Step 1/8 : FROM node:20-alpine                   │  │
│  │   11  ---> a1b2c3d4e5f6                                │  │
│  │   12  ✓ Container built and deployed                   │  │
│  │   ...                                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│                                          [ ⬇ ]  (scroll btn)  │
└────────────────────────────────────────────────────────────────┘
```

### What happens behind the scenes:
```
User taps project card
        │
        ▼
GET /api/deployments/:id/stream  (SSE connection)
        │
        ├── Receives events: log, progress, install-phase, service-status,
        │                     complete, cancelled, error, ping
        │
        ├── Each "log" event:
        │   ├── Data arrives base64-encoded
        │   ├── Decoded via Base64Decoder
        │   ├── ANSI color escapes parsed → AnnotatedString
        │   └── Displayed in monospace terminal
        │
        ├── "progress" / "install-phase" events:
        │   └── Update 5-stage stepper (Clone → Install → Build → Deploy → Ready)
        │
        └── SeqTracker tracks last eventId for resume:
            └── On reconnect: ?since=<seq> for replay
```

### Features:
- **SSE live stream**: Real-time log events via Server-Sent Events
- **Base64 decoding**: Log data decoded transparently
- **ANSI color parsing**: Full terminal colors → rich AnnotatedString
- **5-stage progress stepper**:
  ```
  Clone → Install → Build → Deploy → Ready
  ```
  Pulses during active streaming. Lights up green as each stage completes.
- **Monospace terminal UI**: Mac-style window with title bar, line numbers, dark background
- **Smart auto-scroll**: Toggle on/off. Floating "scroll to bottom" button when user scrolls up.
- **Keyword search**: Filter logs in real-time
- **Log line counter**: Shows filtered line count
- **Connection recovery**: Reconnects with `?since=<seq>` after backgrounding

### SSE Event Types:
| Event | Description |
|---|---|
| `log` | Base64-encoded log line (stdout/stderr) |
| `progress` | Build progress percentage |
| `install-phase` | Phase change (clone, install, build, deploy) |
| `service-status` | Service health update |
| `complete` | Build/deploy completed |
| `cancelled` | Deployment cancelled |
| `error` | Error occurred |
| `end` | Stream terminated |
| `ping` | Keep-alive |

---

## 📈 Screen 4: Real-Time Server Monitoring

Live host telemetry streamed every 3 seconds — for self-hosted instances.

```
┌────────────────────────────────────────────────────────────────┐
│ ◯ OpenShip                    [ ☀️/☾ ]    [ 🟢 Localhost ▾ ]  │
├────────────────────────────────────────────────────────────────┤
│ Monitoring                                                     │
│ Real-time host telemetry & metrics                   [ 🟢 Live ]│
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ 🔴 🟡 🟢           Uptime: 3d 14h                        │  │
│  │                                                          │  │
│  │ 🖥️  This Server                                          │  │
│  │ Deploy Mode: Docker · Host: localhost                     │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐           │
│  │              │ │              │ │              │           │
│  │    ╭───╮    │ │    ╭───╮    │ │    ╭───╮    │           │
│  │   │ 12%│    │ │   │51% │    │ │   │24% │    │           │
│  │    ╰───╯    │ │    ╰───╯    │ │    ╰───╯    │           │
│  │     CPU      │ │     RAM      │ │     Disk     │           │
│  │  12% load    │ │ 4.0/8.0 GB   │ │ 120/500 GB  │           │
│  └──────────────┘ └──────────────┘ └──────────────┘           │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ CPU Utilization Trend                          12%       │  │
│  │ ┌──────────────────────────────────────────────────┐     │  │
│  │ │    ╱╲    ╱╲   ╱╲    ╱╲   ╱╲    ╱╲   ╱╲  ╱╲    │     │  │
│  │ │ ╱╲╱  ╲╱╲╱  ╲╱  ╲╱╲╱  ╲╱  ╲╱╲╱  ╲╱  ╲╱  ╲╱╲  │     │  │
│  │ └──────────────────────────────────────────────────┘     │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Memory Utilization Trend              51% (4.0 GB)       │  │
│  │ ┌──────────────────────────────────────────────────┐     │  │
│  │ │ ───────────────────────────────────────────────  │     │  │
│  │ │ ╱╲  ╱╲  ╱╲  ╱╲  ╱╲  ╱╲  ╱╲  ╱╲  ╱╲  ╱╲  ╱╲  │     │  │
│  │ └──────────────────────────────────────────────────┘     │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ SYSTEM LOAD AVERAGES                                     │  │
│  │ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐      │  │
│  │ │    1 min     │ │    5 min     │ │   15 min     │      │  │
│  │ │     0.50     │ │     0.30     │ │     0.20     │      │  │
│  │ └──────────────┘ └──────────────┘ └──────────────┘      │  │
│  └──────────────────────────────────────────────────────────┘  │
├────────────────────────────────────────────────────────────────┤
│    [ ⊞ Projects ]                [ 📈 Monitoring ]             │
└────────────────────────────────────────────────────────────────┘
```

### What happens behind the scenes:
```
Screen loads
        │
        ├── GET /api/system/servers  →  list of servers
        │
        ├── Picks first server (or user-selected)
        │
        └── GET /api/system/monitor/stream?serverId=<id>  (SSE)
                │
                ├── Receives "stats" event every 3 seconds
                │
                ├── Parses telemetry:
                │   ├── cpu, memTotal, memUsed, memAvail
                │   ├── diskTotal, diskUsed, diskAvail
                │   ├── uptime, load1, load5, load15
                │   └── memory/disk in kilobytes → converted to GB
                │
                ├── Updates 3 circular gauges (animated)
                ├── Appends to rolling sparkline history (30 samples)
                └── Updates load averages & uptime display
```

### Features:
- **3 animated circular gauges**:
  - **CPU**: Percentage load with ring fill
  - **RAM**: Used / Total GB with ring fill
  - **Disk**: Used / Total GB with ring fill
- **Rolling sparkline trend graphs**:
  - CPU utilization trend (last 30 samples = ~90 seconds)
  - Memory utilization trend (last 30 samples)
  - Gradient-filled area charts with animated line drawing
- **System load averages**: 1-min / 5-min / 15-min in monospace pills
- **Uptime display**: Formatted as `Xd Yh` or `Xh Ym`
- **Server metadata card**: Server name, deploy mode, host URL
- **Cloud Mode detection**: Shows info card for cloud instances (monitoring is self-host only)
- **Error recovery**: Retry button on connection failure

### Telemetry Data Received:
```json
{
  "cpu": 12.5,
  "memTotal": 8192000,
  "memUsed": 4096000,
  "memAvail": 4096000,
  "diskTotal": 500000000,
  "diskUsed": 120000000,
  "diskAvail": 380000000,
  "uptime": 86400,
  "load1": 0.5,
  "load5": 0.3,
  "load15": 0.2
}
```

---

## 🔄 Navigation Flow

```
┌─────────────────────────────────────────────────────┐
│                     NAVIGATION                       │
│                                                     │
│   ConnectScreen ──(success)──▶ MainDashboardScreen   │
│                                      │               │
│                     ┌────────────────┤               │
│                     │                │               │
│                     ▼                ▼               │
│              ProjectsTab       MonitorTab            │
│                     │                                │
│                     ▼                                │
│              DeployLogsScreen ◀──(tap project)       │
│                     │                                │
│                     ▼                                │
│              (back) ──▶ ProjectsTab                  │
│                                                     │
│   InstanceSwitcherModal (bottom sheet)               │
│     ├── Switch to another instance                   │
│     ├── Triggers reconnect to new server             │
│     └── "Add Server" → back to ConnectScreen         │
└─────────────────────────────────────────────────────┘
```

---

## 🎨 Design System

### 1:1 Token Mapping (from Openship `theme.css`)

| Token | Dark Mode (`#000000`) | Light Mode (`#F9F9F9`) |
|---|---|---|
| Page Background | `#000000` | `#F9F9F9` |
| Card Background | `#0A0A0A` | `#FFFFFF` |
| Card Elevated | `#141414` | `#FFFFFF` |
| Pill Background | `#161616` | `#F3F4F6` |
| Terminal Background | `#050505` | `#0D1117` |
| Heading Text | `#FFFFFF` | `#000000` |
| Body Text | `rgba(255,255,255,0.66)` | `rgba(0,0,0,0.66)` |
| Muted Text | `rgba(255,255,255,0.50)` | `rgba(0,0,0,0.52)` |
| Card Border | `rgba(255,255,255,0.07)` | `#EAEAEA` |
| Subtle Border | `rgba(255,255,255,0.05)` | `#F0F0F0` |
| Button Primary BG | `#FFFFFF` (text: `#000000`) | `#000000` (text: `#FFFFFF`) |
| Brand Gradient | `#7C3AED → #3B82F6` | `#7C3AED → #3B82F6` |
| Mac Dots | 🔴 `#FF5F56` 🟡 `#FFBD2E` 🟢 `#27C93F` | Same |

---

## 🏗️ Architecture at a Glance

```
androidApp/ (Android only)          shared/ (KMP)
┌──────────────────────────┐        ┌──────────────────────────────────┐
│  Compose UI              │        │  commonMain/                     │
│  - Screens               │───────▶│  - client/ (REST, SSE, MCP)      │
│  - Navigation            │        │  - model/ (data classes)         │
│  - ViewModels            │        │  - viewmodel/                    │
│  - Koin DI               │        │  - ui/ (components, screens)     │
│  - Lifecycle             │        │  - util/ (parsers, decoders)     │
│                          │        │  - theme/ (Colors, Material3)    │
└──────────────────────────┘        │  - storage/ (Encrypted Prefs)    │
                                    │                                  │
                                    │  androidMain/                    │
                                    │  - platform/ (OkHttp engine)     │
                                    └──────────────────────────────────┘
```

### Data Flow per Screen:

| Screen | ViewModel | Repository | API Endpoint |
|---|---|---|---|
| Connect | `ConnectViewModel` | `DiscoveryService` | `GET /api/health/env` |
| Projects | `ProjectsViewModel` | `ProjectsRepository` | `GET /api/projects/home` |
| Deploy Logs | `DeployLogsViewModel` | `DeployLogsRepository` | `SSE /api/deployments/:id/stream` |
| Monitoring | `MonitorViewModel` | `MonitorRepository` | `SSE /api/system/monitor/stream` |

---

## 🛠️ Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.4.10 |
| UI Framework | Compose Multiplatform | 1.11.1 |
| Networking | Ktor Client | 3.1.1 |
| Serialization | kotlinx.serialization | 1.8.0 |
| SSE Engine | Ktor SSE + OkHttp | 4.12.0 |
| DI | Koin | 4.0.2 |
| Security | AndroidX Security Crypto | 1.1.0-alpha06 |
| Navigation | Navigation Compose | 2.8.0-alpha10 |
| Build | Gradle / AGP | 9.1.0 / 9.0.1 |
| Min SDK | Android | 24 (7.0) |
| Target SDK | Android | 36 |

---

## 📱 Build & Run

```bash
# Build & install (auto-configures port forwarding)
./gradlew :androidApp:installDebug

# Launch
adb shell am start -n com.kareemessam.openship/.MainActivity
```

> Port forwarding is automatic: `adb reverse tcp:4000 tcp:4000` and `tcp:20000 tcp:20000`.

---

## 🔮 Roadmap (v0.2.0+)

- [ ] One-tap Redeploy, Restart, Cancel actions
- [ ] Push notifications for deployment status
- [ ] Environment variables manager
- [ ] iOS target (`iosApp`)
- [ ] MCP SDK integration for AI-native tooling

# Openship-App Context & Architecture Handoff

This file is the single source of truth and comprehensive working handoff for the **Openship Mobile Client** (`Openship-App`). Update it whenever an architectural decision changes, a feature slice is implemented, or a milestone is reached.

---

## 📌 Executive Summary

- **App Name**: Openship-App
- **Package**: `com.kareemessam.openship`
- **Platform**: Android (Kotlin Multiplatform Compose architecture designed for future iOS expansion)
- **Status**: **Base Version v0.1.0 (Slices 1 to 4) 100% Completed & Verified on Device `SM-A556E`**
- **Last Updated**: 2026-08-21 23:55 (Local Time)

---

## 🎯 Product Scope & Status

### Base Version Scope (v0.1.0 - Read-Only Real-Time Client)
- [x] **Slice 1: Foundations & Instance Connect** (`/api/health/env`, Encrypted Storage, Multi-Instance management)
- [x] **Slice 2: Projects & Deployments Discovery** (`/api/projects`, `/api/deployments`, Instance Switcher, 1:1 Openship Project Cards)
- [x] **Slice 3: Live Deployment Build Logs & Developer Terminal** (`/api/deployments/:id/stream`, Base64 decoding, ANSI color parsing, 5-stage stepper, smart auto-scroll)
- [x] **Slice 4: Real-Time Server Monitoring** (`/api/system/monitor/stream`, 3-second SSE telemetry, CPU/RAM/Disk circular gauges, rolling gradient sparklines, load averages, uptime)
- [x] **Official Openship Design Language & Themes**: 1:1 token-level parity with web dashboard (`theme.css`) for both **Dark Mode** (`#000000`) and **Light Mode** (`#F9F9F9`) with instant in-app toggle.
- [x] **Custom Centered Logo & Adaptive Launcher Icons**: True extracted artwork (Open ring + Rocket + Cargo ship), mathematically centered at `(512, 512)` with background removed, exported across all density mipmaps and in-app Compose UI.
- [x] **Automated Port Forwarding & Direct Wi-Fi Connectivity**: Automatic Gradle `reverseAdbPorts` lifecycle task + direct LAN IP presets (`192.168.1.112:4000`) to eliminate manual port reversing forever.

### Future Scope (v0.2.0+)
- [ ] Write actions: One-tap Redeploy, Restart service, Cancel active deployment
- [ ] Push notifications for deployment failures/successes
- [ ] Environment variable manager (masked secrets viewing)
- [ ] iOS target release (`iosApp` KMP targets)

---

## 🧱 Architecture & Technology Stack

| Layer | Technology | Configuration & Details |
|---|---|---|
| **Language & Platform** | Kotlin 2.4.10 / JVM 21 | Kotlin Multiplatform (Common / Android) |
| **UI Framework** | Compose Multiplatform 1.11.1 | Jetpack Compose + Material 3 + Custom Canvas Graphics |
| **Networking & SSE** | Ktor Client 3.1.1 | ContentNegotiation, Kotlinx JSON 1.8.0, SSE plugin, OkHttp 4.12.0 engine |
| **Dependency Injection** | Koin 4.0.2 | `koin-core`, `koin-compose`, `koin-compose-viewmodel`, `koin-android` |
| **Storage & Security** | AndroidX Security Crypto 1.1.0-alpha06 | `EncryptedSharedPreferences` (AES-256-GCM / MasterKey) |
| **Navigation** | Navigation Compose 2.8.0-alpha10 | Bottom Navigation (Projects, Monitoring) + Backstack routing for Logs |
| **Build System** | Gradle 9.1.0 / AGP 9.0.1 | Android compileSdk/targetSdk 36, minSdk 24 |

---

## 🎨 1:1 Openship Design System Tokens

Tokens are mapped directly from Openship Web Dashboard [`openship/apps/dashboard/src/styles/theme.css`](file:///home/kareemessam_me/Desktop/Oblian/openship/apps/dashboard/src/styles/theme.css):

| Token | Dark Theme (`#000000`) | Light Theme (`#F9F9F9`) |
|---|---|---|
| `bgPage` | `#000000` (Pure Canvas Black) | `#F9F9F9` (Soft Off-White) |
| `bgCard` | `#0A0A0A` | `#FFFFFF` (Elevated White) |
| `bgCardElevated`| `#141414` | `#FFFFFF` |
| `bgPill` | `#161616` | `#F3F4F6` |
| `bgTerminal` | `#050505` (Deep Terminal) | `#0D1117` (Dev Navy) |
| `textHeading` | `#FFFFFF` | `#000000` |
| `textBody` | `rgba(255,255,255,0.66)` | `rgba(0,0,0,0.66)` |
| `textMuted` | `rgba(255,255,255,0.50)` | `rgba(0,0,0,0.52)` |
| `borderCard` | `rgba(255,255,255,0.07)` | `#EAEAEA` |
| `borderSubtle` | `rgba(255,255,255,0.05)` | `#F0F0F0` |
| `btnPrimaryBg` | `#FFFFFF` (Text: `#000000`) | `#000000` (Text: `#FFFFFF`) |
| `brandGradient` | `linear-gradient(135deg, #7C3AED, #3B82F6)` | `linear-gradient(135deg, #7C3AED, #3B82F6)` |
| `macControls` | Red `#FF5F56`, Yellow `#FFBD2E`, Green `#27C93F` | Red `#FF5F56`, Yellow `#FFBD2E`, Green `#27C93F` |

---

## 📂 Implementation File Map

```
Openship-App/
├── androidApp/
│   ├── build.gradle.kts                      # AGP configuration + auto reverseAdbPorts lifecycle task
│   └── src/main/
│       ├── AndroidManifest.xml               # Network permissions & @mipmap/ic_openship_launcher
│       └── res/
│           ├── drawable/
│           │   ├── app_logo.png              # Centered dark logo (512x512)
│           │   ├── app_logo_white.png        # Centered white logo (512x512)
│           │   ├── ic_launcher_background.xml# Dark adaptive background #0A0A0A
│           │   └── ic_openship_foreground.png# Adaptive foreground with 280px centered logo
│           ├── mipmap-*/                     # Generated mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi squircle & round icons
│           └── xml/network_security_config.xml# Cleartext HTTP permit for local dev (localhost, 192.168.*)
└── shared/
    └── src/commonMain/kotlin/com/kareemessam/openship/
        ├── App.kt                            # Root Composable & Navigation NavHost routing
        ├── shared/di/SharedModules.kt         # Koin DI module definitions
        ├── shared/storage/TokenStorage.kt     # Multi-instance EncryptedSharedPreferences contract
        ├── shared/client/
        │   ├── HttpClientFactory.kt          # Shared Ktor HTTP & SSE client with logging
        │   ├── DiscoveryService.kt           # Probes /api/health/env and normalizes URLs
        │   ├── ProjectsRepository.kt         # Queries /api/projects & /api/deployments
        │   ├── DeployLogsRepository.kt       # Live SSE streaming from /api/deployments/:id/stream
        │   └── MonitorRepository.kt          # Queries /api/system/servers & streams /api/system/monitor/stream
        ├── shared/model/
        │   ├── EnvDto.kt                     # Tolerant Openship environment model
        │   ├── InstanceConfig.kt             # Saved server instances
        │   ├── ProjectDto.kt                 # Project & Deployment DTOs + ProjectSummary domain model
        │   ├── MonitorDto.kt                 # Live telemetry & server host models
        │   └── sse/DeployStreamEvent.kt      # SSE polymorphic event serialization
        ├── shared/viewmodel/
        │   ├── ConnectViewModel.kt           # Instance discovery & PAT persistence
        │   ├── ProjectsViewModel.kt          # Filtering, instance switching, pull-to-refresh
        │   ├── DeployLogsViewModel.kt        # Terminal streaming, ANSI parsing, search, build stages
        │   └── MonitorViewModel.kt           # Live 3s telemetry, 30-sample rolling sparkline history
        ├── shared/util/
        │   ├── AnsiParser.kt                 # ANSI terminal color escape parser -> AnnotatedString
        │   ├── Base64Decoder.kt              # Standard Base64 stdout/stderr log line decoder
        │   └── SeqTracker.kt                 # Monotonic sequence tracker for log stream resumption
        └── shared/ui/
            ├── theme/
            │   ├── OpenshipColors.kt         # 1:1 Light & Dark token definitions
            │   └── OpenshipTheme.kt          # Material3 wrapper & LocalThemeMode switcher
            ├── components/
            │   ├── OpenshipBrandHeader.kt    # Brand TopBar with OpenShip logo image, Mac dots, theme toggle
            │   ├── ProjectCard.kt            # 1:1 Openship card with framework squircle, git branch, SSL badge
            │   ├── StatusBadge.kt            # Pulsing status pill badges (Ready, Building, Failed)
            │   ├── InstanceSwitcherDropdown.kt# Server dropdown switcher & Add Server modal
            │   └── MetricVisuals.kt          # CircularMetricGauge & Canvas SparklineTrendCard
            └── screens/
                ├── connect/ConnectScreen.kt  # Onboarding & Connection Form with Wi-Fi/USB presets
                ├── dashboard/MainDashboardScreen.kt # Bottom Navigation Bar (Projects & Monitoring)
                ├── projects/ProjectsScreen.kt# Search bar, segmented filter control, project list
                ├── logs/DeployLogsScreen.kt  # Monospace developer terminal with 5-stage stepper
                └── monitor/ServerMonitorScreen.kt # Live gauges, rolling sparklines, load averages
```

---

## 🔌 Networking & Local Server Bridge Notes

### Host Server Setup:
- Openship Dashboard / API: `http://localhost:4000` (or local Wi-Fi `http://192.168.1.112:4000`)
- Deployed Spring Boot Container (`openship-personal-finance-tracker-api-dep_v5Smbwvw3A7irNI_`):
  - Host Loopback Port: **`20000`** ➔ Container Port: **`8080`**
  - Health Check: `http://localhost:20000/actuator/health` returns `UP`.
- PostgreSQL Database on Host:
  - Port `5432` configured with `listen_addresses = '*'` in `/etc/postgresql/16/main/postgresql.conf`.
  - `/etc/postgresql/16/main/pg_hba.conf` permits Docker bridge `172.17.0.0/16` and host IP `192.168.1.112`.

### Android Port Forwarding:
1. **Automated Gradle Hook**: `./gradlew installDebug` automatically runs `adb reverse tcp:4000 tcp:4000` and `adb reverse tcp:20000 tcp:20000`.
2. **Wi-Fi Connectivity**: Phone can connect directly to `http://192.168.1.112:4000` over Wi-Fi without needing USB or ADB reverse.

---

## 🧪 Verification & Testing State

- **Unit & Compilation Tests**: `./gradlew test` executes 40 tasks cleanly with 0 compilation errors.
- **Physical Device Tests**: Verified on physical device **Samsung Galaxy A55 5G (`SM-A556E`)** running Android 14/15.
  - Multi-instance connection probe: **PASSED**
  - Project listing & framework squircle rendering: **PASSED**
  - Live deployment log playback with ANSI formatting: **PASSED**
  - Real-time 3-second server telemetry stream & circular gauges: **PASSED**
  - Dark/Light Theme dynamic switching: **PASSED**
  - Custom launcher adaptive icon & in-app branding: **PASSED**

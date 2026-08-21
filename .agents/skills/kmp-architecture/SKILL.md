---
name: kmp-architecture
description: Guidelines and best practices for Kotlin Multiplatform (KMP) architecture, source set hierarchies (commonMain, androidMain, iosMain), expect/actual declarations, and platform isolation. Use when adding multiplatform abstractions, platform-specific engines, or organizing shared business logic.
---

# Kotlin Multiplatform (KMP) Architecture Skill

Guides multiplatform layering, expect/actual patterns, and clean separation between shared logic and platform integrations.

## Architecture Guidelines

1. **Keep commonMain Platform-Agnostic**:
   - Place all domain models, repositories, API clients (MCP, SSE, HTTP), and ViewModels in `shared/src/commonMain/kotlin`.
   - Never import Android framework packages (`android.*`, `androidx.*` specific to Android) into `commonMain`.

2. **Expect / Actual Pattern**:
   - Use `expect` in `commonMain` for platform abstractions (e.g. `expect class TokenStorage`, `expect fun createHttpClientEngine()`).
   - Implement `actual` in `androidMain` (and later `iosMain`) using platform-native APIs (e.g., `EncryptedSharedPreferences` on Android, Keychain on iOS).

3. **Dependency Injection Boundary**:
   - Prefer passing platform implementations into shared modules via Koin modules rather than overusing `expect`/`actual` when standard interfaces suffice.

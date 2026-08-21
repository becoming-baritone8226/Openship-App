---
name: requesting-code-review
description: Standardize self-review and peer code review workflows. Use before submitting changes, creating pull requests, or declaring a milestone complete.
---

# Code Review Superpower

Ensures changes meet architectural, security, performance, and formatting standards.

## Code Review Checklist

1. **Security & Secrets**:
   - [ ] No hardcoded API keys, tokens, or credentials in code or logs.
   - [ ] Sensitive tokens are stored using secure platform mechanisms (e.g., Keystore / EncryptedSharedPreferences).
   - [ ] Network traffic is secured, and cleartext is strictly bounded to internal/development IP ranges.

2. **Architecture & Clean Code**:
   - [ ] Pure business logic is preserved in `commonMain` without platform imports.
   - [ ] State is hoisted in Composable functions; side effects are managed with `LaunchedEffect` or `DisposableEffect`.
   - [ ] Coroutines and streams handle cancellation cooperatively.

3. **Resilience & Error Handling**:
   - [ ] API JSON parsing is tolerant of unexpected/additive fields (`ignoreUnknownKeys = true`).
   - [ ] Network errors, disconnection, and reconnection scenarios are handled gracefully in UI state.

4. **Testing**:
   - [ ] Unit tests cover new data transformations, parsers, and view state flows.

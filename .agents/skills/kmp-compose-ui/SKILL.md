---
name: kmp-compose-ui
description: Compose Multiplatform UI patterns, state hoisting, Material 3 theming, Navigation Compose routing, responsive screen layouts, and lifecycle-aware coroutine side effects. Use when building or updating Compose screens and UI components.
---

# Compose Multiplatform UI Skill

Patterns for building responsive, accessible, and reactive Compose Multiplatform user interfaces.

## Best Practices

1. **State Hoisting**:
   - Keep screen and widget composables stateless by passing state down and events up.
   - Screen wrappers collect state from ViewModels using `collectAsStateWithLifecycle()`.

2. **Side Effect Handling**:
   - `LaunchedEffect(key)` for starting streams or one-shot coroutines tied to a key.
   - `DisposableEffect` for registering/unregistering callbacks and cleaning up active listeners.

3. **Material 3 Theming & Responsive Layouts**:
   - Use `Scaffold`, `TopAppBar`, `NavigationBar`, `Card`, and `LazyColumn`.
   - Support dark/light modes and dynamic colors.

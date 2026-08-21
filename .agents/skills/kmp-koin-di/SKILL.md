---
name: kmp-koin-di
description: Multiplatform dependency injection using Koin 4.x. Use when setting up or modifying modules for networking clients, repositories, platform dependencies, and Compose ViewModels.
---

# KMP Koin Dependency Injection Skill

Clean, multiplatform dependency injection without reflection or annotation processing.

## Module Structure

```kotlin
// Common networking module
val clientModule = module {
    single { HttpClientFactory.create(get()) }
    single { McpClient(get(), get()) }
    single { SseClient(get(), get()) }
}

// Common repository module
val repositoryModule = module {
    single<OpenshipRepository> { OpenshipRepositoryImpl(get(), get(), get()) }
}

// Common ViewModels
val viewModelModule = module {
    viewModel { ConnectViewModel(get()) }
    viewModel { ProjectsViewModel(get()) }
    viewModel { DeployLogsViewModel(get()) }
    viewModel { MonitorViewModel(get()) }
}
```

## Injection in Compose
```kotlin
@Composable
fun ProjectsScreen(viewModel: ProjectsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // UI rendering
}
```

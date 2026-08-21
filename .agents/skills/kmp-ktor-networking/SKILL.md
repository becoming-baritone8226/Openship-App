---
name: kmp-ktor-networking
description: Best practices for configuring Ktor Client 3.x in KMP projects, sharing HttpClient instances, installing SSE and ContentNegotiation plugins, handling connection lifecycles, and managing multiplatform engines (OkHttp/Darwin). Use when implementing HTTP, SSE, or MCP transports.
---

# KMP Ktor Networking Skill

Best practices for Ktor Client in Kotlin Multiplatform applications.

## Key Patterns

1. **Single Shared HttpClient**:
   - Share one configured `HttpClient` instance between REST, MCP `StreamableHttpClientTransport`, and SSE streams.
   - Configure timeouts, connection pools, and logging once in `HttpClientFactory`.

2. **Ktor SSE Plugin**:
   - Install `SSE` plugin:
     ```kotlin
     val client = HttpClient(engine) {
         install(SSE) {
             showCommentEvents()
             showRetryEvents()
         }
         install(ContentNegotiation) {
             json(Json {
                 ignoreUnknownKeys = true
                 isLenient = true
                 encodeDefaults = true
             })
         }
     }
     ```
   - Collect streams with coroutines Flow:
     ```kotlin
     client.sse(urlString = "$baseUrl/api/stream", request = {
         header(HttpHeaders.Authorization, "Bearer $pat")
         header(HttpHeaders.Accept, "text/event-stream")
     }) {
         incoming.collect { event ->
             // parse event.event, event.data, event.id
         }
     }
     ```

3. **Engine Selection**:
   - Android: `OkHttp` engine factory.
   - iOS (future): `Darwin` engine factory.

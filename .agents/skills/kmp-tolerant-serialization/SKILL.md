---
name: kmp-tolerant-serialization
description: Guidelines for resilient JSON serialization using kotlinx.serialization with unversioned REST APIs and polymorphic SSE events. Use when defining API data models or parsing server payloads.
---

# Tolerant JSON Serialization Skill

Rules for parsing responses and SSE event payloads from unversioned APIs.

## Core Rules

1. **Configuring Json**:
   ```kotlin
   val json = Json {
       ignoreUnknownKeys = true  // Never fail on new server fields
       isLenient = true
       encodeDefaults = true
       coerceInputValues = true
   }
   ```

2. **Nullable Fields with Defaults**:
   ```kotlin
   @Serializable
   data class ProjectRow(
       val id: String,
       val name: String,
       val gitRepo: String? = null,
       val framework: String? = null
   )
   ```

3. **Sealed Class Polymorphism for SSE Events**:
   Use `@SerialName` discriminators or event-type router matching.

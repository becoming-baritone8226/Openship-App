package com.kareemessam.openship.shared.client

import com.kareemessam.openship.shared.model.InstanceConfig
import com.kareemessam.openship.shared.model.MonitorStatsDto
import com.kareemessam.openship.shared.model.ServerItemDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

import io.ktor.client.plugins.timeout

class MonitorRepository(
    private val httpClient: HttpClient,
    private val discoveryService: DiscoveryService,
    private val json: Json = HttpClientFactory.tolerantJson
) {

    suspend fun getServers(instance: InstanceConfig): Result<List<ServerItemDto>> = runCatching {
        val baseUrl = discoveryService.normalizeUrl(instance.url)
        val url = "$baseUrl/api/system/servers"

        val response: List<ServerItemDto> = httpClient.get(url) {
            if (instance.pat.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer ${instance.pat}")
            }
        }.body()

        response
    }

    fun streamServerStats(instance: InstanceConfig, serverId: String): Flow<MonitorStatsDto> = flow {
        val baseUrl = discoveryService.normalizeUrl(instance.url)
        val url = "$baseUrl/api/system/monitor/stream?serverId=$serverId"

        try {
            httpClient.sse(
                urlString = url,
                request = {
                    timeout {
                        socketTimeoutMillis = Long.MAX_VALUE
                        requestTimeoutMillis = Long.MAX_VALUE
                    }
                    if (instance.pat.isNotBlank()) {
                        header(HttpHeaders.Authorization, "Bearer ${instance.pat}")
                    }
                    header(HttpHeaders.Accept, "text/event-stream")
                }
            ) {
                incoming.collect { event ->
                    val data = event.data ?: ""
                    if (data.isNotBlank() && data.contains("\"cpu\"")) {
                        try {
                            val stats = json.decodeFromString<MonitorStatsDto>(data)
                            emit(stats)
                        } catch (e: Exception) {
                            // Non-fatal parse issue
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Stream ended or connection issue
        }
    }
}

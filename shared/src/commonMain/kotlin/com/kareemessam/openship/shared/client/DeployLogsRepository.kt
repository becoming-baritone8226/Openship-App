package com.kareemessam.openship.shared.client

import com.kareemessam.openship.shared.model.InstanceConfig
import com.kareemessam.openship.shared.model.sse.DeployStreamEvent
import com.kareemessam.openship.shared.util.Base64Decoder
import com.kareemessam.openship.shared.util.SeqTracker
import io.ktor.client.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class DecodedLogEntry(
    val eventId: Long,
    val text: String,
    val step: String?,
    val stepStatus: String?,
    val level: String?,
    val serviceName: String?
)

class DeployLogsRepository(
    private val httpClient: HttpClient,
    private val discoveryService: DiscoveryService,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) {

    fun streamDeployLogs(
        instance: InstanceConfig,
        deploymentId: String,
        seqTracker: SeqTracker
    ): Flow<DeployStreamEvent> = flow {
        val baseUrl = discoveryService.normalizeUrl(instance.url)
        val resumeSeq = seqTracker.getResumeParam()
        val url = "$baseUrl/api/deployments/$deploymentId/stream?since=$resumeSeq"

        try {
            httpClient.sse(
                urlString = url,
                request = {
                    if (instance.pat.isNotBlank()) {
                        header(HttpHeaders.Authorization, "Bearer ${instance.pat}")
                    }
                    header(HttpHeaders.Accept, "text/event-stream")
                }
            ) {
                incoming.collect { serverSentEvent ->
                    val eventType = serverSentEvent.event
                    val data = serverSentEvent.data ?: ""

                    if (data.isBlank() && eventType == "ping") {
                        emit(DeployStreamEvent.Ping)
                        return@collect
                    }

                    val event = try {
                        when (eventType) {
                            "progress" -> json.decodeFromString<DeployStreamEvent.Progress>(data)
                            "service-status" -> json.decodeFromString<DeployStreamEvent.ServiceStatus>(data)
                            "complete" -> json.decodeFromString<DeployStreamEvent.Complete>(data)
                            "cancelled" -> json.decodeFromString<DeployStreamEvent.Cancelled>(data)
                            "end" -> json.decodeFromString<DeployStreamEvent.End>(data)
                            "error" -> json.decodeFromString<DeployStreamEvent.Error>(data)
                            "ping" -> DeployStreamEvent.Ping
                            else -> {
                                // Default or "log" event
                                if (data.contains("\"type\":\"log\"") || data.contains("\"data\":")) {
                                    json.decodeFromString<DeployStreamEvent.Log>(data)
                                } else if (data.contains("\"type\":\"progress\"")) {
                                    json.decodeFromString<DeployStreamEvent.Progress>(data)
                                } else if (data.contains("\"type\":\"service-status\"")) {
                                    json.decodeFromString<DeployStreamEvent.ServiceStatus>(data)
                                } else {
                                    DeployStreamEvent.Log(data = data)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        DeployStreamEvent.Log(data = data)
                    }

                    if (event is DeployStreamEvent.Log && event.eventId > 0) {
                        seqTracker.update(event.eventId)
                    }

                    emit(event)
                }
            }
        } catch (e: Exception) {
            emit(DeployStreamEvent.Error(error = e.message ?: "Failed to connect to log stream"))
        }
    }
}

package com.kareemessam.openship.shared.client

import com.kareemessam.openship.shared.model.DeploymentDto
import com.kareemessam.openship.shared.model.DeploymentsApiResponse
import com.kareemessam.openship.shared.model.InstanceConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders

/**
 * Fetches a project's deployment history via the REST endpoint (Phase 1 reads).
 * The MCP tool `get_deployments` is reserved for write operations in Phase 2.
 *
 * ponytail: `open` so host tests can substitute a fake without a live server.
 */
open class DeploymentsRepository(
    private val httpClient: HttpClient,
    private val discoveryService: DiscoveryService
) {

    open suspend fun getDeploymentHistory(
        instance: InstanceConfig,
        projectId: String
    ): Result<List<DeploymentDto>> = runCatching {
        val baseUrl = discoveryService.normalizeUrl(instance.url)
        val response: DeploymentsApiResponse = httpClient.get("$baseUrl/api/deployments") {
            parameter("projectId", projectId)
            parameter("perPage", 100)
            if (instance.pat.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer ${instance.pat}")
            }
        }.body()

        sortNewestFirst(response.data)
    }
}

// ponytail: ISO-8601 strings sort lexicographically only for identical offsets;
// the server emits UTC ("Z") so string sort is correct. Switch to epoch parsing
// if mixed-offset timestamps ever appear.
internal fun sortNewestFirst(deployments: List<DeploymentDto>): List<DeploymentDto> =
    deployments.sortedByDescending { it.createdAt ?: "" }

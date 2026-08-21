package com.kareemessam.openship.shared.client

import com.kareemessam.openship.shared.model.*
import com.kareemessam.openship.shared.storage.TokenStorage
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class ProjectsRepository(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage,
    private val discoveryService: DiscoveryService
) {

    suspend fun getProjects(instance: InstanceConfig): Result<List<ProjectSummary>> = runCatching {
        val baseUrl = discoveryService.normalizeUrl(instance.url)
        val projectsUrl = "$baseUrl/api/projects"

        val projectsResponse: ProjectsApiResponse = httpClient.get(projectsUrl) {
            if (instance.pat.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer ${instance.pat}")
            }
        }.body()

        val projectSummaries = projectsResponse.data.map { project ->
            var latestStatus = ProjectStatus.UNKNOWN
            var statusString = "Unknown"
            var commitMsg: String? = null
            var commitSha: String? = null

            // If active deployment is present, fetch deployment details
            if (!project.activeDeploymentId.isNullOrBlank()) {
                try {
                    val depUrl = "$baseUrl/api/deployments"
                    val deploymentsResponse: DeploymentsApiResponse = httpClient.get(depUrl) {
                        parameter("projectId", project.id)
                        if (instance.pat.isNotBlank()) {
                            header(HttpHeaders.Authorization, "Bearer ${instance.pat}")
                        }
                    }.body()

                    val activeDep = deploymentsResponse.data.firstOrNull { it.id == project.activeDeploymentId }
                        ?: deploymentsResponse.data.firstOrNull()

                    if (activeDep != null) {
                        statusString = activeDep.status ?: "Ready"
                        latestStatus = when (activeDep.status?.lowercase()) {
                            "ready", "active", "healthy", "up" -> ProjectStatus.READY
                            "building", "deploying", "queued" -> ProjectStatus.BUILDING
                            "failed", "error", "crashed" -> ProjectStatus.FAILED
                            "stopped", "paused" -> ProjectStatus.STOPPED
                            else -> ProjectStatus.READY
                        }
                        commitMsg = activeDep.commitMessage
                        commitSha = activeDep.commitSha?.take(7)
                    }
                } catch (e: Exception) {
                    // Fallback to active if deployment query fails
                    latestStatus = ProjectStatus.READY
                    statusString = "Active"
                }
            }

            ProjectSummary(
                id = project.id,
                name = project.name,
                slug = project.slug ?: project.name.lowercase().replace(" ", "-"),
                framework = project.framework ?: "custom",
                gitRepo = if (project.gitOwner != null && project.gitRepo != null) {
                    "${project.gitOwner}/${project.gitRepo}"
                } else project.gitRepo,
                gitBranch = project.gitBranch ?: "main",
                port = project.port ?: 8080,
                hostPort = project.hostPort ?: project.port,
                status = latestStatus,
                statusText = statusString.replaceFirstChar { it.uppercase() },
                activeDeploymentId = project.activeDeploymentId,
                commitMessage = commitMsg,
                commitShaShort = commitSha,
                updatedAt = project.updatedAt ?: project.createdAt
            )
        }

        projectSummaries
    }
}

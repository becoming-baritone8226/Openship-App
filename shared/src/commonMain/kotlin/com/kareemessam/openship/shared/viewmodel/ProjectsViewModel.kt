package com.kareemessam.openship.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kareemessam.openship.shared.client.ProjectsRepository
import com.kareemessam.openship.shared.model.InstanceConfig
import com.kareemessam.openship.shared.model.ProjectSummary
import com.kareemessam.openship.shared.storage.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectsUiState(
    val activeInstance: InstanceConfig? = null,
    val allInstances: List<InstanceConfig> = emptyList(),
    val projects: List<ProjectSummary> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
) {
    val filteredProjects: List<ProjectSummary>
        get() = if (searchQuery.isBlank()) {
            projects
        } else {
            projects.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.framework.contains(searchQuery, ignoreCase = true) ||
                (it.gitRepo?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
}

class ProjectsViewModel(
    private val projectsRepository: ProjectsRepository,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectsUiState())
    val state: StateFlow<ProjectsUiState> = _state.asStateFlow()

    init {
        loadInstancesAndProjects()
    }

    fun loadInstancesAndProjects() {
        val instances = tokenStorage.loadInstances()
        val active = tokenStorage.getActiveInstance() ?: instances.firstOrNull()

        _state.update {
            it.copy(
                activeInstance = active,
                allInstances = instances
            )
        }

        if (active != null) {
            fetchProjects(active, isRefresh = false)
        } else {
            _state.update { it.copy(isLoading = false, projects = emptyList()) }
        }
    }

    fun refresh() {
        val active = _state.value.activeInstance ?: return
        fetchProjects(active, isRefresh = true)
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun switchInstance(instanceId: String) {
        tokenStorage.setActiveInstance(instanceId)
        loadInstancesAndProjects()
    }

    fun deleteInstance(instanceId: String) {
        tokenStorage.deleteInstance(instanceId)
        loadInstancesAndProjects()
    }

    private fun fetchProjects(instance: InstanceConfig, isRefresh: Boolean) {
        viewModelScope.launch {
            _state.update {
                if (isRefresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoading = true, error = null)
            }

            val result = projectsRepository.getProjects(instance)
            result.onSuccess { list ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        projects = list,
                        error = null
                    )
                }
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = err.message ?: "Failed to load projects."
                    )
                }
            }
        }
    }
}

package com.kareemessam.openship.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kareemessam.openship.shared.client.MonitorRepository
import com.kareemessam.openship.shared.model.InstanceConfig
import com.kareemessam.openship.shared.model.MonitorStatsDto
import com.kareemessam.openship.shared.model.ServerItemDto
import com.kareemessam.openship.shared.storage.TokenStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MonitorUiState(
    val activeInstance: InstanceConfig? = null,
    val activeServer: ServerItemDto? = null,
    val allServers: List<ServerItemDto> = emptyList(),
    val currentStats: MonitorStatsDto? = null,
    val cpuHistory: List<Float> = emptyList(),
    val memHistory: List<Float> = emptyList(),
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val error: String? = null
)

class MonitorViewModel(
    private val monitorRepository: MonitorRepository,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _state = MutableStateFlow(MonitorUiState())
    val state: StateFlow<MonitorUiState> = _state.asStateFlow()

    private var streamJob: Job? = null
    private val maxHistorySize = 30 // ~90 seconds of history

    init {
        loadServersAndStartMonitoring()
    }

    fun loadServersAndStartMonitoring() {
        val activeInstance = tokenStorage.getActiveInstance() ?: return

        _state.update { it.copy(activeInstance = activeInstance, isLoading = true, error = null) }

        viewModelScope.launch {
            val result = monitorRepository.getServers(activeInstance)
            result.onSuccess { servers ->
                val primaryServer = servers.firstOrNull { it.isLocal == true } ?: servers.firstOrNull()
                _state.update {
                    it.copy(
                        isLoading = false,
                        allServers = servers,
                        activeServer = primaryServer
                    )
                }
                if (primaryServer != null) {
                    startStreaming(activeInstance, primaryServer.id)
                }
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = err.message ?: "Failed to load servers."
                    )
                }
            }
        }
    }

    fun selectServer(serverId: String) {
        val activeInstance = _state.value.activeInstance ?: return
        val server = _state.value.allServers.firstOrNull { it.id == serverId } ?: return
        _state.update { it.copy(activeServer = server, cpuHistory = emptyList(), memHistory = emptyList()) }
        startStreaming(activeInstance, serverId)
    }

    private fun startStreaming(instance: InstanceConfig, serverId: String) {
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            _state.update { it.copy(isStreaming = true, error = null) }

            monitorRepository.streamServerStats(instance, serverId)
                .collect { stats ->
                    val cpuVal = (stats.cpu?.toFloat() ?: 0f).coerceIn(0f, 100f)
                    val memVal = stats.memPercentage

                    _state.update { current ->
                        val updatedCpuHistory = (current.cpuHistory + cpuVal).takeLast(maxHistorySize)
                        val updatedMemHistory = (current.memHistory + memVal).takeLast(maxHistorySize)

                        current.copy(
                            currentStats = stats,
                            cpuHistory = updatedCpuHistory,
                            memHistory = updatedMemHistory,
                            isStreaming = true
                        )
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
    }
}

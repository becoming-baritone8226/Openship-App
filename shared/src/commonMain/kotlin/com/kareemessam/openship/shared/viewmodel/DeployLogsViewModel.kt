package com.kareemessam.openship.shared.viewmodel

import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kareemessam.openship.shared.client.DeployLogsRepository
import com.kareemessam.openship.shared.model.sse.DeployStreamEvent
import com.kareemessam.openship.shared.storage.TokenStorage
import com.kareemessam.openship.shared.util.AnsiParser
import com.kareemessam.openship.shared.util.decodeBase64ToString
import com.kareemessam.openship.shared.util.SeqTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LogItem(
    val id: Long,
    val rawText: String,
    val parsedText: AnnotatedString,
    val step: String?,
    val level: String?,
    val serviceName: String?
)

enum class BuildStage {
    CLONE,
    INSTALL,
    BUILD,
    DEPLOY,
    READY
}

data class DeployLogsUiState(
    val projectName: String = "",
    val deploymentId: String = "",
    val isStreaming: Boolean = false,
    val logs: List<LogItem> = emptyList(),
    val searchQuery: String = "",
    val autoScroll: Boolean = true,
    val currentStage: BuildStage = BuildStage.CLONE,
    val finalStatus: String? = null,
    val error: String? = null
) {
    val filteredLogs: List<LogItem>
        get() = if (searchQuery.isBlank()) {
            logs
        } else {
            logs.filter {
                it.rawText.contains(searchQuery, ignoreCase = true) ||
                (it.serviceName?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
}

class DeployLogsViewModel(
    private val deployLogsRepository: DeployLogsRepository,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _state = MutableStateFlow(DeployLogsUiState())
    val state: StateFlow<DeployLogsUiState> = _state.asStateFlow()

    private val seqTracker = SeqTracker()
    private var streamJob: Job? = null
    private var logCounter = 0L

    fun initDeployment(projectName: String, deploymentId: String) {
        _state.update {
            it.copy(
                projectName = projectName,
                deploymentId = deploymentId,
                logs = emptyList(),
                error = null
            )
        }
        seqTracker.reset()
        startStream(deploymentId)
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun setAutoScroll(enabled: Boolean) {
        _state.update { it.copy(autoScroll = enabled) }
    }

    fun retry() {
        val currentDep = _state.value.deploymentId
        if (currentDep.isNotBlank()) {
            startStream(currentDep)
        }
    }

    private fun startStream(deploymentId: String) {
        val activeInstance = tokenStorage.getActiveInstance() ?: return

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            _state.update { it.copy(isStreaming = true, error = null) }

            deployLogsRepository.streamDeployLogs(activeInstance, deploymentId, seqTracker)
                .collect { event ->
                    when (event) {
                        is DeployStreamEvent.Log -> {
                            val decoded = decodeBase64ToString(event.data)
                            val lines = decoded.split("\n")
                            val newItems = lines.filter { it.isNotEmpty() }.map { line ->
                                logCounter++
                                LogItem(
                                    id = logCounter,
                                    rawText = line,
                                    parsedText = AnsiParser.parse(line),
                                    step = event.step,
                                    level = event.level,
                                    serviceName = event.serviceName
                                )
                            }

                            val stage = determineStage(event.step)
                            _state.update { current ->
                                current.copy(
                                    logs = current.logs + newItems,
                                    currentStage = stage ?: current.currentStage
                                )
                            }
                        }
                        is DeployStreamEvent.Progress -> {
                            val stage = determineStage(event.step)
                            if (stage != null) {
                                _state.update { it.copy(currentStage = stage) }
                            }
                        }
                        is DeployStreamEvent.ServiceStatus -> {
                            // If service is running or built, advance stage
                            if (event.status?.lowercase() == "running") {
                                _state.update { it.copy(currentStage = BuildStage.READY) }
                            }
                        }
                        is DeployStreamEvent.Complete -> {
                            _state.update {
                                it.copy(
                                    currentStage = BuildStage.READY,
                                    finalStatus = event.status ?: "ready",
                                    isStreaming = false
                                )
                            }
                        }
                        is DeployStreamEvent.Cancelled -> {
                            _state.update {
                                it.copy(
                                    finalStatus = "cancelled",
                                    isStreaming = false
                                )
                            }
                        }
                        is DeployStreamEvent.End -> {
                            _state.update {
                                it.copy(
                                    finalStatus = event.status ?: it.finalStatus ?: "finished",
                                    isStreaming = false
                                )
                            }
                        }
                        is DeployStreamEvent.Error -> {
                            _state.update {
                                it.copy(
                                    error = event.error ?: event.message ?: "Stream error",
                                    isStreaming = false
                                )
                            }
                        }
                        DeployStreamEvent.Ping, is DeployStreamEvent.Unknown -> {
                            // Keep alive
                        }
                    }
                }
        }
    }

    private fun determineStage(step: String?): BuildStage? {
        val s = step?.lowercase() ?: return null
        return when {
            s.contains("clone") || s.contains("fetch") -> BuildStage.CLONE
            s.contains("install") || s.contains("dep") -> BuildStage.INSTALL
            s.contains("build") || s.contains("compile") || s.contains("package") -> BuildStage.BUILD
            s.contains("deploy") || s.contains("route") || s.contains("container") -> BuildStage.DEPLOY
            s.contains("ready") || s.contains("done") || s.contains("health") -> BuildStage.READY
            else -> null
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
    }
}

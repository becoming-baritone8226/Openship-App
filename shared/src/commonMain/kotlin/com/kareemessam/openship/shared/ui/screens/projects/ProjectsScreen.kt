package com.kareemessam.openship.shared.ui.screens.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kareemessam.openship.shared.model.ProjectStatus
import com.kareemessam.openship.shared.model.ProjectSummary
import com.kareemessam.openship.shared.ui.components.OpenshipTopBar
import com.kareemessam.openship.shared.ui.components.ProjectCard
import com.kareemessam.openship.shared.ui.theme.OpenshipAppTheme
import com.kareemessam.openship.shared.viewmodel.ProjectsUiState

import com.kareemessam.openship.shared.ui.components.InstanceSwitcherModal

enum class ProjectFilterTab {
    ALL,
    ACTIVE,
    BUILDING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    state: ProjectsUiState,
    onRefresh: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onInstanceSelected: (String) -> Unit,
    onAddInstanceClicked: () -> Unit,
    onProjectClicked: (ProjectSummary) -> Unit,
    onRedeployClick: (ProjectSummary) -> Unit,
    onRedeployConfirm: () -> Unit,
    onRedeployCancel: () -> Unit,
    onRedeployResultConsumed: () -> Unit,
    onOpenLogs: (ProjectSummary, String) -> Unit,
    onHistoryClick: (ProjectSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OpenshipAppTheme.colors
    var selectedFilter by remember { mutableStateOf(ProjectFilterTab.ALL) }
    var isInstanceModalOpen by remember { mutableStateOf(false) }

    // Navigate to the live logs once a redeploy returns a deployment id.
    LaunchedEffect(state.redeployResultDeploymentId) {
        val deploymentId = state.redeployResultDeploymentId
        val project = state.redeployTarget
        if (deploymentId != null && project != null) {
            onOpenLogs(project, deploymentId)
            onRedeployResultConsumed()
        }
    }

    val displayedProjects = remember(state.projects, state.searchQuery, selectedFilter) {
        val base = if (state.searchQuery.isBlank()) {
            state.projects
        } else {
            state.projects.filter {
                it.name.contains(state.searchQuery, ignoreCase = true) ||
                it.framework.contains(state.searchQuery, ignoreCase = true) ||
                (it.gitRepo?.contains(state.searchQuery, ignoreCase = true) == true)
            }
        }
        when (selectedFilter) {
            ProjectFilterTab.ALL -> base
            ProjectFilterTab.ACTIVE -> base.filter { it.status == ProjectStatus.READY }
            ProjectFilterTab.BUILDING -> base.filter { it.status == ProjectStatus.BUILDING || it.status == ProjectStatus.QUEUED }
        }
    }

    InstanceSwitcherModal(
        isOpen = isInstanceModalOpen,
        activeInstance = state.activeInstance,
        allInstances = state.allInstances,
        onDismiss = { isInstanceModalOpen = false },
        onInstanceSelected = onInstanceSelected,
        onAddInstanceClicked = onAddInstanceClicked
    )

    if (state.redeployTarget != null && state.redeployResultDeploymentId == null) {
        RedeployConfirmDialog(
            projectName = state.redeployTarget.name,
            isLoading = state.redeployLoading,
            error = state.redeployError,
            onConfirm = onRedeployConfirm,
            onCancel = onRedeployCancel
        )
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(colors.bgPage)) {
                OpenshipTopBar(
                    instanceLabel = state.activeInstance?.label,
                    onSwitchInstance = { isInstanceModalOpen = true }
                )
            }
        },
        containerColor = colors.bgPage,
        modifier = modifier
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section Header
                Column {
                    Text(
                        text = "Projects",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = colors.textHeading,
                        letterSpacing = (-0.4).sp
                    )
                    Text(
                        text = "${state.projects.size} deployed service${if (state.projects.size == 1) "" else "s"}",
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                }

                // Segmented Filter Control (matching Openship screenshot: Frontend | Backend | Fullstack)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.bgPill)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(999.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SegmentedFilterTab(
                        label = "All (${state.projects.size})",
                        isSelected = selectedFilter == ProjectFilterTab.ALL,
                        onClick = { selectedFilter = ProjectFilterTab.ALL },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentedFilterTab(
                        label = "Active",
                        isSelected = selectedFilter == ProjectFilterTab.ACTIVE,
                        onClick = { selectedFilter = ProjectFilterTab.ACTIVE },
                        modifier = Modifier.weight(1f)
                    )
                    SegmentedFilterTab(
                        label = "Building",
                        isSelected = selectedFilter == ProjectFilterTab.BUILDING,
                        onClick = { selectedFilter = ProjectFilterTab.BUILDING },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Search Bar Field
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChanged,
                    placeholder = {
                        Text(
                            "Search projects by name, repo, or framework...",
                            color = colors.textMuted,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.bgCard,
                        unfocusedContainerColor = colors.bgCard,
                        focusedBorderColor = colors.borderFocus,
                        unfocusedBorderColor = colors.borderInput,
                        focusedTextColor = colors.textHeading,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    singleLine = true
                )

                // Projects List Content
                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = colors.btnPrimaryBg,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    state.error != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.bgCard)
                                .border(1.dp, colors.statusFailedBorder, RoundedCornerShape(16.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = colors.statusFailed,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Connection Failed",
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textHeading,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = state.error,
                                    color = colors.textSecondary,
                                    fontSize = 13.sp
                                )
                                Button(
                                    onClick = onRefresh,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.btnPrimaryBg,
                                        contentColor = colors.btnPrimaryText
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Retry Connection")
                                }
                            }
                        }
                    }
                    displayedProjects.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.bgCard)
                                .border(1.dp, colors.borderCard, RoundedCornerShape(16.dp))
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = "No Projects",
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = if (state.searchQuery.isNotBlank()) "No matching projects" else "No projects deployed",
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textHeading,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (state.searchQuery.isNotBlank()) "Try another search filter." else "Projects deployed on this Openship server will appear here.",
                                    color = colors.textMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(
                                items = displayedProjects,
                                key = { it.id }
                            ) { project ->
                                ProjectCard(
                                    project = project,
                                    onClick = { onProjectClicked(project) },
                                    mcpRedeployAvailable = state.redeployAvailable,
                                    onRedeployClick = { onRedeployClick(project) },
                                    onHistoryClick = { onHistoryClick(project) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RedeployConfirmDialog(
    projectName: String,
    isLoading: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = OpenshipAppTheme.colors
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text("Redeploy project?", fontWeight = FontWeight.Bold, color = colors.textHeading)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "\"$projectName\" will be redeployed from its current source.",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
                if (error != null) {
                    Text(
                        text = error,
                        fontSize = 13.sp,
                        color = colors.statusFailed
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = colors.btnPrimaryBg,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text("Redeploy")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !isLoading) {
                Text("Cancel")
            }
        },
        containerColor = colors.bgCard
    )
}

@Composable
private fun SegmentedFilterTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OpenshipAppTheme.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (isSelected) colors.bgCard else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) colors.borderCard else Color.Transparent,
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) colors.textHeading else colors.textMuted
        )
    }
}

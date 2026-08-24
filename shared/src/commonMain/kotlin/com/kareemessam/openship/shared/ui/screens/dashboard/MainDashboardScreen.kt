package com.kareemessam.openship.shared.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kareemessam.openship.shared.model.ProjectSummary
import com.kareemessam.openship.shared.ui.screens.monitor.ServerMonitorScreen
import com.kareemessam.openship.shared.ui.screens.projects.ProjectsScreen
import com.kareemessam.openship.shared.ui.theme.OpenshipAppTheme
import com.kareemessam.openship.shared.viewmodel.ProjectsViewModel
import org.koin.compose.viewmodel.koinViewModel

enum class DashboardTab {
    PROJECTS,
    MONITOR
}

@Composable
fun MainDashboardScreen(
    onAddInstanceClicked: () -> Unit,
    onProjectClicked: (ProjectSummary) -> Unit,
    onOpenDeploymentLogs: (ProjectSummary, String) -> Unit,
    onHistoryClicked: (ProjectSummary) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableStateOf(DashboardTab.PROJECTS) }
    val colors = OpenshipAppTheme.colors

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = colors.bgCard,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.borderCard, RoundedCornerShape(0.dp))
            ) {
                NavigationBarItem(
                    selected = selectedTab == DashboardTab.PROJECTS,
                    onClick = { selectedTab = DashboardTab.PROJECTS },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == DashboardTab.PROJECTS) Icons.Filled.GridView else Icons.Outlined.GridView,
                            contentDescription = "Projects"
                        )
                    },
                    label = { Text("Projects", fontSize = 11.sp, fontWeight = if (selectedTab == DashboardTab.PROJECTS) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.btnPrimaryText,
                        selectedTextColor = colors.textHeading,
                        indicatorColor = colors.btnPrimaryBg,
                        unselectedIconColor = colors.textMuted,
                        unselectedTextColor = colors.textMuted
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == DashboardTab.MONITOR,
                    onClick = { selectedTab = DashboardTab.MONITOR },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == DashboardTab.MONITOR) Icons.Filled.ShowChart else Icons.Outlined.ShowChart,
                            contentDescription = "Monitoring"
                        )
                    },
                    label = { Text("Monitoring", fontSize = 11.sp, fontWeight = if (selectedTab == DashboardTab.MONITOR) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.btnPrimaryText,
                        selectedTextColor = colors.textHeading,
                        indicatorColor = colors.btnPrimaryBg,
                        unselectedIconColor = colors.textMuted,
                        unselectedTextColor = colors.textMuted
                    )
                )
            }
        },
        containerColor = colors.bgPage,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                DashboardTab.PROJECTS -> {
                    ProjectsScreen(
                        state = state,
                        onRefresh = viewModel::refresh,
                        onSearchChanged = viewModel::onSearchQueryChanged,
                        onInstanceSelected = viewModel::switchInstance,
                        onAddInstanceClicked = onAddInstanceClicked,
                        onProjectClicked = onProjectClicked,
                        onRedeployClick = viewModel::onRedeployClick,
                        onRedeployConfirm = viewModel::confirmRedeploy,
                        onRedeployCancel = viewModel::cancelRedeploy,
                        onRedeployResultConsumed = viewModel::consumeRedeployResult,
                        onOpenLogs = onOpenDeploymentLogs,
                        onHistoryClick = onHistoryClicked
                    )
                }
                DashboardTab.MONITOR -> {
                    ServerMonitorScreen(
                        onAddInstanceClicked = onAddInstanceClicked
                    )
                }
            }
        }
    }
}

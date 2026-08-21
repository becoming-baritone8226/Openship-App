package com.kareemessam.openship

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kareemessam.openship.shared.storage.TokenStorage
import com.kareemessam.openship.shared.ui.screens.connect.ConnectScreen
import com.kareemessam.openship.shared.ui.screens.dashboard.MainDashboardScreen
import com.kareemessam.openship.shared.ui.screens.logs.DeployLogsScreen
import com.kareemessam.openship.shared.ui.theme.OpenshipTheme
import com.kareemessam.openship.shared.ui.theme.ThemeMode
import com.kareemessam.openship.shared.viewmodel.ConnectViewModel
import com.kareemessam.openship.shared.viewmodel.DeployLogsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val themeModeState = remember { mutableStateOf(ThemeMode.DARK) }

    OpenshipTheme(themeModeState = themeModeState) {
        val navController = rememberNavController()
        val tokenStorage: TokenStorage = koinInject()
        val activeInstance = tokenStorage.getActiveInstance()
        val startDestination = if (activeInstance != null) "dashboard" else "connect"

        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable("connect") {
                val viewModel: ConnectViewModel = koinViewModel()
                val state by viewModel.state.collectAsState()

                ConnectScreen(
                    state = state,
                    onUrlChanged = viewModel::onUrlChanged,
                    onLabelChanged = viewModel::onLabelChanged,
                    onPatChanged = viewModel::onPatChanged,
                    onProbeClicked = viewModel::probeUrl,
                    onConnectClicked = viewModel::connect,
                    onSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("connect") { inclusive = true }
                        }
                    }
                )
            }

            composable("dashboard") {
                MainDashboardScreen(
                    onAddInstanceClicked = {
                        navController.navigate("connect")
                    },
                    onProjectClicked = { project ->
                        val depId = project.activeDeploymentId ?: "live"
                        val encodedName = project.name.replace("/", "_")
                        navController.navigate("logs/${project.id}/$depId/$encodedName")
                    }
                )
            }

            composable(
                route = "logs/{projectId}/{deploymentId}/{projectName}",
                arguments = listOf(
                    navArgument("projectId") { type = NavType.StringType },
                    navArgument("deploymentId") { type = NavType.StringType },
                    navArgument("projectName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                val deploymentId = backStackEntry.arguments?.getString("deploymentId") ?: ""
                val projectName = backStackEntry.arguments?.getString("projectName")?.replace("_", "/") ?: ""

                val viewModel: DeployLogsViewModel = koinViewModel()
                val state by viewModel.state.collectAsState()

                LaunchedEffect(deploymentId) {
                    viewModel.initDeployment(projectName, deploymentId)
                }

                DeployLogsScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onSearchChanged = viewModel::onSearchQueryChanged,
                    onAutoScrollChanged = viewModel::setAutoScroll,
                    onRetry = viewModel::retry
                )
            }
        }
    }
}
package com.kareemessam.openship.shared.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kareemessam.openship.shared.ui.screens.connect.ConnectScreen
import com.kareemessam.openship.shared.ui.screens.dashboard.MainDashboardScreen
import com.kareemessam.openship.shared.ui.screens.logs.DeployLogsScreen
import com.kareemessam.openship.shared.viewmodel.ConnectViewModel
import com.kareemessam.openship.shared.viewmodel.DeployLogsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Connect.route) {
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
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Connect.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            MainDashboardScreen(
                onAddInstanceClicked = {
                    navController.navigate(Screen.Connect.route)
                },
                onProjectClicked = { project ->
                    val depId = project.activeDeploymentId ?: "live"
                    navController.navigate(Screen.Logs.createRoute(project.id, depId, project.name))
                }
            )
        }

        composable(
            route = Screen.Logs.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("deploymentId") { type = NavType.StringType },
                navArgument("projectName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
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

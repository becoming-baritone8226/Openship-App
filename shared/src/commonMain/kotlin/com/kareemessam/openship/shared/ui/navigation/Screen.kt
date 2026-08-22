package com.kareemessam.openship.shared.ui.navigation

sealed interface Screen {
    val route: String

    data object Connect : Screen {
        override val route = "connect"
    }

    data object Dashboard : Screen {
        override val route = "dashboard"
    }

    data object Logs : Screen {
        override val route = "logs/{projectId}/{deploymentId}/{projectName}"

        fun createRoute(projectId: String, deploymentId: String, projectName: String): String {
            val encodedName = projectName.replace("/", "_")
            return "logs/$projectId/$deploymentId/$encodedName"
        }
    }
}

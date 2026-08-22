package com.kareemessam.openship

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.kareemessam.openship.shared.storage.TokenStorage
import com.kareemessam.openship.shared.ui.navigation.AppNavHost
import com.kareemessam.openship.shared.ui.navigation.Screen
import com.kareemessam.openship.shared.ui.theme.OpenshipTheme
import com.kareemessam.openship.shared.ui.theme.ThemeMode
import org.koin.compose.koinInject

@Composable
fun App(tokenStorage: TokenStorage = koinInject()) {
    val themeModeState = remember { mutableStateOf(ThemeMode.DARK) }
    val startDestination = if (tokenStorage.getActiveInstance() != null) Screen.Dashboard.route else Screen.Connect.route

    OpenshipTheme(themeModeState = themeModeState) {
        val navController = rememberNavController()
        AppNavHost(navController = navController, startDestination = startDestination)
    }
}

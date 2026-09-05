package com.jarvis.assistant.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jarvis.assistant.ui.screens.MainScreen
import com.jarvis.assistant.ui.screens.SettingsScreen
import com.jarvis.assistant.viewmodel.AssistantViewModel
import com.jarvis.assistant.viewmodel.SettingsViewModel

private object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
}

@Composable
fun JarvisNavHost(
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val assistantViewModel: AssistantViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(
                viewModel = assistantViewModel,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                hasMicPermission = hasMicPermission,
                onRequestMicPermission = onRequestMicPermission
            )
        }
        composable(Routes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel()
            SettingsScreen(viewModel = settingsViewModel, onBack = { navController.popBackStack() })
        }
    }
}

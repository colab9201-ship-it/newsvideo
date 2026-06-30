package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.ProjectViewModel

object NewsDestinations {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val CAMERA = "camera"
    const val EDITOR = "editor/{projectId}"
    
    fun createEditorRoute(projectId: Int) = "editor/$projectId"
}

@Composable
fun NewsNavGraph(
    navController: NavHostController,
    viewModel: ProjectViewModel
) {
    NavHost(
        navController = navController,
        startDestination = NewsDestinations.HOME
    ) {
        composable(NewsDestinations.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToEditor = { projectId ->
                    navController.navigate(NewsDestinations.createEditorRoute(projectId))
                },
                onNavigateToSettings = {
                    navController.navigate(NewsDestinations.SETTINGS)
                }
            )
        }

        composable(NewsDestinations.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(NewsDestinations.CAMERA) {
            CameraScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = NewsDestinations.EDITOR,
            arguments = listOf(navArgument("projectId") { type = NavType.IntType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getInt("projectId") ?: 0
            EditorScreen(
                projectId = projectId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCamera = {
                    navController.navigate(NewsDestinations.CAMERA)
                }
            )
        }
    }
}

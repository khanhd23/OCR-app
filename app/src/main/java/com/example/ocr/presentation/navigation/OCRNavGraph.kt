package com.example.ocr.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ocr.presentation.feature.camera.CameraScreen
import com.example.ocr.presentation.feature.history.HistoryScreen
import com.example.ocr.presentation.feature.intro.IntroScreen
import com.example.ocr.presentation.feature.result.ResultScreen
import com.example.ocr.presentation.feature.splash.SplashScreen

@Composable
fun OCRNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onFinish = {
                    navController.navigate(Screen.Intro.route) {
                        popUpTo(Screen.Splash.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Intro.route) {
            IntroScreen(
                onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) }
            )
        }

        composable(Screen.Camera.route) {
            CameraScreen(
                onNavigateBack = { navController.popBackStack() },
                onOCRSuccess = { docId ->
                    navController.navigate(Screen.Result.createRoute(docId)) {
                        popUpTo(Screen.Camera.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Result.route,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType })
        ) { backStack ->
            val docId = backStack.arguments?.getLong("documentId") ?: -1L
            ResultScreen(
                documentId = docId,
                onNavigateBack = {
                    navController.navigate(Screen.Intro.route) {
                        popUpTo(0)
                    }
                },
                onNavigateToHistory = { navController.navigate(Screen.History.route) }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenDocument = { docId ->
                    navController.navigate(Screen.Result.createRoute(docId))
                }
            )
        }
    }
}
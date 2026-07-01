package com.example.ocr.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Intro : Screen("intro")
    object Camera : Screen("camera")
    object History : Screen("history")
    object Settings : Screen("settings")

    object Result : Screen("result/{documentId}") {
        const val ARG_DOCUMENT_ID = "documentId"

        fun createRoute(documentId: Long): String {
            return "result/$documentId"
        }
    }
}
package com.example.procrastination_detection.navigation

import kotlinx.serialization.Serializable

sealed interface Screen{
    @Serializable
    data object Home : Screen
    @Serializable
    data object RulesManager : Screen
    @Serializable
    data object AppLibrary : Screen
}
package com.example.procrastination_detection

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.procrastination_detection.database.getDatabaseBuilder
import com.example.procrastination_detection.database.getRoomDatabase
import com.example.procrastination_detection.repositories.LocalAppRepository

fun main() = application {
    // 1. Build the DB using the JVM-specific file path
    val dbBuilder = getDatabaseBuilder()
    val database = getRoomDatabase(dbBuilder)

    val appContainer = AppContainer(
        processDao = database.processDao(),
        sessionDao = database.sessionDao(),
        ruleDao = database.ruleDao(),
        categoryDao = database.categoryDao()
    )

    // 3. Pass it to the shared App Composable
    Window(
        onCloseRequest = ::exitApplication,
        title = "Procrastination Detector"
    ) {
        App(appContainer = appContainer)
    }
}
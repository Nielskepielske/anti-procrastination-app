package com.example.procrastination_detection

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.procrastination_detection.database.getDatabaseBuilder
import com.example.procrastination_detection.database.getRoomDatabase
import com.example.procrastination_detection.pages.home.AppListScreen
import com.example.procrastination_detection.repositories.LocalAppRepository

fun main() = application {
    // 1. Build the DB using the JVM-specific file path
    val dbBuilder = getDatabaseBuilder()
    val database = getRoomDatabase(dbBuilder)

    // 2. Create the Repository
    val localRepo = LocalAppRepository(
        processDao = database.processDao(),
        sessionDao = database.sessionDao(),
        ruleDao = database.ruleDao()
    )

    // 3. Pass it to the shared App Composable
    Window(
        onCloseRequest = ::exitApplication,
        title = "Procrastination Detector"
    ) {
        App(repository = localRepo)
    }
}
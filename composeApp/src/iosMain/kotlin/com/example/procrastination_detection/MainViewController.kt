package com.example.procrastination_detection

import androidx.compose.ui.window.ComposeUIViewController
import com.example.procrastination_detection.database.getRoomDatabase
import com.example.procrastination_detection.repositories.LocalAppRepository

fun MainViewController() = ComposeUIViewController {

    // 1. Build the DB using the iOS-specific file paths
    val dbBuilder = getDatabaseBuilder()
    val database = getRoomDatabase(dbBuilder)

    // 2. Create the Repository
    val localRepo = LocalAppRepository(
        processDao = database.processDao(),
        sessionDao = database.sessionDao(),
        ruleDao = database.ruleDao()
    )

    // 3. Pass it to the App
    App(repository = localRepo)
}
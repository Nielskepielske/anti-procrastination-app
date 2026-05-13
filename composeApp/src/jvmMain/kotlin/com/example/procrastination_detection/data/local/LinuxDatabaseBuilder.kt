package com.example.procrastination_detection.data.local

import androidx.room.Room
import androidx.room.Room.databaseBuilder
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

fun buildRoomDatabaseForLinux(): AppDatabase {
    // 1. Find the user's home directory (e.g., /home/username)
    val userHome = System.getProperty("user.home")

    // 2. Define the standard Linux hidden data folder for your app
    val appDataDir = File(userHome, ".local/share/ProcrastinationAware")

    // Create the folders if this is the first time the app is running
    if (!appDataDir.exists()) {
        appDataDir.mkdirs()
    }

    // 3. Define the actual database file
    val dbFile = File(appDataDir, "behavior_data.db")

    // 4. Build and return the Room Database instance
    return databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath
    )
        // In KMP, Room requires you to explicitly set the SQLite driver
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(true)
        .build()
}
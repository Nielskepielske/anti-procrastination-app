package com.example.procrastination_detection.database

// shared/src/jvmMain/kotlin/.../Database.jvm.kt
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    // Pick a safe folder in the user's home directory
    val userHome = System.getProperty("user.home")
    val appFolder = File(userHome, ".procrastination_detection")

    // Create the folder if it doesn't exist yet
    if (!appFolder.exists()) {
        appFolder.mkdirs()
    }

    val dbFile = File(appFolder, "procrastination.db")

    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath
    )
}
package com.example.procrastination_detection.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getDatabaseBuilder(ctx: Context): RoomDatabase.Builder<AppDatabase> {
    val dbFile = ctx.getDatabasePath("procrastination.db")
    return Room.databaseBuilder<AppDatabase>(
        context = ctx.applicationContext,
        name = dbFile.absolutePath
    )
}
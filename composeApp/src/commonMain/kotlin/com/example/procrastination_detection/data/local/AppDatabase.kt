package com.example.procrastination_detection.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [SensorEventEntity::class, RuleEntity::class, AppUsageEntity::class, InboxEntity::class, FocusProfileEntity::class],
    version = 5
)
@TypeConverters(PayloadConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorEventDao(): SensorEventDao
    abstract fun ruleDao(): RuleDao
    abstract fun appUsageDao(): AppUsageDao
    abstract fun inboxDao(): InboxDao
    abstract fun focusProfileDao(): FocusProfileDao
}
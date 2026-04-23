package com.example.procrastination_detection.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.procrastination_detection.data.local.dao.AppUsageDao
import com.example.procrastination_detection.data.local.dao.CompactionDao
import com.example.procrastination_detection.data.local.dao.FocusProfileDao
import com.example.procrastination_detection.data.local.dao.InboxDao
import com.example.procrastination_detection.data.local.dao.RuleDao
import com.example.procrastination_detection.data.local.dao.SensorEventDao
import com.example.procrastination_detection.data.local.entity.AppUsageEntity
import com.example.procrastination_detection.data.local.entity.DailySensorEventEntity
import com.example.procrastination_detection.data.local.entity.FocusProfileEntity
import com.example.procrastination_detection.data.local.entity.HourlySensorEventEntity
import com.example.procrastination_detection.data.local.entity.InboxEntity
import com.example.procrastination_detection.data.local.entity.RuleEntity
import com.example.procrastination_detection.data.local.entity.SensorEventEntity

@Database(
    entities = [
        SensorEventEntity::class,
        HourlySensorEventEntity::class,  // NEW
        DailySensorEventEntity::class,   // NEW
        RuleEntity::class,
        AppUsageEntity::class,
        InboxEntity::class,
        FocusProfileEntity::class
    ],
    version = 6 // Bumped from 5 to 6
)
@TypeConverters(PayloadConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorEventDao(): SensorEventDao
    abstract fun compactionDao(): CompactionDao // NEW
    abstract fun ruleDao(): RuleDao
    abstract fun appUsageDao(): AppUsageDao
    abstract fun inboxDao(): InboxDao
    abstract fun focusProfileDao(): FocusProfileDao
}
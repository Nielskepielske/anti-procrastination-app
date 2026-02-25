@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.example.procrastination_detection.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.example.procrastination_detection.models.db.dto.CategoryEntity
import com.example.procrastination_detection.models.db.dto.MonitoredProcessEntity
import com.example.procrastination_detection.models.db.dto.ProcessEntity
import com.example.procrastination_detection.models.db.dto.RuleEntity
import com.example.procrastination_detection.models.db.dto.RuleLineEntity
import com.example.procrastination_detection.models.db.dto.SessionEntity

@Database(entities = [SessionEntity::class, MonitoredProcessEntity::class, RuleEntity::class, RuleLineEntity::class, CategoryEntity::class, ProcessEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun ruleDao(): RuleDao
    abstract fun ruleLineDao(): RuleLineDao
    abstract fun categoryDao(): CategoryDao
    abstract fun processDao(): ProcessDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

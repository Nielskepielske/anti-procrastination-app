package com.example.procrastination_detection.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageDao {
    @Query("SELECT * FROM app_usage WHERE dayIndex = :dayIndex")
    fun getUsageForDate(dayIndex: Long): Flow<List<AppUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: AppUsageEntity)

    @Query("UPDATE app_usage SET totalSeconds = totalSeconds + :seconds WHERE dayIndex = :dayIndex AND processName = :processName AND windowTitle = :windowTitle")
    suspend fun incrementUsage(dayIndex: Long, processName: String, windowTitle: String, seconds: Long): Int
    @Query("SELECT * FROM app_usage WHERE dayIndex BETWEEN :startDay AND :endDay")
    fun getUsageForRange(startDay: Long, endDay: Long): Flow<List<AppUsageEntity>>

    @Query("SELECT SUM(totalSeconds) FROM app_usage WHERE dayIndex = :dayIndex")
    fun getTotalSecondsForDay(dayIndex: Long): Flow<Long?>
}


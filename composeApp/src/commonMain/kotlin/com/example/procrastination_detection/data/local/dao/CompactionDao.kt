package com.example.procrastination_detection.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.procrastination_detection.data.local.entity.DailySensorEventEntity
import com.example.procrastination_detection.data.local.entity.HourlySensorEventEntity
import com.example.procrastination_detection.data.local.entity.SensorEventEntity

@Dao
interface CompactionDao {

    // --- RAW TO HOURLY ---

    @Query("SELECT * FROM sensor_events WHERE timestamp < :cutoffTimestamp ORDER BY timestamp ASC")
    suspend fun getRawEventsOlderThan(cutoffTimestamp: Long): List<SensorEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlyEvents(events: List<HourlySensorEventEntity>)

    @Query("DELETE FROM sensor_events WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteRawEventsOlderThan(cutoffTimestamp: Long)

    @Transaction
    suspend fun compactRawToHourly(cutoffTimestamp: Long, aggregatedEvents: List<HourlySensorEventEntity>) {
        insertHourlyEvents(aggregatedEvents)
        deleteRawEventsOlderThan(cutoffTimestamp)
    }

    // --- HOURLY TO DAILY (For future use) ---

    @Query("SELECT * FROM sensor_events_hourly WHERE hourTimestamp < :cutoffTimestamp")
    suspend fun getHourlyEventsOlderThan(cutoffTimestamp: Long): List<HourlySensorEventEntity>

    // Fetches aggregated hourly events for a specific time window
    // Fetches aggregated hourly events for a specific time window
    @Query("""
        SELECT * FROM sensor_events_hourly 
        WHERE hourTimestamp BETWEEN :start AND :end 
        AND (:sensorId IS NULL OR sensorId = :sensorId)
        ORDER BY hourTimestamp ASC
    """)
    suspend fun getHourlyEventsBetween(start: Long, end: Long, sensorId: String? = null): List<HourlySensorEventEntity>

    // Fetches aggregated daily events for a massive time window (e.g., Months/Years)
    // Fetches aggregated daily events for a massive time window (e.g., Months/Years)
    @Query("""
        SELECT * FROM sensor_events_daily 
        WHERE dayTimestamp BETWEEN :start AND :end 
        AND (:sensorId IS NULL OR sensorId = :sensorId)
        ORDER BY dayTimestamp ASC
    """)
    suspend fun getDailyEventsBetween(start: Long, end: Long, sensorId: String? = null): List<DailySensorEventEntity>

    // ... you can add the daily insertion/deletion methods here later following the same pattern
}
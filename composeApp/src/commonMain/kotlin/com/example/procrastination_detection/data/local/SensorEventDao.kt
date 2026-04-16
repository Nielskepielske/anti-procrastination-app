package com.example.procrastination_detection.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SensorEventDao {
    @Insert
    suspend fun insertEvent(event: SensorEventEntity)

    // Useful for checking last x amount of time of activity
    @Query("SELECT * FROM sensor_events WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getEventsSince(sinceTimestamp: Long): List<SensorEventEntity>

    // Privacy feature: Auto-delete old row data
    @Query("DELETE FROM sensor_events WHERE timestamp < :beforeTimestamp")
    suspend fun deleteEventsBefore(beforeTimestamp: Long)

    // Behavioral analytics: count events of a specific type, bucketed by hour
    // hourBucket = timestamp / 3600000  (i.e. epoch hour index)
    @Query("""
        SELECT (timestamp / 3600000) AS hourBucket, COUNT(*) AS frequency
        FROM sensor_events
        WHERE payloadType = :payloadType AND timestamp BETWEEN :start AND :end
        GROUP BY hourBucket
        ORDER BY hourBucket ASC
    """)
    suspend fun getEventCountsPerHour(payloadType: String, start: Long, end: Long): List<HourlyCount>

    // Total count of a specific event type for a time range
    @Query("SELECT COUNT(*) FROM sensor_events WHERE payloadType = :payloadType AND timestamp BETWEEN :start AND :end")
    suspend fun getEventCountForRange(payloadType: String, start: Long, end: Long): Int
}

/** Lightweight projection used for the switch-frequency chart. */
data class HourlyCount(
    val hourBucket: Long,
    val frequency: Int
)
package com.example.procrastination_detection.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.procrastination_detection.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveSession(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE status = 'ACTIVE' LIMIT 1")
    fun getActiveSessionFlow(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessionsFlow(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE startTime <= :endTime AND (endTime >= :startTime OR endTime IS NULL)")
    suspend fun getSessionsOverlapping(startTime: Long, endTime: Long): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Update
    suspend fun updateSession(session: SessionEntity)
}

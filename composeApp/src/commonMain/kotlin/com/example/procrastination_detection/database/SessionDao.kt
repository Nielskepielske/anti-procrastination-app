package com.example.procrastination_detection.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.procrastination_detection.models.db.Session
import com.example.procrastination_detection.models.db.dto.SessionEntity
import com.example.procrastination_detection.models.db.dto.SessionFull
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun getFullSession(sessionId: String): Flow<SessionFull?>

    @Transaction
    @Query("SELECT * FROM sessions ORDER BY id DESC")
    fun getAllSessions(): Flow<List<SessionFull>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE name = :name LIMIT 1")
    suspend fun getSessionByName(name: String): SessionFull?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)
}
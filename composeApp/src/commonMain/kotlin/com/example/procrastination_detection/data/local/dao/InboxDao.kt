package com.example.procrastination_detection.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.procrastination_detection.data.local.entity.InboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InboxDao {
    @Query("SELECT * FROM inbox ORDER BY timestampMs DESC")
    fun getAllUnreviewedFlow(): Flow<List<InboxEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE) // Prevent duplicate entries for same contextStr
    suspend fun insert(item: InboxEntity)

    @Delete
    suspend fun delete(item: InboxEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM inbox WHERE contextStr = :contextStr LIMIT 1)")
    suspend fun exists(contextStr: String): Boolean
}

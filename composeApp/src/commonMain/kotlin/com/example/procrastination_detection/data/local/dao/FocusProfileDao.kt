package com.example.procrastination_detection.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.procrastination_detection.data.local.entity.FocusProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusProfileDao {

    @Query("SELECT * FROM focus_profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<FocusProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: FocusProfileEntity)

    @Query("DELETE FROM focus_profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)

    @Query("SELECT * FROM focus_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): FocusProfileEntity?
}

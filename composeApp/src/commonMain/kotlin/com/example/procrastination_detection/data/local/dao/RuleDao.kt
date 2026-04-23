package com.example.procrastination_detection.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.procrastination_detection.data.local.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    // 1. Emits a continuous stream of updates for the Compose UI
    @Query("SELECT * FROM rules")
    fun getAllRulesFlow(): Flow<List<RuleEntity>>

    // 2. A one-time fetch used by the Engine when the app starts
    @Query("SELECT * FROM rules")
    suspend fun getAllRulesSync(): List<RuleEntity>

    // 3. Adds or overwrites a rule
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity)

    // 4. Deletes a rule
    @Delete
    suspend fun deleteRule(rule: RuleEntity)
}
package com.example.procrastination_detection.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.procrastination_detection.models.db.RuleLine
import com.example.procrastination_detection.models.db.dto.RuleLineEntity

@Dao
interface RuleLineDao {
    // fetch functions
    @Query("SELECT * FROM rule_lines")
    suspend fun getAll(): List<RuleLineEntity>
    @Query("SELECT * FROM rule_lines WHERE ruleId = :ruleId")
    suspend fun getAllByRuleId(ruleId: String): List<RuleLineEntity>
    @Query("SELECT * FROM rule_lines WHERE id = :id")
    suspend fun getById(id: String): RuleLineEntity?

    // insert functions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ruleLine: RuleLineEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ruleLines: List<RuleLineEntity>)

    // delete functions
    @Query("DELETE FROM rule_lines WHERE ruleId = :ruleId")
    suspend fun deleteAllByRule(ruleId: String)
    @Delete
    suspend fun delete(ruleLine: RuleLineEntity)
}
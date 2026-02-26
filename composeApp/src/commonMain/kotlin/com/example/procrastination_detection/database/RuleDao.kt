package com.example.procrastination_detection.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.procrastination_detection.models.db.Rule
import com.example.procrastination_detection.models.db.dto.RuleEntity
import com.example.procrastination_detection.models.db.dto.RuleLineEntity
import com.example.procrastination_detection.models.db.dto.RuleWithLines
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Transaction
    @Query("SELECT * FROM rules WHERE id = :ruleId")
    fun getRuleWithLines(ruleId: String): Flow<RuleWithLines?>

    @Transaction
    @Query("SELECT * FROM rules")
    fun getAllRulesWithLines(): Flow<List<RuleWithLines>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRuleLines(lines: List<RuleLineEntity>)

    @Transaction
    suspend fun insertFullRule(rule: RuleEntity, lines: List<RuleLineEntity>) {
        insertRule(rule)
        insertRuleLines(lines)
    }

    // Delete a specific rule (CASCADE will auto-delete its lines!)
    @Query("DELETE FROM rules WHERE id = :ruleId")
    suspend fun deleteRule(ruleId: String)

    // Delete just the lines for a specific rule (used during editing)
    @Query("DELETE FROM rule_lines WHERE ruleId = :ruleId")
    suspend fun deleteRuleLines(ruleId: String)

    // A safe transaction for updating: replace the rule, wipe old lines, insert new lines
    @Transaction
    suspend fun updateFullRule(rule: RuleEntity, lines: List<RuleLineEntity>, ruleId: String) {
        insertRule(rule) // Uses OnConflictStrategy.REPLACE
        deleteRuleLines(ruleId)
        insertRuleLines(lines)
    }
}
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRuleLines(lines: List<RuleLineEntity>)
}
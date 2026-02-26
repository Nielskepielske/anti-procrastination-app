package com.example.procrastination_detection.interfaces

import com.example.procrastination_detection.models.db.Category
import com.example.procrastination_detection.models.db.Rule
import com.example.procrastination_detection.models.db.RuleLine
import kotlinx.coroutines.flow.Flow

interface ConfigRepository {
    val allCategories: Flow<List<Category>>
    val allRules: Flow<List<Rule>>
    suspend fun createCategory(name: String, isProductive: Boolean): Category?
    suspend fun createRule(name: String, draftLines: List<RuleLine>)
    suspend fun deleteRule(ruleId: String)
    suspend fun saveRule(id: String?, name: String, draftLines: List<RuleLine>)
}
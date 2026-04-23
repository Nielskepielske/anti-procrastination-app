package com.example.procrastination_detection.domain.repository

import com.example.procrastination_detection.data.local.AppDatabase
import com.example.procrastination_detection.data.local.entity.RuleEntity
import com.example.procrastination_detection.domain.dictionary.*
import com.example.procrastination_detection.domain.model.Category
import com.example.procrastination_detection.domain.trigger.TriggerManager
import kotlinx.coroutines.flow.Flow

/**
 * All supported rule matching strategies.
 * Adding a new strategy here automatically makes it available in the UI picker.
 */
enum class RuleType(val label: String, val description: String) {
    TITLE_CONTAINS("Title Contains", "Matches if the window title contains the keyword"),
    PROCESS_EXACT("Process Exact", "Matches only this exact process name"),
    PROCESS_CONTAINS("Process Contains", "Matches any process whose name contains the keyword"),
    BROWSER_PROCESS("Browser App", "Marks this process as a browser — enables URL tracking via screenshot OCR"),
    REGEX("Regex Pattern", "Advanced: matches process name + title against a regex expression")
}

interface IRuleRepository {
    val rulesFlow: Flow<List<RuleEntity>>
    suspend fun addRule(condition: String, category: Category, ruleType: RuleType = RuleType.TITLE_CONTAINS, triggerId: String? = null)
    suspend fun updateRule(existing: RuleEntity, condition: String, category: Category, ruleType: RuleType, triggerId: String? = null)
    suspend fun removeRule(rule: RuleEntity)
    suspend fun getRulesForEngine(): List<CategorizationRule>
    suspend fun getBrowserProcessNames(): List<String>
}

class RuleRepository(
    private val database: AppDatabase,
    private val triggerManager: TriggerManager
) : IRuleRepository {

    private val dao = database.ruleDao()

    override val rulesFlow: Flow<List<RuleEntity>> = dao.getAllRulesFlow()

    override suspend fun addRule(condition: String, category: Category, ruleType: RuleType, triggerId: String?) {
        dao.insertRule(
            RuleEntity(ruleType = ruleType.name, condition = condition, category = category, triggerId = triggerId)
        )
    }

    override suspend fun updateRule(existing: RuleEntity, condition: String, category: Category, ruleType: RuleType, triggerId: String?) {
        dao.deleteRule(existing)
        dao.insertRule(
            RuleEntity(ruleType = ruleType.name, condition = condition, category = category, triggerId = triggerId)
        )
    }

    override suspend fun removeRule(rule: RuleEntity) {
        dao.deleteRule(rule)
    }

    override suspend fun getRulesForEngine(): List<CategorizationRule> {
        return dao.getAllRulesSync().mapNotNull { entity ->
            val type = runCatching { RuleType.valueOf(entity.ruleType) }.getOrNull()
                ?: return@mapNotNull TitleContainsRule(entity.condition, entity.category) // safe fallback
            
            val trigger = entity.triggerId?.let { triggerManager.getTrigger(it) }

            when (type) {
                RuleType.TITLE_CONTAINS    -> TitleContainsRule(entity.condition, entity.category)
                RuleType.PROCESS_EXACT     -> ProcessExactRule(entity.condition, entity.category)
                RuleType.PROCESS_CONTAINS  -> ProcessContainsRule(entity.condition, entity.category)
                RuleType.BROWSER_PROCESS   -> BrowserProcessRule(entity.condition, trigger)
                RuleType.REGEX             -> RegexRule(entity.condition, entity.category)
            }
        }
    }

    /** Returns the condition strings of all BROWSER_PROCESS rules (used by BrowserAnalyserEngine). */
    override suspend fun getBrowserProcessNames(): List<String> {
        return dao.getAllRulesSync()
            .filter { it.ruleType == RuleType.BROWSER_PROCESS.name }
            .map { it.condition }
    }
}
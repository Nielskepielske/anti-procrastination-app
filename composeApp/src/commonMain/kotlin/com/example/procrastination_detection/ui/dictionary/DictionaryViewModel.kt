package com.example.procrastination_detection.ui.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.procrastination_detection.data.local.InboxDao
import com.example.procrastination_detection.data.local.InboxEntity
import com.example.procrastination_detection.data.local.RuleEntity
import com.example.procrastination_detection.domain.dictionary.DictionaryEngine
import com.example.procrastination_detection.domain.model.Category
import com.example.procrastination_detection.domain.repository.IRuleRepository
import com.example.procrastination_detection.domain.repository.RuleType
import com.example.procrastination_detection.domain.trigger.ActionTrigger
import com.example.procrastination_detection.domain.trigger.TriggerManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DictionaryViewModel(
    private val dictionaryEngine: DictionaryEngine,
    private val ruleRepository: IRuleRepository,
    private val inboxDao: InboxDao,
    private val triggerManager: TriggerManager
) : ViewModel() {

    // Static exposure of available triggers for the UI pickers
    val availableTriggers: List<ActionTrigger> = triggerManager.availableTriggers

    // Live stream of confirmed user rules from Room
    val savedRulesFlow: StateFlow<List<RuleEntity>> = ruleRepository.rulesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Live stream of dynamically discovered, unreviewed apps/URLs from the DiscoveryEngine
    val inboxFlow: StateFlow<List<InboxEntity>> = inboxDao.getAllUnreviewedFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Approve an inbox item: saves it as a rule, reloads the engine, then removes it from inbox. */
    fun approveInboxItem(item: InboxEntity, category: Category, ruleType: RuleType = RuleType.TITLE_CONTAINS, triggerId: String? = null) {
        viewModelScope.launch {
            ruleRepository.addRule(item.contextStr, category, ruleType, triggerId)
            dictionaryEngine.updateRules(ruleRepository.getRulesForEngine())
            inboxDao.delete(item)
        }
    }

    /** Dismiss an inbox item without saving a rule (user doesn't want to track it). */
    fun dismissInboxItem(item: InboxEntity) {
        viewModelScope.launch {
            inboxDao.delete(item)
        }
    }

    /** Manually add a custom rule from the FAB dialog. */
    fun addManualRule(condition: String, category: Category, ruleType: RuleType = RuleType.TITLE_CONTAINS, triggerId: String? = null) {
        viewModelScope.launch {
            ruleRepository.addRule(condition, category, ruleType, triggerId)
            dictionaryEngine.updateRules(ruleRepository.getRulesForEngine())
        }
    }

    /** Edit an existing rule in-place (delete + re-insert to preserve DB simplicity). */
    fun editRule(existing: RuleEntity, condition: String, category: Category, ruleType: RuleType, triggerId: String? = null) {
        viewModelScope.launch {
            ruleRepository.updateRule(existing, condition, category, ruleType, triggerId)
            dictionaryEngine.updateRules(ruleRepository.getRulesForEngine())
        }
    }

    fun deleteRule(rule: RuleEntity) {
        viewModelScope.launch {
            ruleRepository.removeRule(rule)
            dictionaryEngine.updateRules(ruleRepository.getRulesForEngine())
        }
    }
}
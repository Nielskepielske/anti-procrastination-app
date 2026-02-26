package com.example.procrastination_detection.pages.rule

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.procrastination_detection.interfaces.ConfigRepository
import com.example.procrastination_detection.models.db.Category
import com.example.procrastination_detection.models.db.Rule
import com.example.procrastination_detection.models.db.RuleLine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

data class DraftRuleLine(
    val category: Category,
    val isProductive: Boolean = true,
    val maxConsecutiveMins: String = "",
    val maxTotalMins: String = ""
)

class RulesViewModel(
    private val configRepository: ConfigRepository
) : ViewModel() {

    // 1. Data Streams from the Database
    val rules = configRepository.allRules.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val categories = configRepository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // 2. UI Navigation State
    var isCreatingRule by mutableStateOf(false)
        private set

    // 3. Form State
    var draftRuleName by mutableStateOf("")
        private set
    var draftLines by mutableStateOf<List<DraftRuleLine>>(emptyList())
        private set

    // --- SEARCH DIALOG STATE ---
    var showCategoryDialog by mutableStateOf(false)
        private set
    var categorySearchQuery by mutableStateOf("")
        private set

    // Dynamically filter categories based on the search query
    val filteredCategories: StateFlow<List<Category>> = combine(
        configRepository.allCategories,
        snapshotFlow { categorySearchQuery }
    ) { categories, query ->
        if (query.isBlank()) categories
        else categories.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var editingRuleId by mutableStateOf<String?>(null)
        private set

    // 4. Form Actions
    fun startCreatingRule() {
        draftRuleName = ""
        // Generate a fresh draft line for every category that exists!
        draftLines = categories.value.map { DraftRuleLine(category = it) }
        editingRuleId = null
        isCreatingRule = true
    }

    fun startEditingRule(rule: Rule) {
        editingRuleId = rule.id
        draftRuleName = rule.name

        // Map the saved RuleLines back into Drafts for the UI
        draftLines = rule.lines.map { line ->
            DraftRuleLine(
                category = line.category,
                isProductive = line.isProductive,
                // Use the new helper!
                maxConsecutiveMins = formatSecondsToMinutes(line.maxConsecutiveSeconds),
                maxTotalMins = formatSecondsToMinutes(line.maxTotalSeconds)
            )
        }

        isCreatingRule = true // Opens the form UI
    }

    fun cancelCreation() {
        isCreatingRule = false
    }

    fun updateRuleName(name: String) {
        draftRuleName = name
    }

    fun updateDraftLine(category: Category, isProductive: Boolean, consecutive: String, total: String) {
        draftLines = draftLines.map {
            if (it.category.id == category.id) {
                it.copy(isProductive = isProductive, maxConsecutiveMins = consecutive, maxTotalMins = total)
            } else {
                it
            }
        }
    }

    // 5. Save Action
    fun saveRule() {
        if (draftRuleName.isBlank()) return

        viewModelScope.launch {
            val domainLines = draftLines.map { draft ->
                RuleLine(
                    id = "", ruleId = "", name = draft.category.name, category = draft.category,
                    isProductive = draft.isProductive,
                    maxConsecutiveSeconds = ((draft.maxConsecutiveMins.toDoubleOrNull() ?: 0.0) * 60).toLong(),
                    maxTotalSeconds = ((draft.maxTotalMins.toDoubleOrNull() ?: 0.0) * 60).toLong()
                )
            }

            // Use the new saveRule repo method!
            configRepository.saveRule(editingRuleId, draftRuleName, domainLines)

            isCreatingRule = false
            editingRuleId = null
        }
    }

    fun deleteRule(rule: Rule) {
        viewModelScope.launch {
            configRepository.deleteRule(rule.id)
        }
    }


    // Dialog controls
    fun openCategoryDialog() { showCategoryDialog = true }
    fun closeCategoryDialog() {
        showCategoryDialog = false
        categorySearchQuery = ""
    }
    fun updateSearchQuery(query: String) { categorySearchQuery = query }

    // Add an existing category to the rule
    fun addCategoryLine(category: Category) {
        // Prevent adding duplicates to the list
        if (draftLines.none { it.category.id == category.id }) {
            draftLines = draftLines + DraftRuleLine(category = category, isProductive = category.isProductive)
        }
        closeCategoryDialog()
    }

    // Create a brand new category and add it immediately
    fun createAndAddCategory(name: String) {
        viewModelScope.launch {
            // Defaulting to unproductive since they are usually adding limits
            val newCategory = configRepository.createCategory(name, isProductive = false)
            if (newCategory != null) {
                addCategoryLine(newCategory)
            }
        }
    }


    // Helper function to format seconds nicely
    private fun formatSecondsToMinutes(seconds: Long): String {
        if (seconds <= 0) return ""

        val minutes = seconds / 60.0
        // If it's a perfect whole number (like 1.0), drop the decimal
        return if (minutes % 1.0 == 0.0) {
            minutes.toLong().toString()
        } else {
            minutes.toString() // Leaves it as "0.5"
        }
    }

// Inside startEditingRule()...

}
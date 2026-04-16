package com.example.procrastination_detection.domain.dictionary

import com.example.procrastination_detection.domain.model.Category
import com.example.procrastination_detection.domain.model.WindowData
import com.example.procrastination_detection.domain.trigger.ActionTrigger

/**
 * The core matching abstraction. Every rule knows how to evaluate a WindowData snapshot
 * and either returns a Category or null (= this rule doesn't apply here).
 *
 * The DictionaryEngine iterates rules in priority order and returns the first non-null hit.
 * Adding a new matching strategy = implement this interface + add a case in RuleRepository.
 */
interface CategorizationRule {
    val trigger: ActionTrigger? get() = null
    fun evaluate(data: WindowData): Category?
}

/**
 * Matches when the window title contains [keyword] (case-insensitive).
 * Good for apps where the title carries meaningful context (document editors, terminals).
 * Example: condition="budget.xlsx", category=PRODUCTIVE
 */
class TitleContainsRule(
    private val keyword: String,
    private val category: Category
) : CategorizationRule {
    override fun evaluate(data: WindowData) =
        if (data.windowTitle.contains(keyword, ignoreCase = true)) category else null
}

/**
 * Matches when processName is exactly [processName] (case-insensitive).
 * Good for apps whose category is always the same regardless of title.
 * Example: condition="jetbrains-idea", category=PRODUCTIVE
 */
class ProcessExactRule(
    private val processName: String,
    private val category: Category
) : CategorizationRule {
    override fun evaluate(data: WindowData) =
        if (data.processName.equals(processName, ignoreCase = true)) category else null
}

/**
 * Matches when processName contains [keyword] (case-insensitive, partial match).
 * Useful when the exact process name varies slightly by version or installation.
 * Example: condition="jetbrains", category=PRODUCTIVE — matches "jetbrains-idea", "jetbrains-clion", etc.
 */
class ProcessContainsRule(
    private val keyword: String,
    private val category: Category
) : CategorizationRule {
    override fun evaluate(data: WindowData) =
        if (data.processName.contains(keyword, ignoreCase = true)) category else null
}

/**
 * Marks a process as a browser-class application.
 * Unlike other rules this one returns AMBIGUOUS — the definitive category will come from
 * BrowserAnalyserEngine (OCR → BrowserOCRContext → DictionaryEngine URL lookup).
 *
 * This rule is also used as a signal: BrowserAnalyserEngine queries these rules on startup
 * to know which processes it should watch for and trigger screenshot captures on.
 *
 * Example: condition="firefox", category=AMBIGUOUS (overridden later by URL-based rules)
 */
class BrowserProcessRule(
    val processName: String,
    override val trigger: ActionTrigger? = null
) : CategorizationRule {
    override fun evaluate(data: WindowData) =
        if (data.processName.contains(processName, ignoreCase = true)) Category.AMBIGUOUS else null
}

/**
 * Matches when the window title (or URL, for browser context) matches a regex pattern.
 * Escape the condition string as a valid Kotlin/Java regex.
 * Example: condition="reddit\\.com\\/r\\/(gaming|memes)", category=DISTRACTING
 */
class RegexRule(
    pattern: String,
    private val category: Category
) : CategorizationRule {
    private val regex = Regex(pattern, RegexOption.IGNORE_CASE)
    override fun evaluate(data: WindowData): Category? {
        val combined = "${data.processName} ${data.windowTitle}"
        return if (regex.containsMatchIn(combined)) category else null
    }
}

// ---------------------------------------------------------------------------
// Engine
// ---------------------------------------------------------------------------

/**
 * Evaluates [rules] in insertion order against a [WindowData] snapshot.
 * Returns the first matching category, or UNCATEGORIZED if nothing matches.
 *
 * Rules are kept in RAM so evaluation is O(n) with zero DB I/O on the hot path.
 * Call [updateRules] whenever the DB changes (addRule / removeRule from the UI).
 */
data class CategoryMatch(val category: Category, val trigger: ActionTrigger?)

class DictionaryEngine(private var rules: List<CategorizationRule> = emptyList()) {

    fun updateRules(newRules: List<CategorizationRule>) {
        this.rules = newRules
    }

    fun categorize(windowData: WindowData): CategoryMatch {
        for (rule in rules) {
            val match = rule.evaluate(windowData)
            if (match != null) return CategoryMatch(match, rule.trigger)
        }
        return CategoryMatch(Category.UNCATEGORIZED, null)
    }

    /** Returns true if the given process name is registered as a browser-class app. */
    fun isBrowserProcess(processName: String): Boolean =
        rules.filterIsInstance<BrowserProcessRule>()
            .any { it.processName.equals(processName, ignoreCase = true) }
}
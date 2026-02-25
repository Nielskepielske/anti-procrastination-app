package com.example.procrastination_detection.globals

import com.example.procrastination_detection.models.db.Category
import com.example.procrastination_detection.models.db.Rule
import com.example.procrastination_detection.models.db.RuleLine
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object DefaultRules {

    @OptIn(ExperimentalUuidApi::class)
    fun getRules(): List<Rule> {
        // 1. Generate the Rule ID first so the RuleLines can reference it
        val baseRuleId = Uuid.random().toString()

        // 2. Create your categories
        val defaultCategory = Category(
            id = Uuid.random().toString(),
            name = "Default",
            isProductive = true
        )

        // 3. Create your rule lines, pointing to the Category and the Rule ID
        val baseRuleLine = RuleLine(
            id = Uuid.random().toString(),
            ruleId = baseRuleId,
            name = "Follow Default Category",
            category = defaultCategory,
            isProductive = defaultCategory.isProductive
        )

        // 4. Finally, assemble the Rule with its lines
        val baseRule = Rule(
            id = baseRuleId,
            name = "Base Rule",
            lines = listOf(baseRuleLine)
        )

        return listOf(baseRule)
    }
}
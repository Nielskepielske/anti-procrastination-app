package com.example.procrastination_detection.mappers

import com.example.procrastination_detection.models.db.Category
import com.example.procrastination_detection.models.db.Rule
import com.example.procrastination_detection.models.db.RuleLine
import com.example.procrastination_detection.models.db.dto.RuleLineWithCategory
import com.example.procrastination_detection.models.db.dto.RuleWithLines

// Convert RuleLineWithCategory -> RuleLine
fun RuleLineWithCategory.toDomain(): RuleLine {
    return RuleLine(
        id = this.line.id,
        ruleId = this.line.ruleId,
        name = this.line.name,
        isProductive = this.line.isProductive,
        category = Category(
            id = this.category.id,
            name = this.category.name,
            isProductive = this.category.isProductive
        ),
        maxConsecutiveSeconds = this.line.maxConsecutiveSeconds,
        maxTotalSeconds = this.line.maxTotalSeconds
    )
}

// Convert RuleWithLines -> Rule
fun RuleWithLines.toDomain(): Rule {
    return Rule(
        id = this.rule.id,
        name = this.rule.name,
        lines = this.lines.map { it.toDomain() }
    )
}
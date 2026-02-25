package com.example.procrastination_detection.models.db.dto

import androidx.room.Embedded
import androidx.room.Relation

// 1. Combine RuleLineEntity with its Category
data class RuleLineWithCategory(
    @Embedded val line: RuleLineEntity,
    @Relation(
        entity = CategoryEntity::class,
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity
)

// 2. Combine the Rule with its List of RuleLines
data class RuleWithLines(
    @Embedded val rule: RuleEntity,
    @Relation(
        entity = RuleLineEntity::class,
        parentColumn = "id",
        entityColumn = "ruleId"
    )
    val lines: List<RuleLineWithCategory>
)
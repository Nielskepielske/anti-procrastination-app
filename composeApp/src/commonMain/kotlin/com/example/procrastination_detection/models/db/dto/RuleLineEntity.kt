package com.example.procrastination_detection.models.db.dto

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("rule_lines")
data class RuleLineEntity(
    @PrimaryKey
    val id: String,
    val ruleId: String,
    val name: String,
    val categoryId: String,
    val isProductive: Boolean
)
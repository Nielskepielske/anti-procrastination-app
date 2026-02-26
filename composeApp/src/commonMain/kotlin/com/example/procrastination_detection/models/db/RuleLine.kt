package com.example.procrastination_detection.models.db

import androidx.room.Entity
import androidx.room.PrimaryKey

data class RuleLine(
    val id: String,
    val ruleId: String,
    val name: String,
    val category: Category,
    val isProductive: Boolean,
    val maxConsecutiveSeconds: Long,
    val maxTotalSeconds: Long
)

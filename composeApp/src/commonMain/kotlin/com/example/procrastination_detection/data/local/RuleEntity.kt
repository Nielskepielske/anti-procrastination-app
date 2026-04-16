package com.example.procrastination_detection.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.procrastination_detection.domain.model.Category

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleType: String,
    val condition: String,
    val category: Category,
    val triggerId: String? = null
)

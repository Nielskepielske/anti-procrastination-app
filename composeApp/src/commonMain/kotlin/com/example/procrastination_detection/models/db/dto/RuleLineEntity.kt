package com.example.procrastination_detection.models.db.dto

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "rule_lines",
    foreignKeys = [
        ForeignKey(
            entity = RuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.CASCADE // If rule is deleted, delete its lines!
        )
    ]
)
data class RuleLineEntity(
    @PrimaryKey val id: String,
    val ruleId: String,
    val name: String,
    val categoryId: String,
    val isProductive: Boolean,
    val maxConsecutiveSeconds: Long,
    val maxTotalSeconds: Long
)
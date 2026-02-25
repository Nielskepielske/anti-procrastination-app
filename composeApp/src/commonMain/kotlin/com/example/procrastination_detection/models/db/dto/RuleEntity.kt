package com.example.procrastination_detection.models.db.dto

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("rules")
data class RuleEntity(
    @PrimaryKey
    val id: String,
    val name: String,
)
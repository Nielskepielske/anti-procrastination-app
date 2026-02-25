package com.example.procrastination_detection.models.db

import androidx.room.Entity
import androidx.room.PrimaryKey

data class Rule(
    val id: String,
    val name: String,
    val lines: List<RuleLine>
);

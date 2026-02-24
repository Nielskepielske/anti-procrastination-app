package com.example.procrastination_detection.models.db

data class RuleLine(
    val id: String,
    val name: String,
    val category: Category,
    val isProductive: Boolean
)

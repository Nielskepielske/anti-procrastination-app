package com.example.procrastination_detection.models.db

import androidx.room.Entity
import androidx.room.PrimaryKey

data class Process(
    val id: String,
    val name: String,
    val category: Category
)

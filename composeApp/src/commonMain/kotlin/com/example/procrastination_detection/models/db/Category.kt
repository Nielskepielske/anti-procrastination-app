package com.example.procrastination_detection.models.db

import androidx.room.Entity
import androidx.room.PrimaryKey

data class Category(
    val id: String,
    val name: String,
    val isProductive: Boolean
)

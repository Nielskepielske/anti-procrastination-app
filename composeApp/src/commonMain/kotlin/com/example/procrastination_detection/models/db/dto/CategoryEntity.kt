package com.example.procrastination_detection.models.db.dto

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val isProductive: Boolean
)
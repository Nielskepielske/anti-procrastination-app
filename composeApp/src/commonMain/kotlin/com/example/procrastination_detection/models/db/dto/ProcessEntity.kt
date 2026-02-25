package com.example.procrastination_detection.models.db.dto

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "processes",
    indices = [Index(value = ["name"], unique = true)]
)
data class ProcessEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val categoryId: String
)
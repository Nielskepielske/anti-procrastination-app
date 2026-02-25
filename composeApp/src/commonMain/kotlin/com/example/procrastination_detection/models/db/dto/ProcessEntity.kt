package com.example.procrastination_detection.models.db.dto

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("processes")
data class ProcessEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val categoryId: String
)
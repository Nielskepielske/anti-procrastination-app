package com.example.procrastination_detection.models.db.dto

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("sessions")
data class SessionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val ruleId: String
)
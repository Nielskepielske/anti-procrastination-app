package com.example.procrastination_detection.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.procrastination_detection.domain.model.Category

@Entity(tableName = "inbox")
data class InboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contextStr: String, // E.g. "youtube.com" or "Discord"
    val timestampMs: Long,
    val discoveredByStrategy: String, // e.g. "KeywordMatcher", "LLM"
    val suggestedCategory: Category // Pre-fill UI recommendation
)
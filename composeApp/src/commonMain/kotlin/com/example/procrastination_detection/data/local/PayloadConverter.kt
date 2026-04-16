package com.example.procrastination_detection.data.local

import androidx.room.TypeConverter
import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.model.Category
import kotlinx.serialization.json.Json

class PayloadConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromPayload(payload: SensorPayload): String {
        return json.encodeToString(payload)
    }

    @TypeConverter
    fun toPayload(jsonString: String): SensorPayload {
        return json.decodeFromString<SensorPayload>(jsonString)
    }

    @TypeConverter
    fun fromCategory(category: Category): String {
        return category.name
    }

    @TypeConverter
    fun toCategory(name: String): Category {
        return try {
            Category.valueOf(name)
        } catch (e: Exception) {
            Category.UNCATEGORIZED // Safe fallback if something breaks
        }
    }
    @TypeConverter
    fun fromEscalationLevel(level: com.example.procrastination_detection.domain.model.EscalationLevel): String {
        return level.name
    }

    @TypeConverter
    fun toEscalationLevel(name: String): com.example.procrastination_detection.domain.model.EscalationLevel {
        return try {
            com.example.procrastination_detection.domain.model.EscalationLevel.valueOf(name)
        } catch (e: Exception) {
            com.example.procrastination_detection.domain.model.EscalationLevel.GENTLE
        }
    }
}
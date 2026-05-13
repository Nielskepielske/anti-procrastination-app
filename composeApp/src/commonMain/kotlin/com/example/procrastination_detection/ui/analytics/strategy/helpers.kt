package com.example.procrastination_detection.ui.analytics.strategy

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun formatBucketTimestamp(timestamp: Long, totalDurationMillis: Long): String {
    val timeZone = TimeZone.currentSystemDefault()
    val localDateTime = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone)

    val hour = localDateTime.hour.toString().padStart(2, '0')
    val minute = localDateTime.minute.toString().padStart(2, '0')
    val timeString = "$hour:$minute"

    return if (totalDurationMillis <= 86_400_000L) {
        // Less than 24 hours: "14:30"
        timeString
    } else {
        // More than 24 hours: "Apr 25, 14:30"
        val month = localDateTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
        "$month $day, $timeString"
    }
}
package com.example.procrastination_detection.domain.event

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class AggregatedPayload {

    @Serializable
    @SerialName("aggregated_mouse")
    data class Mouse(
        val totalHoverDurationMillis: Long,
        val totalClicks: Int,
        val totalDistanceTraveled: Double
    ) : AggregatedPayload()

    @Serializable
    @SerialName("aggregated_keyboard")
    data class Keyboard(
        val averageCadenceWpm: Int,
        val peakCadenceWpm: Int
    ) : AggregatedPayload()

    @Serializable
    @SerialName("aggregated_frequency")
    data class Frequency(
        val totalOccurrences: Int,
        val uniqueContexts: Int // E.g., how many unique apps or URLs were visited
    ) : AggregatedPayload()
}
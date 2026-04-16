package com.example.procrastination_detection.domain.discovery

import com.example.procrastination_detection.domain.model.Category

/**
 * A DiscoveryStrategy attempts to automatically classify an unknown application or URL
 * without requiring explicit user input.
 *
 * Implementations can range from simple keyword matching to cloud/local LLM calls.
 * Each strategy returns a [DiscoveryResult] indicating its confidence and suggested category.
 * The DiscoveryEngine picks the highest-confidence result and saves it to the Inbox.
 *
 * To add a new strategy, simply implement this interface and register it in Koin — the
 * DiscoveryEngine picks it up via getAll<DiscoveryStrategy>() with zero wiring needed.
 */
interface DiscoveryStrategy {
    val strategyName: String
    suspend fun classify(contextStr: String): DiscoveryResult
}

data class DiscoveryResult(
    val suggestedCategory: Category,
    /** 0.0 = no confidence, 1.0 = certain */
    val confidence: Float,
    val strategyName: String
)

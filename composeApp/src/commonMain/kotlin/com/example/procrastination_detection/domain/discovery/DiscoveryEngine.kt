package com.example.procrastination_detection.domain.discovery

import com.example.procrastination_detection.data.local.InboxDao
import com.example.procrastination_detection.data.local.InboxEntity
import com.example.procrastination_detection.domain.event.ProcessedEvent
import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.model.Category
import com.example.procrastination_detection.domain.pipeline.EventPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * The DiscoveryEngine is the gatekeeper for unknown applications and websites.
 *
 * It subscribes to the EventPipeline and watches for events where the DictionaryEngine
 * returns UNCATEGORIZED. When found, it runs all injected DiscoveryStrategies to obtain
 * an automatic classification suggestion, then saves the item to the InboxEntity table
 * for the user to optionally confirm or override later via the DictionaryHubScreen.
 *
 * Design principles:
 * - Strategies are injected via Koin (getAll<DiscoveryStrategy>()), making it trivial to
 *   add new classifiers (LLM, embedding-based, user-profile-trained) without touching this class.
 * - Items already present in the Inbox are skipped (de-duplication via InboxDao.exists()).
 * - All DB writes are fire-and-forget on Dispatchers.IO so they never block the event stream.
 */
class DiscoveryEngine(
    private val pipeline: EventPipeline,
    private val inboxDao: InboxDao,
    private val strategies: List<DiscoveryStrategy>,
    private val scope: CoroutineScope
) {
    fun startListening() {
        scope.launch {
            pipeline.processedEvents.collect { event ->
                if (event.category == Category.UNCATEGORIZED) {
                    val contextStr = event.toContextString()
                    if (contextStr.isNotBlank()) {
                        handleUnknown(contextStr, event.timestamp)
                    }
                }
            }
        }
    }

    private fun handleUnknown(contextStr: String, timestamp: Long) {
        scope.launch(Dispatchers.Default) {
            // Skip if already in inbox
            if (inboxDao.exists(contextStr)) return@launch

            // Run all strategies, pick the one with the highest confidence
            val bestResult = strategies
                .map { it.classify(contextStr) }
                .maxByOrNull { it.confidence }
                ?: return@launch

            inboxDao.insert(
                InboxEntity(
                    contextStr = contextStr,
                    timestampMs = timestamp,
                    discoveredByStrategy = bestResult.strategyName,
                    suggestedCategory = bestResult.suggestedCategory
                )
            )
        }
    }
}

/** Extracts a human-readable context string from a processed event for display and storage. */
private fun ProcessedEvent.toContextString(): String = when (val p = payload) {
    is SensorPayload.AppSwitch -> p.windowData.processName
    is SensorPayload.TitleChange -> "${p.windowData.processName}||${p.windowData.windowTitle}"
    is SensorPayload.BrowserOCRContext -> p.url
    else -> ""
}

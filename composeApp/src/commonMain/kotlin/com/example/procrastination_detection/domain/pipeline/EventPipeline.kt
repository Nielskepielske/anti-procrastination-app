package com.example.procrastination_detection.domain.pipeline

import com.example.procrastination_detection.domain.dictionary.DictionaryEngine
import com.example.procrastination_detection.domain.dictionary.CategoryMatch
import com.example.procrastination_detection.domain.event.ProcessedEvent
import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.model.Category
import com.example.procrastination_detection.domain.pipeline.stream.StreamInsight
import com.example.procrastination_detection.domain.pipeline.stream.StreamProcessingEngine
import com.example.procrastination_detection.domain.repository.SensorEventRepository
import com.example.procrastination_detection.domain.trigger.ActionTrigger
import com.example.procrastination_detection.models.db.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

class EventPipeline (
    private val dictionaryEngine : DictionaryEngine,
    private val repository: SensorEventRepository,
    private val streamEngine: StreamProcessingEngine
    ){
    // 1. The Intake Pipe (Sensors push data here)
    // extraBufferCapacity ensures we don't drop events if the DB is momentarily slow
    private val _rawEvents = MutableSharedFlow<SensorPayload>(extraBufferCapacity = 64)

    // Tracks the currently running Action Trigger so we can cleanly stop it when focus changes
    private var activeTrigger: ActionTrigger? = null

    // 2. The Broadcast Pipe (Timers, UI, and Interventions listen here)
    private val _processedEvents = MutableSharedFlow<ProcessedEvent>(extraBufferCapacity = 64)
    val processedEvents: SharedFlow<ProcessedEvent> = _processedEvents.asSharedFlow()

    // A new flow to broadcast real-time anomalies to the UI or notification system
    private val _streamInsights = MutableSharedFlow<StreamInsight>(extraBufferCapacity = 10)
    val streamInsights = _streamInsights.asSharedFlow()

    // Adding something to watch the current state
    private val _currentState = MutableStateFlow<ProcessedEvent?>(null)
    val currentState: StateFlow<ProcessedEvent?> = _currentState.asStateFlow()

    // 3. Sensor Entry Point
    suspend fun emitRawEvent(payload: SensorPayload)
    {
        _rawEvents.emit(payload)
    }

    // 4. The Processing Loop
    fun start(scope: CoroutineScope){
        scope.launch {
            _rawEvents.collect { payload ->
                val timestamp = Clock.System.now().toEpochMilliseconds()

                // Categorize based on payload type
                val categoryMatch = when (payload) {
                    is SensorPayload.AppSwitch -> {
                        println("AppSwitch")
                        dictionaryEngine.categorize(payload.windowData)
                    }
                    is SensorPayload.TitleChange -> {
                        println("Titlechange")
                        // Still categorise the new title — critical for browser apps where a title change
                        // means a new URL/website that needs evaluation (e.g., GitHub → Reddit).
                        dictionaryEngine.categorize(payload.windowData)
                    }
                    is SensorPayload.BrowserOCRContext -> {
                        // Route URL through the dictionary as a synthetic window lookup
                        val synthetic = com.example.procrastination_detection.domain.model.WindowData(
                            processName = "browser",
                            windowTitle = payload.url
                        )
                        dictionaryEngine.categorize(synthetic)
                    }
                    // Metrics payloads carry no inherent category — BehaviorAnalysisEngine reads them
                    is SensorPayload.MouseMetrics -> CategoryMatch(Category.NEUTRAL, null)
                    is SensorPayload.KeyboardMetrics -> CategoryMatch(Category.NEUTRAL, null)
                }
                
                val category = categoryMatch.category

                // --- Dynamic Sensor Lifecycle Management ---
                // Only evaluate lifecycle boundaries on parent-level context shifts
                if (payload is SensorPayload.AppSwitch || payload is SensorPayload.TitleChange) {
                    val newTrigger = categoryMatch.trigger
                    if (newTrigger?.id != activeTrigger?.id) {
                        println("\uD83D\uDD04 Boundary Shift: Stopping old trigger ${activeTrigger?.id}, Starting new trigger ${newTrigger?.id}")
                        activeTrigger?.stop()
                        newTrigger?.start()
                        activeTrigger = newTrigger
                    }
                }
                // -------------------------------------------

                println("Category: $category")

                // 2. LIVE SLIDING WINDOW ANALYSIS
                val insight = streamEngine.processLiveEvent(payload)
                if (insight != null) {
                    println("🚨 Insight Detected: ${insight.title}")
                    _streamInsights.emit(insight)

                    // Note: You could also save this insight to a new DB table here
                    // if you want to show "Anomalies Over Time" on your dashboard!
                }

                // Create the processed wrapper
                val processedEvent = ProcessedEvent(
                    timestamp = timestamp,
                    payload = payload,
                    category = category
                )

                _currentState.value = processedEvent

                // Save the raw payload to the Room Database (Fire and forget)
                launch {
                    repository.saveEvent(payload, timestamp)
                }

                // Broadcast the processed event to the rest of the app
                _processedEvents.emit(processedEvent)
            }
        }
    }

}
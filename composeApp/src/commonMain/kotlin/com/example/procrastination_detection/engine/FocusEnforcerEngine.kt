package com.example.procrastination_detection.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

class FocusEnforcerEngine {
    // Map of offending app titles to their aggression level
    private val _enforcedApps = MutableStateFlow<Map<String, Int>>(emptyMap())
    val enforcedApps = _enforcedApps.asStateFlow()
    
    // An event to trigger resetting all apps across the system
    private val _resetAllTrigger = MutableStateFlow(0)
    val resetAllTrigger = _resetAllTrigger.asStateFlow()

    private var currentDelayMs = 10_00L
    private var penaltyLevel = 1
    private var enforcementJob: Job? = null
    
    private var currentOffendingAppTitle: String? = null

    // Call this when distraction is detected
    fun startEnforcement(scope: CoroutineScope, appTitle: String) {
        currentOffendingAppTitle = appTitle
        // Add it to enforced apps if not present
        if (!_enforcedApps.value.containsKey(appTitle)) {
             _enforcedApps.value = _enforcedApps.value.toMutableMap().apply { put(appTitle, penaltyLevel) }
        }

        if (enforcementJob?.isActive == true) return

        enforcementJob = scope.launch {
            while (isActive) {
                delay(currentDelayMs)
                // Increment penalty for current active offending app
                currentOffendingAppTitle?.let { title ->
                   val currentLevel = _enforcedApps.value[title] ?: penaltyLevel
                   _enforcedApps.value = _enforcedApps.value.toMutableMap().apply { put(title, currentLevel + 1) }
                }

                currentDelayMs = max(500L, (currentDelayMs * 0.75).toLong())
                penaltyLevel++
            }
        }
    }

    private var lastUpdateTime = 0
    fun lessenPenalty(timeSinceLastProcrastination: Int){
        if(timeSinceLastProcrastination - lastUpdateTime > 5){
            penaltyLevel = max(0, penaltyLevel - 1)
            lastUpdateTime = timeSinceLastProcrastination
            
            // Lessen for enforced apps slowly
            val newEnforcedApps = _enforcedApps.value.toMutableMap()
            var changed = false
            newEnforcedApps.forEach { (title, level) ->
                if (level > 0) {
                    newEnforcedApps[title] = max(0, level - 1)
                    changed = true
                }
            }
            if(changed) {
                _enforcedApps.value = newEnforcedApps
            }
        }
    }

    // Call this when they finally go back to working (for a specific app context or in general)
    fun stopEnforcement() {
        enforcementJob?.cancel()
        currentDelayMs = 10_000L // Reset the trap
        penaltyLevel = 1
        currentOffendingAppTitle = null
    }

    // Called when session ends completely
    fun resetAllEnforcements() {
        stopEnforcement()
        _enforcedApps.value = emptyMap()
        _resetAllTrigger.value += 1
    }
}

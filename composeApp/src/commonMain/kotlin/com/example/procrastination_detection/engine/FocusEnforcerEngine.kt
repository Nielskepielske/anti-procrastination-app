package com.example.procrastination_detection.engine

import com.example.procrastination_detection.models.OverlayState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlin.math.max

class FocusEnforcerEngine {
    private val _overlayState = MutableStateFlow(OverlayState())
    val overlayState = _overlayState.asStateFlow()

    private var currentDelayMs = 10_00L
    private var penaltyLevel = 1
    private var enforcementJob: Job? = null

    // Call this when distraction is detected
    fun startEnforcement(scope: CoroutineScope) {
        if (enforcementJob?.isActive == true) return

        enforcementJob = scope.launch {
            while (isActive) { // Keeps running until they stop procrastinating
                // 1. Show the overlay
                _overlayState.value = OverlayState(isVisible = true, aggressionLevel = penaltyLevel)

                // 2. We pause this loop until the user clicks "Dismiss" (handled below)
                // waitForDismissal()

                // 3. User dismissed it! Now they must suffer the shrinking delay.
                delay(currentDelayMs)

                // 4. Make it faster and angrier for next time
                currentDelayMs = max(500L, (currentDelayMs * 0.75).toLong())
                penaltyLevel++
            }
        }
    }

    // The UI will call this when the user clicks the "Go Away" button
    fun onOverlayDismissed() {
        _overlayState.value = _overlayState.value.copy(isVisible = false)
    }

    private var lastUpdateTime = 0
    fun lessenPenalty(timeSinceLastProcrastination: Int){
        if(timeSinceLastProcrastination - lastUpdateTime > 5){
            penaltyLevel = max(0, penaltyLevel - 1)
            lastUpdateTime = timeSinceLastProcrastination
        }
    }

    // Call this when they finally go back to working
    fun stopEnforcement() {
        enforcementJob?.cancel()
        _overlayState.value = OverlayState(isVisible = false)
        currentDelayMs = 10_000L // Reset the trap
        penaltyLevel = 1
    }

    private suspend fun waitForDismissal() {
        // Suspend the coroutine until the state changes to hidden
        _overlayState.first { !it.isVisible }
    }
}
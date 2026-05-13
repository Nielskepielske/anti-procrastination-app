# Event Pipeline & Data Flow Architecture

Our robust event processing infrastructure is the backbone of the application. It guarantees multi-platform compatibility by strictly decoupling data acquisition (sensors) from data processing (the core "brain"). 

## Overview of the Flow

The system does not utilize direct method calls between sensors and UI interfaces. It's built as a **Reactive Data Pipeline**, heavily leveraging Kotlin Coroutines and `SharedFlow`. 

1. **Detection**: OS-specific components observe activity.
2. **Ingestion**: Raw events enter a buffer.
3. **Evaluation**: The internal dictionary assigns context.
4. **Broadcast**: The unified, normalized event is shared with all active subscribers.

---

## 1. The Intake Pipe (`SensorManager` -> `EventPipeline`)

The entry point of the pipeline is entirely generic. The OS-specific implementations never classify data; they only capture it. 

*   **`SensorManager`**: Takes the active `FocusProfile` and determines which `BehaviorSensor` instances should be enabled (e.g., `LinuxWindowTracker`, `MouseTracker`). 
*   **`SensorPayload`**: When a sensor detects something, it packages the observation into a `SensorPayload` (such as `SensorPayload.AppSwitch` containing `WindowData`).
*   **Emission**: Sensors push these raw payloads into the `EventPipeline` via `emitRawEvent(payload)`.

## 2. In-Memory Processing (`EventPipeline.start`)

Inside `EventPipeline.kt`, a dedicated coroutine loop acts as the clearinghouse.

1.  **Timestamping**: It tags the incoming payload with `Clock.System.now().toEpochMilliseconds()`.
2.  **Categorization Strategy**: It passes the raw payload details (like window title and program name) to the **`DictionaryEngine`**.
3.  **Local Rule Matching**: `DictionaryEngine` matches this against up-to-date user rules downloaded in RAM, spitting out a category enum (e.g., `PRODUCTIVE`, `DISTRACTING`, `AMBIGUOUS`).

## 3. Database Offloading (Fire & Forget)

As a secondary parallel processing routine, the original `SensorPayload` and timestamp are written directly to the `SensorEventRepository` as JSON via Room DB. This is non-blocking ("fire and forget"), ensuring the system's live reactions are never delayed by disk I/O. 

## 4. Broadcasting Output (`ProcessedEvent`)

Once the payload acquires a category, the pipeline wraps it into a final `ProcessedEvent(timestamp, payload, category)` and broadcasts it via `_processedEvents.emit(processedEvent)`.

The pipeline pushes this onto a `SharedFlow`, functioning as a radio broadcast. Multiple consumers can "tune in" to this flow independently:
*   **`FocusTimerEngine`**: Evaluates `DISTRACTING` streams against the focus threshold.
*   **`DashboardViewModel`**: Renders the current Category and app title dynamically to the UI.
*   **(Future) `AnalyticsAccumulator`**: Summarizes the spans into continuous blocks of productive/distracted time in the DB.

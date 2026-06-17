# Features & Core Architecture

This document maps out the domain logic layer that dictates how components initialize, interact, and how new functionality is securely integrated while respecting KMP boundaries.

## Kotlin Multiplatform (KMP) Abstraction Level

Our core principle is **zero operating system dependency** inside the specific tracking layers of `commonMain`. 

Instead of embedding `xdotool` logic or Linux notification libraries deep into the app logic, we rely entirely on Kotlin `expect/actual` function pairing or **Dependency Injected Plugins** provided at the platform boundary. All data, rules, triggers, and timers operate strictly on abstract models.

## The Koin Plugin Infrastructure

Rather than utilizing rigid "if OS = Linux -> Start AppTracker" blocks, the system uses Koin's dependency multi-binding capabilities (`getAll<T>()`). We build components dynamically out of whatever tools are handed over by the host platform module (`linuxModule`, `androidModule`, etc.).

### 1. The Sensor Network (`SensorManager`)
We define a universal `BehaviorSensor` interface in the common layer.
*   **Platform Specific Implementation**: Linux provides `LinuxWindowTracker`, Android would provide `AndroidUsageStatsTracker`. All of them execute their OS-specific mechanisms.
*   **The Injection Hookup**: During application boot, the OS supplies a set of implementations to Koin. Then, `SensorManager(availableSensors = getAll<BehaviorSensor>())` harvests them.
*   **Feature Scalability**: Need a keyboard cadence tracker? Just build a class implementing `BehaviorSensor`, push it into the Koin module array, and it's instantly recognized and controlled by the frontend dashboard. No hardcoding is necessary.

### 2. Intervention Logic (`InterventionManager`)
The logic applies identical principles to user reprimanding. 
When the `FocusTimerEngine` detects a procrastination threshold breach, it defers action to the `InterventionManager`. 
*   **Continuous Heat Engine**: The `FocusTimerEngine` uses a state machine to track an `aggressionScore`. Heat builds while distracted and cools down while productive. This prevents users from "cheating" the timer by quickly switching apps.
*   **Injection**: `InterventionManager` aggregates all injected `InterventionStrategy` tools (e.g., `LinuxNotificationStrategy`, `DynamicFadingStrategy`).
*   **Firing**: Based on user `FocusProfile` directives, it triggers the appropriate negative enforcement routines securely.
*   **Auditing**: When triggered, it emits a `SystemIntervention` back into the `EventPipeline` so it can be logged and plotted on analytics charts.

### 3. Session Management (`SessionManager` & `CsvExportEngine`)
Rather than tracking indefinitely and polluting the SQLite database with gigabytes of raw events, tracking is encapsulated in discrete sessions.
*   **State Machine**: Sessions can be started, paused, resumed, and stopped. When paused, the underlying sensors are halted but the session ID remains active.
*   **Orphan Handling**: If the app crashes, the active session is marked "orphaned" upon next boot. The UI blocks interactions until the user decides to resume or finish the crashed session, ensuring continuous, unbroken data when necessary.
*   **Archiving**: When a session completes, the `CsvExportEngine` compresses the raw JSON payload records into a CSV file and deletes them from the active SQLite database, guaranteeing O(1) performance for live tracking regardless of historical data depth.

---

## Data Schema & Storage Architecture 

To avoid the infamous "schema rot" issue as the project evolves, the `Room` SQlite implementation takes a flexible approach regarding incoming raw tracking metrics.

*   **Type Converters**: Instead of making wide MySQL tables with 20 null columns (e.g., `mouse_clicks`, `window_title`, `url`), `SensorEventEntity` has a generalized `payload_json` field. We serialize subclasses of `SensorPayload` on the fly. 
*   **State Alignment**: The UI configuration dashboards do not have to write manual signals. User rule creations in `RuleRepository` fire synchronous updates, mirroring the new database rows precisely into the low-latency `DictionaryEngine` RAM cache in real-time.

## UI Strategy

Using **Compose Multiplatform** integrated directly with Jetpack Navigation, the UI operates mostly decoupled. The ViewModels simply subscribe to either local `Flows` hosted by the `DictionaryEngine`/`SensorManager` to react to configuration states, or attach themselves to the `EventPipeline` for real-time reactivity (such as showing the tracked application immediately upon focus change).

## Analytics Processing Strategy

Our data analytics are handled by a multi-pillar processing architecture (Stream Processing for live anomalies, Batch Processing for compaction, and Presentation Processing for UI Math). See [Analytics & Processing Architecture](analytics_architecture.md) for a deep-dive into how data flows from rapid sensors into scalable visuals.

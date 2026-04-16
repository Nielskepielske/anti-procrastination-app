# Project Architecture & Recap: Procrastination-Aware Computing

## What is the app?
It is a privacy-first, multi-platform behavioral tracking and intervention system built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**.

While the initial deployment targets Linux desktops (via JVM), the architecture is explicitly designed to expand to other operating systems (macOS, Windows) and mobile platforms (Android/iOS). Ultimately, the app will support **shared data flows across local devices** (e.g., your phone detecting mindless scrolling and telling your desktop to trigger an intervention). It tracks not just websites, but **entire applications** (e.g., VS Code, Figma, Slack) to build a complete picture of the user's digital environment.

## What is the purpose of the app?
Standard app and website blockers are rigid and "dumb"—if you block YouTube, you cannot watch a coding tutorial; if you block Figma, you can't work on a design task.

The purpose of this system is to understand **context and ambiguity**. It dynamically categorizes digital behavior (Productive, Distracting, Ambiguous), respects user privacy by processing all rules and databases locally, and executes intelligent interventions (like fading windows or system nudges) only when the user crosses a specific procrastination threshold.

---

## How does it work? (The Core Loop)
The system relies on a continuous, reactive loop:
1. **The Eyes (Sensors):** Background daemons track system state. Currently, this uses Linux OS commands (`xdotool`) to track the exact process (e.g., `code`) and the window title (e.g., `StackOverflow`).
2. **The Brain (Dictionary Engine):** Raw data is fed into a fast, RAM-based categorization engine. It compares the data against user-defined rules stored in a local SQLite database to categorize the activity.
3. **The Muscle (Timer Engine):** If a `DISTRACTING` event occurs, a hidden countdown timer begins. If the user returns to a `PRODUCTIVE` task, the timer cancels.
4. **The Reflex (Interventions):** If the timer reaches the threshold defined by the active "Focus Profile," an `InterventionStrategy` fires to nudge the user back on track.

---

## Deep Dive: The Event Pipeline
Data does not move linearly; it moves reactively. The core of the app is built on Kotlin `SharedFlows` acting as a central nervous system.

* **Raw Payloads (`SensorPayload`):** When a sensor detects a change, it wraps the data in a Kotlin Sealed Class (e.g., `SensorPayload.AppSwitch(WindowData)`). This means the pipeline doesn't care *where* the data came from (desktop window, mouse movement, or a local network ping from a mobile phone).
* **The Intake & Processing:** The `EventPipeline` catches the raw payload, tags it with a standard Unix timestamp, and passes it to the `DictionaryEngine` for categorization.
* **The Processed Event:** The pipeline wraps the data into a `ProcessedEvent(timestamp, payload, category)` and broadcasts it on a public `SharedFlow`.
* **The Listeners:** Multiple parts of the app listen to this broadcast simultaneously without blocking each other. The `FocusTimerEngine` listens to control the countdown, the UI's `DashboardViewModel` listens to update the live screen, and the Room database listener secretly saves a log of it in the background.

---

## Deep Dive: Architecture & Choices for Flexibility
Every architectural choice was made to ensure the system is highly modular, scalable, and research-friendly.

### 1. Kotlin Multiplatform (KMP) Isolation
The codebase is strictly divided. The "Core Brain" lives in `commonMain` and knows absolutely nothing about operating systems, filesystems, or specific sensors. If we want to deploy to Android tomorrow, the Core Brain requires **zero code changes**. We simply write an Android-specific sensor in the `androidMain` module and inject it into the shared pipeline.

### 2. Koin Dependency Injection & Multi-Binding
Because this is a research tool, we need the ability to test different variables. We use Koin's `getAll()` multi-binding feature to create a "Plugin Architecture."
* We can write 10 different `BehaviorSensors` (Window Tracker, Mouse Tracker, Keyboard Cadence Tracker).
* We can write 10 different `InterventionStrategies` (OS Notification, Screen Opacity Fade, Sound Alert).
* When a user selects a `FocusProfile` in the UI, the `SensorManager` dynamically looks into the Koin toolbox, turns on the required sensors, and turns off the rest.

### 3. Generic Database Payloads
The Room SQLite database stores events as a generic JSON payload using Kotlin TypeConverters. This prevents "Schema Rot." If we introduce a completely new type of sensor data (e.g., heart rate from a smartwatch) next year, we do not have to migrate or rebuild our SQL tables. The database happily accepts and stores the new JSON string.

### 4. Cross-Device Scalability
Because the event pipeline accepts generic `SensorPayloads` and the UI is built entirely in Compose Multiplatform, the runway is clear for local networking. A future module can be added that listens to a local WebSocket, receives a payload from a companion mobile app, and injects it directly into the desktop's `EventPipeline` as if it were a local desktop event.

---

## Building Phases of the Application
* **Phase 1: The Core Brain** — Data structures, generic local Room database, dictionary logic, and the reactive `SharedFlow` event pipeline. *(Complete)*
* **Phase 2: Data Collectors** — Linux-specific `xdotool`/Hyprland app/window tracker, semantic `AppSwitch` vs `TitleChange` event splitting, and the dynamic `SensorManager`. *(Complete)*
* **Phase 3: The Timer, Analytics & Interventions** — Background countdown engine (`FocusTimerEngine`), `AnalyticsTimerEngine` accumulating daily usage data into `AppUsageEntity`, native OS notification strategies. *(Complete)*
* **Phase 4: The Discovery Pipeline** — `DiscoveryEngine` + `InboxEntity` for passive auto-suggestion, OCR-based `BrowserAnalyserEngine` with 4-step URL extraction fallback chain, `KeywordMatcherStrategy` baseline classifier. *(Complete)*
* **Phase 5: Dynamic Trigger Management** — `ActionTrigger` / `TriggerManager` architecture allowing heavy sensors (like the OCR engine) to lifecycle-bind cleanly to specific dictionary rules via Koin multi-binding. UI-exposed trigger selection in rule dialogs. *(Complete)*
* **Phase 6: Focus Profiles & Analytics Dashboard** — Persisting a real `FocusProfile` via DataStore, connecting it to `FocusTimerEngine` and `InterventionManager`, building the configurable Escalation Matrix UI, and rendering a live Analytics screen with Canvas-drawn charts. *(In Progress)*

## Current Progress
* The platform-agnostic KMP backend is fully operational.
* The local SQLite database (`Room`) is wired bi-directionally. When a user creates a rule in the UI, it saves to disk and instantly updates the live `DictionaryEngine` in RAM. Rules support 5 matching strategies: `TITLE_CONTAINS`, `PROCESS_EXACT`, `PROCESS_CONTAINS`, `BROWSER_PROCESS`, and `REGEX`.
* The `BrowserAnalyserEngine` is active: it takes screenshots, runs Tesseract OCR, and extracts URLs using a 4-step fallback chain (BoundingBox+TitleMatch → BoundingBox → FullImage+TitleMatch → FullImage). Results are emitted as `BrowserOCRContext` events into the main event pipeline.
* The `ActionTrigger` system allows user-created rules to lifecycle-bind heavy sensors. Creating a `BROWSER_PROCESS` rule with the `BrowserAnalysisTrigger` automatically starts/stops the OCR engine based on which app is in focus.
* The Compose UI features four main screens:
    * **Dashboard:** Live view of the active application and its real-time category.
    * **Dictionary Hub:** Full CRUD for rules (add, edit, delete, approve inbox candidates).
    * **Profile Manager:** Configuration stub — being upgraded to a fully wired screen in Phase 6.
    * **Analytics:** *(Coming in Phase 6)* — historical usage stats, focus score, and behavioral graphs.

## What are we working on next?
**Phase 6: Focus Profiles & Analytics Dashboard**
* **`SensorEventEntity` upgrade** — Adding a `payloadType` discriminator column so the DB can efficiently answer behavioral queries (e.g. switch frequency per hour) without deserializing JSON.
* **Real `FocusProfile` persistence** — Saving and loading the active profile via Jetpack DataStore. The `FocusTimerEngine` and `InterventionManager` will read this live instead of using hardcoded values.
* **Configurable Escalation Matrix** — Users assign which `InterventionStrategy` plugins fire at each level (GENTLE / FIRM / AGGRESSIVE) via a toggle UI.
* **Analytics Screen** — Canvas-drawn charts showing focus score, category breakdown, top apps by time, and app-switch frequency per hour.
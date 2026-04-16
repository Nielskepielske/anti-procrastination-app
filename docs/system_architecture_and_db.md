# System Architecture, Flow & Database Schematics

In true KMP plugin architectural fashion, the design enforces a massive separation of concerns. Modules scale securely and locally, accommodating multi-OS platforms despite distinct hardware needs (e.g., specific Linux xdotool/Wayland scrapers vs Android Accessibility Services).

## 1. App Execution Flow
The event processing architecture functions as a unidirectional river stream flowing through multiple specific engines:

1.  **Sensors**: Platform-specific listeners harvest data loops (e.g., `LinuxWindowTracker` gets foreground app; `BrowserAnalyserEngine` utilizes background OCR Coroutine). 
2.  **Ingestion**: They compile a `SensorPayload` subclass (e.g., `SensorPayload.BrowserOCRContext`) and throw it at the `EventPipeline`.
3.  **Filtration (The Dictionary Engine)**: The pipeline runs these raw strings through user-configured Dictionary Rules to apply a rigid Category tag (`PRODUCTIVE`, `DISTRACTING`).
    *   *If `UNCATEGORIZED`*: The payload is passed out to the `DiscoveryEngine` which uses strategies (ML / WordMatch) to auto-tag the app, registering it quietly in the `InboxEntity` table.
4.  **Behavior Analysis**: Processed streams run simultaneously into the `AnalyticsTimerEngine` (saving metrics quietly to DB) and the `BehaviorAnalysisEngine`.
5.  **Intervention & Re-routing**: If the analysis flags a procrastination event (e.g., Mindless Scroll), it fires to the `InterventionManager`, triggering ambient UI strategies (toasts, choice architectures).

---

## 2. Directory & Structure Logic

The overarching directory structures inside `commonMain`:
*   `domain/event/` ➝ Home to `SensorPayload` representing real-time system metrics. (The heartbeat).
*   `domain/pipeline/` ➝ The Engine Room containing isolated logic coroutines mapping specific goals (`FocusTimerEngine`, `BehaviorAnalysisEngine`, `AnalyticsTimerEngine`). They interact purely through observing flows.
*   `domain/intervention/` ➝ Home of Choice Architectures. Specific interface bounds for non-coercive redirection UI.
*   `data/local/` ➝ The single scalable entrypoint for the localized private SQLite database. Disconnected from Domain logic to ensure safe iteration.

---

## 3. Database Schemas (Room SQLite — Schema V5+)

Because privacy bounds mandate that extensive analytical logs exist solely locally, the architecture of the SQL database takes an expanding schema approach. The database version is bumped whenever a new table or column is added. Because `fallbackToDestructiveMigration(true)` is active during development, no explicit migration scripts are written at this stage — this will change before any production release.

> **Version history:** V1 (SensorEvents only) → V2 (+AppUsage) → V3 (+Inbox) → V4 (+triggerId on RuleEntity) → V5 (+payloadType on SensorEventEntity). 

### `SensorEventEntity` (Raw Event Log)
Every `SensorPayload` event emitted by the pipeline is persisted here for historical audit and behavioral analytics.
*   `timestamp`: Unix epoch milliseconds.
*   `payload`: Full serialized JSON of the `SensorPayload` sealed subclass (via `TypeConverter`).
*   `payloadType`: Plain string discriminator extracted at write-time (e.g. `"APP_SWITCH"`, `"TITLE_CHANGE"`, `"BROWSER_OCR"`, `"MOUSE_METRICS"`).

**Why `payloadType`?** The `payload` column is an opaque JSON blob. SQL cannot filter or group by the *type* of event without deserializing every row (full O(n) scan). By storing `payloadType` as an indexed plain column at write-time, we unlock O(log n) queries like "count all APP_SWITCH events between 14:00 and 15:00 on a given day" — the foundation for all per-hour behavioral stat graphs.

### `AppUsageEntity` (Daily Aggregate Analytics)
Replaces complex session mapping with simple metric aggregates. Written by `AnalyticsTimerEngine` on every context boundary shift.
*   `dayIndex`: Epoch day (`timestamp / 86400000`). Avoids any KMP timezone dependency.
*   `processName` & `windowTitle`: Primary composite keys for the UPSERT pattern.
*   `totalSeconds`: Monotonically incrementing counter. The DAO `incrementUsage()` does an UPDATE; 0 rows affected → INSERT fresh, avoiding expensive SELECT round-trips.

### `InboxEntity` (Pending Discovery Suggestions)
Holds unchecked auto-classified candidates from the `DiscoveryEngine`. Deliberately separate from `RuleEntity` so unreviewed AI suggestions never pollute the authoritative dictionary.
*   `contextStr`: The captured payload key (e.g. `"youtube.com/shorts"`).
*   `discoveredByStrategy`: Which classifier surfaced this (e.g. `"KEYWORD_MATCHER"`).
*   `suggestedCategory`: Pre-fills the decision in the UI for fast human review.

### `RuleEntity` (The Core Dictionary)
Where active user choices live. Drives the in-memory `DictionaryEngine` cache on boot and on every rule mutation.
*   `ruleType`: Matching strategy (`TITLE_CONTAINS`, `PROCESS_EXACT`, `PROCESS_CONTAINS`, `BROWSER_PROCESS`, `REGEX`).
*   `condition`: The phrase or pattern to search for.
*   `category`: `PRODUCTIVE`, `DISTRACTING`, or `AMBIGUOUS`.
*   `triggerId`: Optional ID of an `ActionTrigger` plugin. When set, the `EventPipeline` automatically calls `trigger.start()` / `trigger.stop()` at process-boundary shifts, enabling heavy sensors (like the OCR engine) to lifecycle-bind to specific app rules without hardcoding.

## 4. Focus Profiles & Escalation Matrix

The system supports **multiple named Focus Profiles** that the user can create, edit, delete, and switch between at runtime. This requires two distinct storage mechanisms.

### Why Room + DataStore (not just one or the other)?

The profile **catalogue** is a relational list: you query all profiles, upsert individual ones, and delete by ID. Room is designed for exactly this. Using DataStore for a list would require manual JSON serialization of the whole list on every write — fragile and non-atomic.

However, tracking *which* profile is currently active is a single global scalar (one ID string). Encoding that in Room would require a table with a single row and an enforced constraint to ensure only one profile is ever "active" — unnecessary complexity. DataStore's key-value model is purpose-built for exactly this kind of singleton preference.

**Result:**
- **Room `FocusProfileEntity`** — full CRUD catalogue of all profiles.
- **DataStore `active_profile_id`** — one string key. Switching profiles writes only this key.

### `FocusProfileEntity` Schema (Room)
*   `id`: UUID string primary key, generated at creation time.
*   `name`: Human-readable profile name (e.g. "Deep Work").
*   `thresholdMinutes`: How long the user must be in a DISTRACTING state before an intervention fires.
*   `escalationLevel`: Which level is currently active (`GENTLE`, `FIRM`, `AGGRESSIVE`).
*   `strategyMapJson`: JSON-serialized `Map<EscalationLevel, List<String>>` — maps each level to the list of `InterventionStrategy` IDs that fire at that level. Stored as JSON via TypeConverter (same pattern as `SensorPayload`).
*   `activeSensorIdsJson`: JSON-serialized `List<String>` — the `BehaviorSensor.id` values that should be running while this profile is active (e.g. `["WINDOW_TRACKER", "MOUSE_TRACKER"]`).

When the user switches the active profile, `FocusProfileRepository.setActiveProfile()` writes the new ID to DataStore and immediately calls `SensorManager.applyProfile(profile)`. `SensorManager` already contains this logic: it stops all sensors, then starts only those whose ID appears in `profile.requiredSensorIds`. The infrastructure was designed for exactly this use case — it just never had a real persisted profile to read from.

### The Escalation Matrix
Each escalation level maps to a **user-configurable list of `InterventionStrategy` IDs**:
```
GENTLE     → [LINUX_NUDGE]
FIRM       → [LINUX_NUDGE, OPACITY_FADE]
AGGRESSIVE → [LINUX_NUDGE, OPACITY_FADE, APP_KILLER]
```
These defaults are editable per-profile. New `InterventionStrategy` Koin registrations automatically appear in the Profile Manager UI's matrix checkboxes without any engine changes.

### The Intervention Chain
1. `LinuxWindowTracker` detects a DISTRACTING app → emits `AppSwitch`.
2. `EventPipeline` categorizes and broadcasts `ProcessedEvent(category=DISTRACTING)`.
3. `FocusTimerEngine` reads the **active profile** from `FocusProfileRepository` and starts a countdown of `profile.thresholdMinutes`.
4. If the user does not switch to a PRODUCTIVE app before expiry → `InterventionManager.trigger(profile)` is called.
5. `InterventionManager` resolves `profile.strategyMap[profile.escalationLevel]` and fires each strategy in sequence.

## 5. Trigger Management & Lifecycles
Many complex engines (like OCR) are far too heavy to run passively 24/7.
*   **The Problem:** Determining when to start them dynamically based on user configuration.
*   **The Solution:** `ActionTrigger` objects are injected into Koin. The central `TriggerManager` maps them. If a Dictionary Rule (`RuleEntity`) holds a `triggerId` string, the system dynamically hydrates the rule. When an active rule contains a trigger, the `EventPipeline` seamlessly initiates the heavy sensor upon process activation, minimizing RAM overhead.

---

## 6. Expandability Considerations
New evaluation models (like a secure ChatGPT payload scraper) will not necessitate refactoring the DB or Pipeline at all. To implement a new ML evaluation node, one merely creates a class `RemoteLLMEvaluationNode : DiscoveryStrategy` and binds it dynamically within Koin. The `DiscoveryEngine` will pick it up and process it asynchronously without modifying core pipelines.

Similarly, new `InterventionStrategy` implementations (e.g. `WindowShakeStrategy`, `SoundAlertStrategy`) are registered in Koin and automatically appear in the Profile Manager's escalation matrix configurator without touching any engine code.

# Project Changelog

All notable architectural and implementation changes are recorded here, grouped by session/phase.

---

## Phase 6 — Focus Profiles & Behavioral Analytics (2026-04-16)

**Problem**: The system was hardcoded to a single "distraction vs productive" threshold and lacked a way for users to visualize their behavior over time or choose different enforcement intensities.

**Solution**: Implemented a complete Focus Profile system with an Escalation Matrix and a dedicated Analytics Dashboard with live heartbeat updates.

| File | Status | Reason |
|---|---|---|
| `domain/model/FocusProfile.kt` | **NEW** | Core profile model. Defines name, `thresholdMinutes`, and a `strategyMap` (Escalation Matrix) connecting `GENTLE`/`FIRM`/`AGGRESSIVE` levels to specific intervention IDs. |
| `data/local/FocusProfileEntity.kt` | **NEW** | Room entity for profiles. Uses JSON serialization for the complex `strategyMap` and `requiredSensorIds` list. |
| `data/local/ActiveProfileStore.kt` | **NEW** | KMP DataStore implementation (actuals in `jvmMain`). Persists the `active_profile_id` across app restarts. |
| `domain/repository/FocusProfileRepository.kt` | **NEW** | Mediates between Room and DataStore. Automatically triggers `SensorManager.applyProfile()` when the active profile changes. |
| `ui/AnalyticsScreen.kt` | **NEW** | Comprehensive M3 dashboard. Features: Circular Focus Score gauge, Time Distribution bar, Top Activities list (with `formatDuration` for seconds-level granularity), and an Area Chart for App Switch frequency. |
| `domain/pipeline/AnalyticsTimerEngine.kt` | **MODIFIED** | Added a **30-second heartbeat** and `Mutex` locking. Usage data now updates live in the database and UI without requiring a window switch. |
| `domain/pipeline/FocusTimerEngine.kt` | **MODIFIED** | Now dynamically reads the `thresholdMinutes` from the active profile instead of a hardcoded 5-minute constant. |
| `domain/intervention/InterventionManager.kt` | **MODIFIED** | Now accepts a `FocusProfile` instead of a singular strategy. Executes precisely the strategies mapped to the current escalation level. |
| `di/CoreModule.kt` | **MODIFIED** | Registered `FocusProfileDao`, `ActiveProfileStore`, `FocusProfileRepository`, and `AnalyticsViewModel`. Fixed missing `SensorEventDao` registration. |

**Bug Fixes & Polish**:
- **KMP Compatibility**: Replaced `java.util.UUID` with a random string generator in `commonMain` to ensure non-JVM targets compile.
- **Type Safety**: Fixed `AnalyticsViewModel` to use `WindowData` objects for categorization and resolved `CategoryMatch` property access issues.
- **UI UX**: Added `formatDuration` helper to show `5s`, `1m 20s` etc., preventing short activities from showing as `0m`.
- **DI Stability**: Resolved `NoDefinitionFoundException` for several DAOs in the Koin graph.

---

## Phase 5 Addendum — OCR Fallback Chain & URL Accuracy (2026-04-16)

**Problem**: The `BrowserAnalyserEngine` was taking full-screen screenshots and feeding the entire image to Tesseract, which caused two major accuracy issues: (1) any text on the webpage or in tab titles that matched the URL pattern got mistakenly emitted as an active URL (e.g. `niels.bus` from an email address in the tab bar), and (2) the `titleMatch` helper only checked the first domain segment, causing mismatches for compound subdomains like `kotlinlang.org` against a title containing just `"kotlin"`.

**Solution**: Replaced the single-pass OCR with a 4-step escalating fallback chain and added an in-memory image crop, all inside `LocalUrlExtractor.jvm.kt` without touching the OS-layer screenshot functions.

| File | Status | Reason |
|---|---|---|
| `helpers/LocalUrlExtractor.kt` | **MODIFIED** | Refactored `extractUrlWithRegex` into `extractAllUrlsWithRegex` which returns all found candidates (deduplicated list). Updated `expect` signature to accept `windowTitle: String?` for cross-reference matching. |
| `helpers/LocalUrlExtractor.jvm.kt` | **REPLACED** | Full rewrite. Implements the 4-step chain: (1) BoundingBox+TitleMatch, (2) BoundingBox Only, (3) FullImage+TitleMatch, (4) FullImage Only. In-memory crop via `BufferedImage.getSubimage()` targets the top 15% of the screen. `titleMatch` now checks ALL domain segments, not just the first. Added `hasKnownTld()` allowlist that rejects OCR noise like `.bus`, `.bar` and other non-existent TLDs. |
| `helpers/LocalUrlExtractor.android.kt` | **MODIFIED** | Updated actual signature to match new `expect` contract. Marked as `TODO` for future ML Kit crop implementation. |
| `engine/BrowserAnalyserEngine.kt` | **MODIFIED** | `trackingLoop` now reads `pipeline.currentState.value` to extract the last known window title and passes it to the extractor. `BrowserOCRContext.windowTitle` is now the real browser title rather than a duplicate of the URL. |

---

**Problem**: The `BrowserAnalyserEngine` was started manually, but the application lacked a clean architectural method to let the user specify exactly *when* an engine should spin up based on their explicit application rules. Direct string connections were brittle and non-scalable for future complex engines.

**Solution**: Re-architected resolution patterns using a decoupled `ActionTrigger` interface grouped behind a unified `TriggerManager`. The UI can dynamically query these triggers and link them directly to created dictionary rules (`RuleEntity`).

| File | Status | Reason |
|---|---|---|
| `domain/trigger/ActionTrigger.kt` | **NEW** | Added core `ActionTrigger` and `IAmbiguousActionTrigger` interfaces. Replaces old static resolvers. Dictates `start()` and `stop()` lifecycle events. |
| `domain/trigger/TriggerManager.kt` | **NEW** | Centralized Koin injected lookup manager. Maps all injected `ActionTrigger` plugins ensuring the viewmodel has reactive access to the system array. |
| `data/local/RuleEntity.kt` | **MODIFIED** | Added a nullable `triggerId: String?`. Rules now explicitly host structural lifecycle hooks when stored in the database. |
| `ui/DictionaryHubScreen.kt` | **MODIFIED** | Add/Edit Rule dialogs conditionally render an intelligent "Sensor Trigger" dropdown. It dynamically renders ONLY the triggers compliant with the selected Rule's `Category`. |
| `di/CoreModule.kt` | **MODIFIED** | Koin graph updated to spawn `BrowserAnalyserEngine`, hook it into `BrowserAnalysisTrigger`, and register the `TriggerManager`. |

---

## Phase 4 Addendum — Rule Editing & Dashboard Live Connection (2026-04-16)

**Problem**: Rules were immutable once created, requiring deletion and re-creation to make changes. Also, the Dashboard was only updating on full `AppSwitch` events, missing intra-app title changes (like browser tabs), and rules weren't loading automatically on app startup.

**Solution**: Added a full CRUD (Create, Read, Update, Delete) cycle for rules and unified the Dashboard's state mapping to handle all payload types and engine-resolved categories.

| File | Status | Reason |
|---|---|---|
| `ui/DictionaryHubScreen.kt` | **MODIFIED** | Added `EditRuleDialog` and an edit icon to each saved rule card. This uses the same flexible `RuleType` dropdown as creation, allowing you to change both the condition and the matching strategy in-place. |
| `domain/repository/RuleRepository.kt` | **MODIFIED** | Added `updateRule()` — implemented as an atomic delete + insert to maintain simplicity while supporting the new UI editing flow. |
| `ui/DashboardViewModel.kt` | **MODIFIED** | Updated `currentAppFlow` and `currentCategoryFlow` to reactive map `pipeline.currentState`. Now handles `TitleChange` and `BrowserOCRContext`, ensuring the Dashboard title updates when you switch browser tabs or an OCR URL is detected. |
| `ui/DashboardScreen.kt` | **MODIFIED** | Shifted categorization color logic from string-based to enum-based (`Category`). The "Current Focus" badge now reflects the real-time category name even for `UNCATEGORIZED` or `AMBIGUOUS` items. |
| `main.kt` | **MODIFIED** | Added an initialization block that pre-loads rules from the database into the `DictionaryEngine` before the `EventPipeline` starts. Categorization now works instantly on first boot. |

---

## Phase 4 Addendum — Semantic Event Splitting (2026-04-16)

**Problem**: The tracker was using `class||title` as a single composite key, meaning an IntelliJ file switch looked identical to switching from IntelliJ to Firefox. This caused fragmented 2-second analytics segments for every file the user opened inside their IDE.

**Solution**: Split the composite key into two separate payloads, each carrying distinct semantic meaning. The tracker itself is now responsible for knowing *what changed* and emitting the precise type.

| File | Status | Reason |
|---|---|---|
| `domain/event/SensorPayload.kt` | **MODIFIED** | Added `TitleChange(windowData)` as a new sealed subclass. A title change means the window title changed but within the **same process**. The Kotlin sealed `when` compiler now enforces that every engine handles both `AppSwitch` and `TitleChange` explicitly, preventing silent bugs when new payload types are introduced. |
| `sensor/LinuxWindowTracker.kt` | **MODIFIED** | Replaced single `lastWindowClassTitle` string with two independent state vars: `lastProcessName` and `lastWindowTitle`. Uses a Kotlin `when` expression to determine what changed: **process boundary** → `AppSwitch`; **title-only change** → `TitleChange`; **no change** → no emission. |
| `domain/pipeline/EventPipeline.kt` | **MODIFIED** | Added `TitleChange` case: routed through `DictionaryEngine` the same as `AppSwitch`. This is intentional — a browser title change from `github.com` to `reddit.com` must still be categorised. |
| `domain/pipeline/AnalyticsTimerEngine.kt` | **MODIFIED** | `AppSwitch` closes and opens a timer segment (process boundary). `TitleChange` only updates `currentWindowTitle` — **no segment is closed**. Result: switching files in IntelliJ adds time to the IntelliJ segment, not a new micro-segment per file. |
| `domain/discovery/DiscoveryEngine.kt` | **MODIFIED** | `toContextString()` now maps `AppSwitch` to just the process name (for app-level inbox entries) and `TitleChange` to `processName||title` (for specific window/URL context). |

---

## Phase 3 — Analytics Layer & Dictionary UI (2026-04-16)

### 3.1 Analytics Data Layer
**Part of**: Analytics / Database

| File | Status | Reason |
|---|---|---|
| `data/local/AppUsageEntity.kt` | **NEW** | Lightweight table replacing the old heavy `Session`/`MonitoredProcess` schema. Stores daily app duration using `dayIndex` (epoch day), `processName`, `windowTitle`, and `totalSeconds`. Using epoch-day as an integer avoids any KMP timezone dependency. |
| `data/local/AppUsageDao.kt` | **NEW** | Room DAO for `AppUsageEntity`. Key method: `incrementUsage()` does an SQL UPDATE — if 0 rows updated we INSERT fresh, avoiding separate SELECT round-trips. |
| `data/local/AppDatabase.kt` | **MODIFIED** | Version bumped 1→2, `AppUsageEntity` added to entity list. Destructive migration enabled via `.fallbackToDestructiveMigration(true)` in `LinuxDatabaseBuilder.kt` since we are still in rapid prototyping. |

### 3.2 AnalyticsTimerEngine
**Part of**: Domain / Pipeline

| File | Status | Reason |
|---|---|---|
| `domain/pipeline/AnalyticsTimerEngine.kt` | **NEW** | Subscribes to `pipeline.processedEvents`. On every `AppSwitch`, calculates `(currentTimestamp - lastTimestamp) / 1000` seconds spent on the *previous* app, then UPSERTs into `AppUsageEntity`. Guards against impossible durations (sleep/suspend artefacts, <1s micro-switches). |
| `di/CoreModule.kt` | **MODIFIED** | `AppUsageDao` and `AnalyticsTimerEngine` registered as Koin `single` instances. |
| `main.kt` | **MODIFIED** | `analyticsTimerEngine.startListening()` called after `eventPipeline.start()` to begin accumulation immediately on boot. |

### 3.3 Dictionary Hub UI Revamp
**Part of**: UI

| File | Status | Reason |
|---|---|---|
| `ui/DictionaryViewModel.kt` | **MODIFIED** | `categorizeApp()` now passes `isExactProcess` to `ruleRepository.addRule()`. Fake static inbox list retained temporarily (replaced in Phase 4). |
| `ui/DictionaryHubScreen.kt` | **REPLACED** | Full rewrite with Material 3 ElevatedCards, dual Productive/Distracting action buttons in inbox, FAB + AlertDialog for manual rule addition, and colour-coded category pills in the saved rules tab. |

---

## Phase 4 — Discovery Engine & Browser OCR Integration (2026-04-16)

### 4.1 SensorPayload Expansion
**Part of**: Domain / Events

| File | Status | Reason |
|---|---|---|
| `domain/event/SensorPayload.kt` | **MODIFIED** | Replaced the placeholder `MouseActivity` class with three new concrete types: `BrowserOCRContext(url, windowTitle)` for OCR-sourced events; `MouseMetrics(hoverDurationMillis, clicks, distance)` for future behavioural analysis; `KeyboardMetrics(cadenceWpm)` for typing cadence detection. **Sealed classes** are used deliberately so Kotlin's exhaustive `when` expression forces every new payload type to be handled in the `EventPipeline`. |

### 4.2 EventPipeline — Expanded Categorisation
**Part of**: Domain / Pipeline

| File | Status | Reason |
|---|---|---|
| `domain/pipeline/EventPipeline.kt` | **MODIFIED** | `when(payload)` block updated to handle all four concrete payload types. `BrowserOCRContext.url` is fed through the `DictionaryEngine` as a *synthetic* `WindowData(processName="browser", windowTitle=url)`, allowing existing Title-Contains rules to match URLs without any special casing. `MouseMetrics`/`KeyboardMetrics` emit `NEUTRAL` — they are read by the `BehaviorAnalysisEngine` (Phase 5), not categorised individually. |

### 4.3 Inbox Database
**Part of**: Data / Database

| File | Status | Reason |
|---|---|---|
| `data/local/InboxEntity.kt` | **NEW** | Separate table from `RuleEntity` by design — the Inbox holds *unreviewed candidates* surfaced by the `DiscoveryEngine`. Keeping it distinct prevents unchecked AI suggestions from polluting the authoritative dictionary until the user explicitly approves them. Fields: `contextStr`, `timestampMs`, `discoveredByStrategy`, `suggestedCategory`. |
| `data/local/InboxDao.kt` | **NEW** | Uses `OnConflictStrategy.IGNORE` on insert to naturally deduplicate repeated sightings of the same app. Exposes `exists(contextStr)` for a fast pre-insert guard. |
| `data/local/AppDatabase.kt` | **MODIFIED** | Version bumped 2→3, `InboxEntity` registered, `inboxDao()` abstract accessor added. |

### 4.4 Discovery Strategy System
**Part of**: Domain / Discovery

| File | Status | Reason |
|---|---|---|
| `domain/discovery/DiscoveryStrategy.kt` | **NEW** | The extensibility interface. Any new classifier (local LLM, embeddings, user-profile heuristics) implements `classify(contextStr): DiscoveryResult` and is registered in Koin. The `DiscoveryEngine` picks it up via `getAll<DiscoveryStrategy>()` — no engine changes needed. Returns a `confidence: Float` so multiple strategies can compete and the best result wins. |
| `domain/discovery/KeywordMatcherStrategy.kt` | **NEW** | The zero-dependency local baseline strategy. Curated lists for distracting (YouTube, Reddit, etc.) and productive (GitHub, StackOverflow, etc.) domains. Confidence scales with keyword length — longer specific match = higher confidence. Returns `Category.AMBIGUOUS` with 0.0 confidence when truly unknown, signalling the `DiscoveryEngine` to still save it for manual human review. |

### 4.5 The DiscoveryEngine
**Part of**: Domain / Discovery

| File | Status | Reason |
|---|---|---|
| `domain/discovery/DiscoveryEngine.kt` | **NEW** | Subscribes to `pipeline.processedEvents`. On every `UNCATEGORIZED` event, extracts the `contextStr` (process+title for AppSwitch, URL for BrowserOCRContext), deduplicates via `InboxDao.exists()`, runs all strategies in parallel, picks the highest-confidence result, and inserts an `InboxEntity`. Running strategies on `Dispatchers.Default` keeps this non-blocking with respect to the main event stream. |
| `di/CoreModule.kt` | **MODIFIED** | `InboxDao`, `KeywordMatcherStrategy` (as `DiscoveryStrategy`), and `DiscoveryEngine` registered as Koin `single` instances. Strategies use multi-binding (`single<DiscoveryStrategy>`), enabling future strategies to be added without touching this engine. |
| `main.kt` | **MODIFIED** | `discoveryEngine.startListening()` called on boot. |

### 4.6 BrowserAnalyserEngine — Pipeline Integration
**Part of**: Engine / Sensors

| File | Status | Reason |
|---|---|---|
| `engine/BrowserAnalyserEngine.kt` | **MODIFIED** | Added `EventPipeline` as a constructor parameter. Replaced the `TODO` and `println` in `trackingLoop()` with `pipeline.emitRawEvent(SensorPayload.BrowserOCRContext(...))`. The OCR-extracted URL now enters the same reactive stream as all other sensor data, making it fully categorisable and discoverable with zero special-casing. The engine retains its own `CoroutineScope` on `Dispatchers.Default` to keep heavy OCR work isolated from the main pipeline execution. |

### 4.7 Dictionary Hub — Live Inbox
**Part of**: UI

| File | Status | Reason |
|---|---|---|
| `ui/DictionaryViewModel.kt` | **REPLACED** | `inboxFlow` now backs onto `inboxDao.getAllUnreviewedFlow()` — a real live Room query — replacing the hardcoded fake list. New methods: `approveInboxItem(item, category)` saves a rule and deletes the inbox entry atomically; `dismissInboxItem(item)` deletes without saving. `addManualRule()` replaces `categorizeApp()` for the FAB dialog path. |
| `ui/DictionaryHubScreen.kt` | **REPLACED** | `InboxContent` now renders `InboxEntity` objects. Each card shows the auto-suggested category with colour-coded dot, the originating strategy name, and three actions: Productive, Distracting, or Dismiss. Saved rules display `ruleType` in human-readable form. |

---

## Documentation Updates (2026-04-16)

| File | Status | Reason |
|---|---|---|
| `docs/project_philosophy_and_roadmap.md` | **NEW** | Core mission doc. Explains the "procrastination as signal" philosophy, the Escalation Matrix (gentle → aggressive opt-in), local privacy guarantees, and the 6-phase roadmap. |
| `docs/system_architecture_and_db.md` | **NEW** | Technical deep-dive into the unidirectional data flow (Sensor → Pipeline → Analysis → Intervention), directory logic, and all Room DB schemas with rationale for design choices. |
| `docs/event_pipeline_data_flow.md` | **NEW** | Detailed walkthrough of the reactive SharedFlow pipeline: intake, filtration, DB offloading, and broadcasting. |
| `docs/features_and_architecture.md` | **NEW** | KMP abstraction layer explanation, Koin plugin infrastructure, and sensor/intervention scalability patterns. |

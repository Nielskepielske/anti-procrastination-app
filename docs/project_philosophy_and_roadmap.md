# Project Philosophy & Evolutionary Roadmap

This document serves as the foundational text for the overarching vision of the Procrastination Detection project. It explicitly breaks away from conventional application-blocking paradigms, opting instead for a pattern-based, choice-oriented architecture.

## 1. Core Goal & Philosophy
The core tenet of this project is clear: **Procrastination is not a problem to be forcefully destroyed, but rather a behavioral signal we embrace and channel into positive activities.**

### 1.1 Empathy Over Punishment
Traditional focus applications work via coercion (e.g., locking access to specific applications, aggressive visual overlays). This project introduces **Intelligent Re-Routing**. When behavior identifies as a procrastination episode, the system aims to dynamically present alternatives—such as suggesting a specific reading chapter or a relevant YouTube video—while maintaining the user's ultimate autonomous control. 

### 1.2 Choice Architecture & Ambient Notifications
To implement re-routing, the application utilizes "Choice Architecture." Interventions must never interrupt the workflow detrimentally by default. A key implementation is the use of *Ambient Notifications* (e.g., quiet toasts) allowing users to easily ignore or click into a more productive state without the stress of an opaque screen blocker.

### 1.3 The Escalation Matrix (Opt-In Aggressive Blocking)
While the default stance is empathetic re-routing, the architecture fully supports **Aggressive Blocking** through the `InterventionManager`. 
Because interventions execute as decoupled `InterventionStrategy` plugins, the system empowers the user to configure a *Progressive Escalation Matrix* via their Focus Profile:
*   **Level 1 (Gentle)**: Detects mindless scrolling ➝ Triggers `AmbientToastStrategy`.
*   **Level 2 (Firm)**: User ignores toasts for 10 minutes ➝ Triggers `ChoiceDialogStrategy` overlay.
*   **Level 3 (Aggressive Lockout)**: User explicitly requests rigid adherence ➝ Triggers `AggressiveBlockerStrategy` (Opaque un-closable screen overlay killing the app).
This ensures the primary philosophical choice architecture is maintained, while the "nuclear option" remains structurally viable for those who specifically opt-in.

### 1.4 Local Privacy Primary
Given the highly sensitive nature of active behavioral sensing (screen content, typing cadence), a foundational goal is that **all possible compute is kept strictly local** (e.g., local SQLite DBs, local Kotlin algorithms, local OCR extractions via `BrowserAnalyserEngine`).

---

## 2. Key Features

1.  **Multi-Dimensional Behavioral Sensing**: Tracks more than simple "App X is open". By absorbing keyboard cadence, excessive empty mouse hover detection, and specific URL tab scraping (via Screen OCR), the application senses the "quality" of interaction.
2.  **Ambiguity Classification (AI/Heuristics)**: When an app or website is unknown (Category: `AMBIGUOUS`), discovery strategies like word-matchers or secure LLM-prompts evaluate the window to tentatively categorize it into the Inbox for quick user review.
3.  **Dynamic Adaptive Interventions**: The choice architecture observes feedback. If a user always ignores "Ambient Notification A" but clicks "Suggestion Alternative B", the system learns to prioritize Strategy B.

---

## 3. Product Roadmap

### Phase 1: Engine Foundation ✅
- Abstract Kotlin Multiplatform setup (Shared logic, external OS hooks).
- Event Pipeline baseline functioning (SharedFlow processing).

### Phase 2: Sensor Ingestion & Categorization ✅
- Linux-specific `AppSwitch` trackers (Hyprland `getActiveApp`).
- Semantic event splitting: `AppSwitch` vs `TitleChange` for analytics granularity.
- Basic Dictionary mapping `ProcessName` or `WindowTitle` to `PRODUCTIVE` or `DISTRACTING`.

### Phase 3: Analytics & Manual Administration ✅
- Dynamic database implementations registering exact duration metrics via the `AnalyticsTimerEngine`.
- Comprehensive generic UI (`DictionaryHubScreen`) enabling immediate user overrides on categorizations.
- Full CRUD cycle for rules (Create, Edit, Delete).

### Phase 4: Dynamic Classification & Ambiguity Handling ✅
- `DiscoveryEngine` + `InboxEntity` for passive auto-suggestion of new rules.
- Browser OCR (`BrowserAnalyserEngine`) feeding URLs as first-class `SensorPayload.BrowserOCRContext` events.
- `KeywordMatcherStrategy` as baseline local classifier.
- 4-step OCR fallback chain with TLD validation and title cross-referencing.

### Phase 5: Dynamic Trigger Management ✅
- `ActionTrigger` interface + `TriggerManager` registry to cleanly control heavy sensor lifecycle.
- `BrowserAnalysisTrigger` auto-starts the OCR engine when a matching browser rule is active.
- UI-exposed trigger dropdown in Add/Edit Rule dialogs — only shows triggers compatible with the rule's category.
- `triggerId` persisted in `RuleEntity` and hydrated back into live `CategorizationRule` at engine load time.

### Phase 6: Focus Profiles & Analytics Dashboard (Current Focus)
- Implement a persisted `FocusProfile` model (threshold, escalation level, sensor selection).
- Build a real Analytics screen showing daily productivity statistics from `AppUsageEntity` data.
- Connect `FocusTimerEngine` to the active profile's threshold to drive interventions.

### Phase 7: Behavioral Pattern Detection
- Introducing the `BehaviorAnalysisEngine`.
- Detecting non-linear procrastination concepts: The *Mindless Scroll*, the *Rapid Tab Hop*, and *Slow Cadence Drift*.

### Phase 8: Gentle Re-Routing System
- Replacing standard opaque layer enforcement with Ambient Notification strategies.
- Establishing an intent-feedback loop to track which suggestions physically worked and which were ignored.

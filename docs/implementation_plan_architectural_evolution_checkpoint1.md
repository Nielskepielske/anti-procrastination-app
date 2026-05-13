# Architectural Evolution: Ambient Behaviors & OCR

## Vision & Goal
The objective is to establish an ecosystem that treats procrastination not as a crime to be rigidly blocked, but as a behavioral signal to gently channel back into productivity. This architecture must remain profoundly flexible (scalable sensors, swappable strategies) and operate with absolute local privacy.

## Re-Routing Principles
- **No Forceful Blocking**: The system maps behaviors, not just apps.
- **Ambient Choice**: Use ambient choice architecture (polite toasts/suggestions) crafted by extensible algorithms or LLMs, letting the user optionally pivot without losing workflow context.

## Proposed Changes

### Component 1: Expansive Sensor Pipeline & The Browser Analyser
Our flexible `SensorPayload` hierarchy is designed exactly for this. We will integrate your locally-processed OCR screen-scraping logic.

#### [MODIFY] `SensorPayload.kt`
*   Extend the sealed classes:
    *   `AppSwitch(windowData)`
    *   `BrowserOCRContext(url, extractedTabName)`: This payload is independently emitted by the existing `BrowserAnalyserEngine` whenever it successfully extracts a URL.
    *   `MouseMetrics(hoverDuration, clicks)`
    *   `KeyboardMetrics(cadenceWpm)`

#### [MODIFY] `BrowserAnalyserEngine.kt`
*   Instead of simply printing the URL to console upon OCR extraction, it will construct a `SensorPayload.BrowserOCRContext` and invoke `eventPipeline.emitRawEvent()`. This weaves the browser's context effortlessly into our central, reactive event stream for categorization alongside active window titles.

---

### Component 2: Dynamic Registry & Discovery (The Inbox)
To seamlessly learn about new applications or websites without constant manual input, the pipeline will capture unknown variables automatically.

#### [NEW] `data/local/InboxEntity.kt` & Database Schema
*   A newly designed database table explicitly for Uncategorized discoveries: `InboxEntity(id, processName, windowTitle, lastSeenTimestamp)`.

#### [NEW] `domain/discovery/DiscoveryEngine.kt`
*   Listens to the pipeline. When the `DictionaryEngine` fails to match an OCR URL or window title (yielding `Category.UNCATEGORIZED`), the `DiscoveryEngine` intercepts the payload.
*   It passes the payload through a dynamic list of injected `DiscoveryStrategy` implementations (e.g., `KeywordMatcherStrategy`, `LocalLLMStrategy`).
*   It subsequently saves the item to the `InboxEntity` table, attaching the *DiscoveryStrategy's suggested category* to be easily approved or modified by the user later via the `DictionaryHubScreen`.

---

### Component 3: Behavioral Pattern Identification
We shift from merely accumulating timer seconds to actively looking for patterns indicative of procrastination.

#### [NEW] `domain/pipeline/BehaviorAnalysisEngine.kt`
*   Operates synchronously on the pipeline's output. Built utilizing a Sliding Window Coroutine model.
*   **Pattern Triggers**:
    *   *Mindless Scrolling*: Detects continuous `Category.DISTRACTING` context + Active Mouse scrolling but zero distinct `AppSwitch` clicks over N minutes.
    *   *Rapid Hopping*: Detects >5 subsequent `AppSwitch` or `BrowserOCRContext` URLs flipping within 30 seconds.
*   Emits a higher-level `BehaviorPattern` class.

---

### Component 4: Choice Architecture (Re-routing Interventions)
Redesigning the `InterventionManager` to present alternatives gracefully.

#### [MODIFY] `InterventionManager.kt`
*   Instead of triggering an Opacity Enforcer on timer boundaries, it triggers when the `BehaviorAnalysisEngine` emits a negative pattern.
*   Utilizes a generic `ReRoutingStrategy` interface. An implementation like `AmbientToastSuggestionStrategy` gently slides a notification asking the user to pivot back to a pre-defined active goal, offering true *Choice Architecture*.

## Open Questions

> [!CAUTION]
> **Performance Profile of OCR** 
> Since the `BrowserAnalyserEngine` takes intermittent screenshots, running OCR logic sequentially inside the core `AppSwitch` logic can be heavy. Are you comfortable with `BrowserAnalyserEngine` operating entirely asynchronously on `Dispatchers.Default`, emitting `BrowserOCRContext` updates potentially a few seconds *after* the initial `AppSwitch` event is logged?

> [!WARNING]
> **LLM Integrations**
> You mentioned using an LLM to generate suggestions. Are we planning to rely on local ML inference engines inside KMP (via JNI/expect-actual bindings), or will this require a secure payload hand-off to an external API (like OpenAI) which would slightly compromise the "100% local processing" privacy goal?

## User Review Required
Please review this updated plan prioritizing ultimate flexibility, OCR ingestion, and the gentle Ambient Re-Routing philosophical approach. Let me know if you would like to proceed with writing the extensive documentation suite next.

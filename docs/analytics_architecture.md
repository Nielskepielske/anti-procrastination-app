# Analytics & Processing Architecture

To handle high-frequency sensor data (e.g., every 5 seconds) without crashing the app or freezing the UI, data is processed across three distinct pipelines, utilizing a Plug-and-Play Workflow driven by Koin Multibindings.

## The Three Processing Pillars

### Pillar 1: Stream Processing (Live Anomalies)
**Where:** `EventPipeline` & `StreamProcessingEngine`  
**Purpose:** Real-time pattern detection.  
**Mechanism:** As raw events stream in, the engine holds a lightweight memory buffer (a `SlidingWindowAnalyzer`). Analyzers evaluate this live window to detect patterns instantaneously. For example, a `TabHoppingAnalyzer` looks at the last few `TitleChange` events. If distinct URLs are repeatedly detected in a short span, it immediately emits a `StreamInsight` to trigger an intervention *before* the data hits the database.

### Pillar 2: Batch Processing (Database Compaction)
**Where:** `CompactionEngine` & Background Schedulers (`AndroidCompactionScheduler` / `DesktopCompactionScheduler`)  
**Purpose:** Prevents database bloat and storage exhaustion.  
**Mechanism:** A background worker wakes up periodically (e.g., hourly). It fetches old `SensorEventEntity` rows, routes them to `SensorCompactionStrategy` implementations to mathematically summarize them, saves them to aggregated tables (e.g., `sensor_events_hourly`), and deletes the raw data. This allows the app to store years of analytics without running out of device storage.

### Pillar 3: Presentation Processing (UI Formatting)
**Where:** `FlexibleAnalyticsViewModel` & `DashboardDataStrategy`  
**Purpose:** Transforms stored DB records into visual charts on-demand.  
**Mechanism:** When a user requests a chart (e.g., "Switch Frequency for the Last 24 Hours"), the ViewModel delegates to a specific strategy. The strategy asks the `TimeSeriesResampler` to slice the time window into perfectly even buckets. It then passes each bucket to a specific reducer (e.g., `SwitchCountReducer`) to calculate the exact float value for that point on the chart.

**Synchronized Fetching:** To ensure that multiple series in a `CombinedChartBlock` align perfectly, the ViewModel calculates a single, consistent `(startTime, endTime)` window for the entire refresh cycle. This window is passed to all strategies, ensuring that their internal `TimeSeriesResampler` logic generates the exact same number of buckets on the same grid alignment, preventing "drifting" or "offset" lines.

---

## Data Flow & Wrappers

To maintain Clean Architecture, the DB schema never leaks into the UI layer, and the context of time is strictly preserved.

1. **Intelligent Routing:** When the UI asks the `SensorEventRepository` for a timeframe, the repository looks at the duration. If duration $\le$ 24 hours, it queries the raw table. If > 24 hours, it queries the aggregated hourly table.
2. **`OptimizedDataResult`:** The repository returns this wrapper containing either `Raw` or `Aggregated` data to the UI layer.
3. **Time Context (`Timestamped<T>`):** Inside the result, the data is wrapped in a `Timestamped` container. This allows the UI mappers to know exactly when an event happened without needing to know anything about the underlying `SensorEventEntity` database schema.
4. **Recursive State Management:** The `FlexibleAnalyticsViewModel` manages a tree of `DashboardBlock` objects. `CombinedChartBlock` acts as a composite, allowing the UI to treat a group of sensors as a single entity for layout while maintaining individual configuration (strategy, color) for each child.

---

## The Plug-and-Play Workflow

The architecture utilizes Koin Multibindings (`getAll()`) to eliminate central bottlenecks. Adding a new feature requires zero changes to core files.

**Example: Adding a new "Scroll Tracker"**
1. **The Data**: Define `ScrollMetrics : SensorPayload` and `ScrollAggregated : AggregatedPayload`.
2. **The Sensor**: Implement `ScrollTracker : BehaviorSensor`. Koin injects it into `SensorManager`.
3. **The Compaction**: Implement `ScrollCompactionStrategy`. Koin injects it into `CompactionEngine`.
4. **The UI Math**: Implement `ScrollReducer : WindowedReducer`.
5. **The UI Presenter**: Implement `ScrollChartStrategy : DashboardDataStrategy`. Koin injects it into `FlexibleAnalyticsViewModel`.

Because the Compose UI uses a generic ChartData registry, the new scroll chart will automatically render on the dashboard with full TimeRange controls.

# Creating New Dashboard Data Strategies

The Flexible Analytics Dashboard is built using a dynamic, interface-driven architecture. This means you can add completely new visualizations and metrics to the dashboard without writing a single line of UI code.

The system relies on two main components:
1. **Windowed Reducer**: Aggregates raw, high-frequency telemetry events (like 1-second mouse polls) into meaningful data points inside a time bucket (e.g., 30 minutes).
2. **Dashboard Data Strategy**: Provides the UI with the name, the required chart type, compatible sensors, and the logic to fetch and format the reduced data into `ChartData`.

## Step 1: Create a Reducer
A reducer takes a list of `SensorPayload` events that occurred in a specific time bucket and reduces them to a single `Float` value.

Create a class extending `WindowedReducer` in `domain/pipeline/resampling/`:

```kotlin
class MyCustomReducer : WindowedReducer<SensorPayload> {
    override fun reduce(bucketTimestamp: Long, items: List<SensorPayload>): Float {
        // Filter for the payloads you care about
        val specificEvents = items.filterIsInstance<SensorPayload.MyCustomMetrics>()
        
        // Return your aggregated Float value
        return specificEvents.sumOf { it.someValue }.toFloat()
    }
}
```

## Step 2: Create a Strategy
A strategy tells the dashboard *what* your chart is called and *how* to construct it.

Create a class extending `DashboardDataStrategy` in `ui/analytics/strategy/`:

```kotlin
class MyCustomStrategy(
    private val sensorEventDao: SensorEventDao,
    private val reducer: WindowedReducer<SensorPayload>
) : DashboardDataStrategy {

    override val dataTypeId: String = "my_custom_metric"
    override val displayName: String = "My Custom Metric"
    
    // Choose your chart type (e.g., ChartData.Line::class, ChartData.Bar::class)
    override val chartType: KClass<out ChartData> = ChartData.Line::class
    
    // Set to null to allow all sensors, or specify a SensorType (e.g., setOf(SensorType.MY_SENSOR.name))
    override val compatibleEventTypes: Set<String>? = null 

    override suspend fun generateChartData(startTime: Long, endTime: Long, sensorId: String?): ChartData? {
        // 1. Fetch raw events
        val events = sensorEventDao.getEventsBetween(startTime, endTime, sensorId)
        
        // 2. Determine bucket size
        val bucketSize = if ((endTime - startTime) <= 3_600_000L) 60_000L else 3_600_000L

        // 3. Resample into buckets
        val buckets = TimeSeriesResampler.bucketData(
            data = events,
            startTime = startTime,
            endTime = endTime,
            bucketSizeMillis = bucketSize,
            timestampSelector = { it.timestamp }
        )

        val sortedEntries = buckets.entries.sortedBy { it.key }

        // 4. Reduce each bucket using your Reducer
        val points = sortedEntries.map { (time, items) ->
            reducer.reduce(time, items = items.map { it.payload })
        }

        // 5. Construct and return your ChartData object
        val dataset = ChartData.Line.LineDataset("My Metric", points, Color.Blue)
        return ChartData.Line(
            lines = listOf(dataset),
            maxPoint = points.maxOrNull() ?: 0f,
            xCategories = sortedEntries.map { it.key },
            valueSuffix = " units" // Displayed on the Y-Axis
        )
    }
}
```

## Step 3: Register in Koin
Finally, register your Reducer and Strategy in `di/CoreModule.kt`. 

**Critically, you must `named()` your strategy and cast it to `DashboardDataStrategy`** so that the Flexible Analytics Dashboard automatically discovers it:

```kotlin
// In coreModule
factory { MyCustomReducer() }

single<DashboardDataStrategy>(org.koin.core.qualifier.named("myCustomMetric")) { 
    MyCustomStrategy(sensorEventDao = get(), reducer = get<MyCustomReducer>()) 
}
```

That's it! When you launch the app, your new strategy will automatically appear in the "Add Block" dropdown menu in the Flexible Analytics Dashboard. You can even combine it with other strategies that share the same `chartType`!

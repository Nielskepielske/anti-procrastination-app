# Adding a New Sensor

This guide outlines how to create and integrate a new tracking sensor into the procrastination detection engine. The architecture relies on Dependency Injection (Koin) to automatically discover and start registered sensors.

## 1. Define the Sensor ID
Open `domain/sensor/SensorType.kt` and add your new sensor to the enum.
```kotlin
enum class SensorType {
    WINDOW_TRACKER,
    BROWSER_ANALYSER_SENSOR,
    YOUR_NEW_SENSOR // <-- Add here
}
```

## 2. Implement the Sensor Logic
For most continuous tracking scenarios, you should extend `BasePollingSensor` instead of implementing `BehaviorSensor` directly. This gives you automatic background coroutine management and error handling.

Create a new file for your sensor:
```kotlin
import com.example.procrastination_detection.domain.sensor.BasePollingSensor
import com.example.procrastination_detection.domain.sensor.SensorType
import kotlinx.coroutines.CoroutineScope

class YourNewSensor(
    scope: CoroutineScope
) : BasePollingSensor(scope, intervalMs = 1000L) { // Polls every 1 second

    override val type: SensorType = SensorType.YOUR_NEW_SENSOR

    override suspend fun onStart() {
        // Optional: setup resources when tracking begins
    }

    override suspend fun pollData() {
        // Implement your data gathering logic here.
        // E.g., read system metrics, check mouse movement, etc.
        // If you need to emit data, use your injected EventPipeline:
        // eventPipeline.emitRawEvent(SensorPayload.MouseMetrics(...))
    }

    override fun onStop() {
        // Optional: clean up resources when tracking stops
    }
}
```

## 3. Register the Sensor in Koin
Open your platform-specific module (e.g., `main.kt` for Linux) and register your sensor into the Dependency Injection graph.

```kotlin
val linuxModule = module {
    // ...
    single<BehaviorSensor> {
        YourNewSensor(scope = get())
    }
}
```
**That's it!** The `SensorManager` automatically uses `getAll<BehaviorSensor>()`, so your new sensor will be picked up, added to the UI lists, and automatically managed by the application lifecycle.

## Custom Non-Polling Sensors
If your sensor is entirely event-driven (e.g. relying on OS webhooks or global listeners) and does not need a polling loop, you can bypass `BasePollingSensor` and implement the `BehaviorSensor` interface directly:

```kotlin
class WebhookSensor : BehaviorSensor {
    override val type: SensorType = SensorType.WEBHOOK_SENSOR

    override fun start() {
        // Register OS listener
    }

    override fun stop() {
        // Unregister OS listener
    }
}
```

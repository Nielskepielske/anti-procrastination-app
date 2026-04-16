package com.example.procrastination_detection
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.procrastination_detection.data.local.AppDatabase
import com.example.procrastination_detection.data.local.buildRoomDatabaseForLinux
import com.example.procrastination_detection.database.getDatabaseBuilder
import com.example.procrastination_detection.database.getRoomDatabase
import com.example.procrastination_detection.di.initKoin
import com.example.procrastination_detection.domain.intervention.InterventionStrategy
import com.example.procrastination_detection.domain.pipeline.EventPipeline
import com.example.procrastination_detection.domain.pipeline.FocusTimerEngine
import com.example.procrastination_detection.domain.sensor.BehaviorSensor
import com.example.procrastination_detection.domain.sensor.SensorManager
import com.example.procrastination_detection.engine.FocusEnforcerEngine
import com.example.procrastination_detection.factories.WindowStyleManagerFactory
import com.example.procrastination_detection.intervention.LinuxNotificationStrategy
import com.example.procrastination_detection.repositories.LocalAppRepository
import com.example.procrastination_detection.sensor.LinuxWindowTracker
import com.mmk.kmpnotifier.extensions.composeDesktopResourcesPath
import com.mmk.kmpnotifier.notification.NotificationImage
import com.mmk.kmpnotifier.notification.Notifier
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import org.koin.dsl.module
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

fun main() = application {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    // Define Linux-specific dependencies
    val linuxModule = module {
        // Register sensors to the toolbox
        single<BehaviorSensor> {
            LinuxWindowTracker(eventPipeline = get(), scope = applicationScope)
        }
        // Give the whole toolbox to the Manager
        single{
            SensorManager(availableSensors = getAll<BehaviorSensor>())
        }
        // Build the Room database specifically for Linux desktop
        single<AppDatabase> {
            buildRoomDatabaseForLinux()
        }

        // Provide the scope for the TimerEngine
        single<CoroutineScope> { applicationScope }

        // Add Linux NotificationStrategy to the toolbox
        single<InterventionStrategy> { LinuxNotificationStrategy() }
    }

    // Start the core brain
    initKoin(linuxModule)

    val koin = GlobalContext.get()

    val eventPipeline = koin.get<EventPipeline>()
    val dictionaryEngine = koin.get<com.example.procrastination_detection.domain.dictionary.DictionaryEngine>()
    val ruleRepository = koin.get<com.example.procrastination_detection.domain.repository.IRuleRepository>()

    // Load initial rules into the engine
    applicationScope.launch {
        val initialRules = ruleRepository.getRulesForEngine()
        dictionaryEngine.updateRules(initialRules)
    }

    eventPipeline.start(applicationScope)

    val sensorManager = koin.get<SensorManager>()
    sensorManager.startAllActiveSensors()

    val timerEngine = koin.get<FocusTimerEngine>()
    timerEngine.startListening()

    val analyticsTimerEngine = koin.get<com.example.procrastination_detection.domain.pipeline.AnalyticsTimerEngine>()
    analyticsTimerEngine.startListening()

    val discoveryEngine = koin.get<com.example.procrastination_detection.domain.discovery.DiscoveryEngine>()
    discoveryEngine.startListening()

    Window(onCloseRequest = ::exitApplication) {
        App()
    }
    // 1. Build the DB using the JVM-specific file path
//    val dbBuilder = getDatabaseBuilder()
//    val database = getRoomDatabase(dbBuilder)
//
//    val appContainer = AppContainer(
//        processDao = database.processDao(),
//        sessionDao = database.sessionDao(),
//        ruleDao = database.ruleDao(),
//        categoryDao = database.categoryDao()
//    )
//
//    // Notification manager
//    val iconUrl = Thread.currentThread().contextClassLoader.getResource("ic_notification.png")
//    val iconPath = iconUrl?.let { File(it.toURI()).absolutePath } ?: ""
//
//    // 2. Initialize the Notifier
//    NotifierManager.initialize(
//        NotificationPlatformConfiguration.Desktop(
//            notificationIconPath = iconPath
//        )
//    )
//
//    // Focus enforcer
//    val focusEngine = remember { appContainer.focusEnforcerEngine }
//    val enforcedApps by focusEngine.enforcedApps.collectAsState()
//    val resetTrigger by focusEngine.resetAllTrigger.collectAsState()
//
//    // 3. Pass it to the shared App Composable
//    Window(
//        onCloseRequest = ::exitApplication,
//        title = "Procrastination Detector"
//    ) {
//
//        val styleManager = remember { WindowStyleManagerFactory.create(window) }
//        val currentlyModifiedApps = remember { mutableSetOf<String>() }
//
//        LaunchedEffect(Unit) {
//                styleManager.setWindowOpacity(0.8f, "kitty")
//        }
//
//        LaunchedEffect(enforcedApps) {
//            enforcedApps.forEach { (appTitle, level) ->
//                val dynamicOpacity = kotlin.math.max(0.1f, 1.0f - (level * 0.1f))
//                styleManager.setWindowOpacity(dynamicOpacity, appTitle)
//                currentlyModifiedApps.add(appTitle)
//            }
//
//            // Restore apps that are no longer enforced
//            val removedApps = currentlyModifiedApps.filter { !enforcedApps.containsKey(it) }
//            removedApps.forEach {
//                styleManager.setWindowOpacity(1.0f, it)
//                currentlyModifiedApps.remove(it)
//            }
//        }
//
//        LaunchedEffect(resetTrigger) {
//            if (resetTrigger > 0) {
//                currentlyModifiedApps.forEach {
//                    styleManager.setWindowOpacity(1.0f, it)
//                }
//                currentlyModifiedApps.clear()
//            }
//        }
//        App(appContainer = appContainer)
//    }




    // We use this to make the screen visible to our hyprland
//    if(true){
//        Window(
//            onCloseRequest = { /* Do nothing, they can't close it easily! */ },
//            title = "AggressiveOverlay", // Crucial for Hyprland rules!
//            undecorated = true,
//            transparent = true,
//            alwaysOnTop = true,
//            state = rememberWindowState()
//        ) {
//            // The UI they are forced to look at
//            val styleManager = remember { WindowStyleManagerFactory.create(window) }
//
////             When your logic dictates a change:
//            LaunchedEffect(Unit) {
////                val newOpacity = 0.5f + (overlayState.aggressionLevel * 0.1f)
////                println("Setting opacity to $newOpacity")
////                styleManager.setWindowOpacity(newOpacity)
//                styleManager.setWindowOpacity(0.5f) // Serves mostly to induce the hyprctl rules in the beginning for the culling
//            }
//
//            //val dynamicOpacity = min((overlayState.aggressionLevel * 0.1f), 1.0f)
//
//            val animatedOpacity by animateFloatAsState(targetValue = 0.5f, animationSpec = tween(durationMillis = 500))
//
//            Box(modifier = Modifier
//                .fillMaxSize()
//                .graphicsLayer { alpha = animatedOpacity }
//                .background(Color.White)
//                .padding(40.dp)
//            ) {
////                Text("STOP PROCRASTINATING. Level: ${overlayState.aggressionLevel}")
////                Button(onClick = { focusEngine.onOverlayDismissed() }) {
////                    Text("I'll go back to work (Dismiss)")
////                }
//
//            }
//        }
//    }

}
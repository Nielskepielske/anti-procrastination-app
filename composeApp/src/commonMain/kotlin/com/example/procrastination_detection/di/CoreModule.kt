package com.example.procrastination_detection.di

import com.example.procrastination_detection.domain.dictionary.DictionaryEngine
import com.example.procrastination_detection.domain.discovery.DiscoveryEngine
import com.example.procrastination_detection.domain.discovery.DiscoveryStrategy
import com.example.procrastination_detection.domain.discovery.KeywordMatcherStrategy
import com.example.procrastination_detection.domain.event.SensorPayload
import com.example.procrastination_detection.domain.intervention.InterventionManager
import com.example.procrastination_detection.domain.intervention.InterventionStrategy
import com.example.procrastination_detection.domain.pipeline.EventPipeline
import com.example.procrastination_detection.domain.pipeline.FocusTimerEngine
import com.example.procrastination_detection.domain.pipeline.compaction.CompactionEngine
import com.example.procrastination_detection.domain.pipeline.compaction.strategies.AppSwitchCompactionStrategy
import com.example.procrastination_detection.domain.pipeline.compaction.strategies.MouseCompactionStrategy
import com.example.procrastination_detection.domain.pipeline.resampling.IntensityCountReducer
import com.example.procrastination_detection.domain.pipeline.resampling.SwitchCountReducer
import com.example.procrastination_detection.domain.pipeline.resampling.WindowedReducer
import com.example.procrastination_detection.domain.pipeline.stream.SlidingWindowAnalyzer
import com.example.procrastination_detection.domain.pipeline.stream.StreamProcessingEngine
import com.example.procrastination_detection.domain.pipeline.stream.TabHoppingAnalyzer
import com.example.procrastination_detection.domain.repository.IRuleRepository
import com.example.procrastination_detection.domain.repository.RuleRepository
import com.example.procrastination_detection.domain.repository.SensorEventRepository
import com.example.procrastination_detection.domain.repository.SensorEventRepositoryImpl
import com.example.procrastination_detection.domain.trigger.ActionTrigger
import com.example.procrastination_detection.domain.trigger.BrowserAnalysisTrigger
import com.example.procrastination_detection.domain.trigger.TriggerManager
import com.example.procrastination_detection.engine.BrowserAnalyserEngine
import com.example.procrastination_detection.helpers.LocalUrlExtractor
import com.example.procrastination_detection.ui.analytics.AnalyticsViewModel
import com.example.procrastination_detection.ui.analytics.FlexibleAnalyticsViewModel
import com.example.procrastination_detection.ui.analytics.strategy.DashboardDataStrategy
import com.example.procrastination_detection.ui.analytics.strategy.SwitchFrequencyStrategy
import com.example.procrastination_detection.ui.dashboard.DashboardViewModel
import com.example.procrastination_detection.ui.dictionary.DictionaryViewModel
import com.example.procrastination_detection.ui.profile.ProfileViewModel
import com.example.procrastination_detection.domain.sensor.TelemetryManager
import com.example.procrastination_detection.domain.session.SessionManager
import com.example.procrastination_detection.domain.pipeline.CsvExportEngine
import com.example.procrastination_detection.domain.pipeline.resampling.MouseDistanceReducer
import com.example.procrastination_detection.domain.pipeline.resampling.MouseIdleReducer
import com.example.procrastination_detection.domain.pipeline.resampling.SystemInterventionReducer
import com.example.procrastination_detection.ui.analytics.strategy.SystemInterventionStrategy
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val coreModule = module {
    // 1. The Dictionary Engine (Starts empty, rules load later)
    single { DictionaryEngine(emptyList()) }

    // 2. The Repository (Koin will automatically find the AppDatabase via get())
    single<SensorEventRepository> {
        SensorEventRepositoryImpl(database = get(), sessionManagerProvider = { get() }, csvReader = get())
    }
    // We pass TriggerManager to the RuleRepository to hydrate retrieved ActionTriggers
    single<IRuleRepository> { RuleRepository(database = get(), triggerManager = get()) }

    // 3. The Central Pipeline (Automatically injects the dictionary and repositories
    single {
        EventPipeline(
            dictionaryEngine = get(),
            repository = get(),
            streamEngine = get()
        )
    }

    single {
        InterventionManager(
            availableStrategies = getAll<InterventionStrategy>(),
            pipeline = get()
        )
    }

    // We inject the coroutinescope from the outside
    single {
        FocusTimerEngine(
            pipeline = get(),
            interventionManager = get(),
            sensorManager = get(),
            scope = get(),
            focusProfileRepository = get()
        )
    }

    single { get<com.example.procrastination_detection.data.local.AppDatabase>().focusProfileDao() }
    single { get<com.example.procrastination_detection.data.local.AppDatabase>().sensorEventDao() }
    single { get<com.example.procrastination_detection.data.local.AppDatabase>().compactionDao() }
    single { com.example.procrastination_detection.data.local.ActiveProfileStore() }
    single {
        com.example.procrastination_detection.domain.repository.FocusProfileRepository(
            focusProfileDao = get(),
            activeProfileStore = get(),
            sensorManager = get(),
            scope = get()
        )
    }

    single { get<com.example.procrastination_detection.data.local.AppDatabase>().sessionDao() }

    single {
        CsvExportEngine(
            sensorEventDao = get(),
            sessionDao = get(),
            fileExporter = get(),
            scope = get()
        )
    }

    single {
        SessionManager(
            sessionDao = get(),
            sensorManager = get(),
            focusProfileRepository = get(),
            csvExportEngine = get(),
            scope = get()
        )
    }

    single {
        TelemetryManager(
            pipeline = get(),
            sensorManager = get(),
            scope = get()
        )
    }

    single { get<com.example.procrastination_detection.data.local.AppDatabase>().appUsageDao() }

    single {
        com.example.procrastination_detection.domain.pipeline.AnalyticsTimerEngine(
            pipeline = get(),
            appUsageDao = get(),
            sensorManager = get(),
            scope = get()
        )
    }

    single { get<com.example.procrastination_detection.data.local.AppDatabase>().inboxDao() }

    // Discovery strategies — add new ones here and they're auto-included in DiscoveryEngine
    single<DiscoveryStrategy> { KeywordMatcherStrategy() }

    single {
        DiscoveryEngine(
            pipeline = get(),
            inboxDao = get(),
            strategies = getAll<DiscoveryStrategy>(),
            scope = get()
        )
    }

    // --- Dynamic Triggers ---
    single { LocalUrlExtractor() }
    
    // Engine instantiated internally so we can pass it to Triggers
    single { BrowserAnalyserEngine(urlExtractor = get(), pipeline = get(), scope = get()) }

    // Register active Triggers
    single<ActionTrigger> { BrowserAnalysisTrigger(browserAnalyserEngine = get()) }
    
    // Provides list of all ActionTriggers loaded into Koin
    single { TriggerManager(availableTriggers = getAll<ActionTrigger>()) }

    // Viewmodels
    viewModel {
        DashboardViewModel(
            pipeline = get(),
            sessionManager = get(),
            sensorManager = get(),
            browserAnalyserEngine = get(),
            focusProfileRepository = get(),
            telemetryManager = get()
        )
    }
    viewModel { DictionaryViewModel(dictionaryEngine = get(), ruleRepository = get(), inboxDao = get(), triggerManager = get()) }
    viewModel { ProfileViewModel(sensorManager = get(), interventionManager = get(), focusProfileRepository = get()) }
    viewModel { AnalyticsViewModel(appUsageDao = get(), sensorEventDao = get(), dictionaryEngine = get()) }
    single { com.example.procrastination_detection.data.local.AnalyticsConfigStore() }
    viewModel { 
        FlexibleAnalyticsViewModel(
            strategies = getAll<DashboardDataStrategy>().toSet(), 
            sensorManager = get(), 
            configStore = get(),
            sessionDao = get(),
            sessionDownloader = get(),
            sensorEventRepository = get()
        ) 
    }


    // Compaction
    // Here all strategies
    factory<MouseCompactionStrategy> { MouseCompactionStrategy() }
    factory<AppSwitchCompactionStrategy> { AppSwitchCompactionStrategy() }

    // Build the engine
    single {
        val strategies = setOf(
            get<MouseCompactionStrategy>(),
            get<AppSwitchCompactionStrategy>()
        )
        CompactionEngine(strategies)
    }

    factory { SwitchCountReducer() }
    factory { com.example.procrastination_detection.domain.pipeline.resampling.AggressionHeatReducer() }
    factory { IntensityCountReducer() }
    factory { com.example.procrastination_detection.domain.pipeline.resampling.MouseDistanceReducer() }
    factory { com.example.procrastination_detection.domain.pipeline.resampling.MouseIdleReducer() }

    single<DashboardDataStrategy>(org.koin.core.qualifier.named("switchFreq")) { SwitchFrequencyStrategy(repository = get(), reducer = get()) }
    single<DashboardDataStrategy>(org.koin.core.qualifier.named("aggressionHeat")) { com.example.procrastination_detection.ui.analytics.strategy.AggressionHeatStrategy(repository = get(), reducer = get<com.example.procrastination_detection.domain.pipeline.resampling.AggressionHeatReducer>()) }
    single<DashboardDataStrategy>(org.koin.core.qualifier.named("topElements")) { com.example.procrastination_detection.ui.analytics.strategy.TopElementsStrategy(appUsageDao = get()) }
    single<DashboardDataStrategy>(org.koin.core.qualifier.named("intensity")) { com.example.procrastination_detection.ui.analytics.strategy.IntensityStrategy(repository = get(), reducer = get<IntensityCountReducer>()) }
    single<DashboardDataStrategy>(org.koin.core.qualifier.named("mouseDist")) { com.example.procrastination_detection.ui.analytics.strategy.MouseDistanceStrategy(repository = get(), reducer = get<com.example.procrastination_detection.domain.pipeline.resampling.MouseDistanceReducer>()) }
    single<DashboardDataStrategy>(org.koin.core.qualifier.named("mouseIdle")) { com.example.procrastination_detection.ui.analytics.strategy.MouseIdleStrategy(repository = get(), reducer = get<com.example.procrastination_detection.domain.pipeline.resampling.MouseIdleReducer>()) }
    
    // Intervention counting Reducer & Strategy
    factory { SystemInterventionReducer() }
    single<DashboardDataStrategy>(org.koin.core.qualifier.named("interventionCount")) { SystemInterventionStrategy(repository = get(), reducer = get()) }

    // Streaming
    single<SlidingWindowAnalyzer<out SensorPayload>> { TabHoppingAnalyzer() }
    single {
        StreamProcessingEngine(analyzers = getAll<SlidingWindowAnalyzer<out SensorPayload>>().toSet())
    }


}
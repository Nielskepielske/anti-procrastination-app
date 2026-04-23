package com.example.procrastination_detection.di

import com.example.procrastination_detection.domain.dictionary.DictionaryEngine
import com.example.procrastination_detection.domain.discovery.DiscoveryEngine
import com.example.procrastination_detection.domain.discovery.DiscoveryStrategy
import com.example.procrastination_detection.domain.discovery.KeywordMatcherStrategy
import com.example.procrastination_detection.domain.intervention.InterventionManager
import com.example.procrastination_detection.domain.intervention.InterventionStrategy
import com.example.procrastination_detection.domain.pipeline.EventPipeline
import com.example.procrastination_detection.domain.pipeline.FocusTimerEngine
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
import com.example.procrastination_detection.ui.dashboard.DashboardViewModel
import com.example.procrastination_detection.ui.dictionary.DictionaryViewModel
import com.example.procrastination_detection.ui.profile.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val coreModule = module {
    // 1. The Dictionary Engine (Starts empty, rules load later)
    single { DictionaryEngine(emptyList()) }

    // 2. The Repository (Koin will automatically find the AppDatabase via get())
    single<SensorEventRepository> {
        SensorEventRepositoryImpl(database = get())
    }
    // We pass TriggerManager to the RuleRepository to hydrate retrieved ActionTriggers
    single<IRuleRepository> { RuleRepository(database = get(), triggerManager = get()) }

    // 3. The Central Pipeline (Automatically injects the dictionary and repositories
    single {
        EventPipeline(
            dictionaryEngine = get(),
            repository = get()
        )
    }

    single {
        InterventionManager(availableStrategies = getAll<InterventionStrategy>())
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
    single { com.example.procrastination_detection.data.local.ActiveProfileStore() }
    single {
        com.example.procrastination_detection.domain.repository.FocusProfileRepository(
            focusProfileDao = get(),
            activeProfileStore = get(),
            sensorManager = get()
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
    viewModel { DashboardViewModel(pipeline = get(), sensorManager = get()) }
    viewModel { DictionaryViewModel(dictionaryEngine = get(), ruleRepository = get(), inboxDao = get(), triggerManager = get()) }
    viewModel { ProfileViewModel(sensorManager = get(), interventionManager = get(), focusProfileRepository = get()) }
    viewModel { AnalyticsViewModel(appUsageDao = get(), sensorEventDao = get(), dictionaryEngine = get()) }
}
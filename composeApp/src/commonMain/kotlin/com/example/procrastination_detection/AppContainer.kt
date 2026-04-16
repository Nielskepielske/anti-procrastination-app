package com.example.procrastination_detection

import com.example.procrastination_detection.database.CategoryDao
import com.example.procrastination_detection.database.ProcessDao
import com.example.procrastination_detection.database.RuleDao
import com.example.procrastination_detection.database.SessionDao
import com.example.procrastination_detection.engine.BrowserAnalyserEngine
import com.example.procrastination_detection.engine.FocusEnforcerEngine
import com.example.procrastination_detection.engine.TrackingEngine
import com.example.procrastination_detection.helpers.BrowserAnalyserConfig
import com.example.procrastination_detection.helpers.LocalUrlExtractor
import com.example.procrastination_detection.interfaces.ConfigRepository
import com.example.procrastination_detection.interfaces.ProcessRepository
import com.example.procrastination_detection.interfaces.SessionRepository
import com.example.procrastination_detection.repositories.local.LocalConfigRepository
import com.example.procrastination_detection.repositories.local.LocalProcessRepository
import com.example.procrastination_detection.repositories.local.LocalSessionRepository

class AppContainer(
    private val processDao: ProcessDao,
    private val ruleDao: RuleDao,
    private val sessionDao: SessionDao,
    private val categoryDao: CategoryDao
) {
    // val processRepository : ProcessRepository by lazy {
    //     LocalProcessRepository(processDao)
    // }
    // val configRepository : ConfigRepository by lazy {
    //     LocalConfigRepository(processDao, ruleDao)
    // }
    // val sessionRepository : SessionRepository by lazy {
    //     LocalSessionRepository(sessionDao, processDao, ruleDao)
    // }



    // val focusEnforcerEngine: FocusEnforcerEngine by lazy {
    //     FocusEnforcerEngine()
    // }
    // val browserAnalyserEngine: BrowserAnalyserEngine by lazy {
    //     // BrowserAnalyserEngine(urlExtractor = LocalUrlExtractor(), pipline = get())
    // }
    // val trackingEngine: TrackingEngine by lazy {
    //     TrackingEngine(sessionRepository, focusEnforcerEngine, browserAnalyserEngine)
    // }


}
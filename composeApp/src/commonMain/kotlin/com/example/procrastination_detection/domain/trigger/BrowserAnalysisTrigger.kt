package com.example.procrastination_detection.domain.trigger

import com.example.procrastination_detection.domain.dictionary.DictionaryEngine
import com.example.procrastination_detection.engine.BrowserAnalyserEngine

class BrowserAnalysisTrigger(val browserAnalyserEngine: BrowserAnalyserEngine) : IAmbiguousActionTrigger() {
    override val id: String = "browser"

    override fun start() {
        browserAnalyserEngine.start()
    }

    override fun stop() {
        browserAnalyserEngine.stop()
    }
}
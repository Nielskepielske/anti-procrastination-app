package com.example.procrastination_detection.domain.sensor

interface BehaviorSensor {
    val id: String
    fun start()
    fun stop()
}
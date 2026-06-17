package com.example.procrastination_detection.domain.sensor

interface BehaviorSensor {
    val type: SensorType
    fun start()
    fun stop()
}
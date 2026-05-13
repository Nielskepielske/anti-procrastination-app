package com.example.procrastination_detection.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module

/**
 * Called from the platform-specific entry point (e.g., Linux main function).
 * @param platformModule The module containing OS-specific implementations (like the Room DB builder).
 */
fun initKoin(platformModule: Module) {
    startKoin {
        // Load the shared components
        modules(coreModule)

        // Load the platform-specific components (Database, Linux Sensors, etc.)
        modules(platformModule)
    }
}
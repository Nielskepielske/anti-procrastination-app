package com.example.procrastination_detection

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
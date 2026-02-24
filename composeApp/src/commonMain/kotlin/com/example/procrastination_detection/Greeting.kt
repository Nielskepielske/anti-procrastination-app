package com.example.procrastination_detection

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello Niels, ${platform.name}!"
    }
}
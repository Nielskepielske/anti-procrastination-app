package com.example.procrastination_detection.domain.model

enum class Category(val label : String, val description: String) {
    PRODUCTIVE("Productive", "For applications that are definetely productive"),
    DISTRACTING("Distracting", "For distracting applications"),
    NEUTRAL("Neutral", "For applications that could contains child processes (e.g. Browsers)"),
    AMBIGUOUS("Ambiguous", "When the content of the applicication is variable and needs further evaluation"),
    UNCATEGORIZED("Uncategorized", "When there was no categorize asigned to the application yet")
}
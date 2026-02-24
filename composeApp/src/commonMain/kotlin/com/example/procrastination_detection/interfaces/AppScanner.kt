package com.example.procrastination_detection.interfaces

import com.example.procrastination_detection.models.ProcessInfo

interface AppScanner {
  fun getRunningAppNames(): List<ProcessInfo>
}

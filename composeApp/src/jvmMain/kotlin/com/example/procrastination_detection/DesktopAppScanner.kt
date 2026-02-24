package com.example.procrastination_detection

import com.example.procrastination_detection.interfaces.AppScanner
import com.example.procrastination_detection.models.ProcessInfo
import oshi.SystemInfo

class DesktopAppScanner : AppScanner {
    private val si = SystemInfo()
    private val os = si.operatingSystem

    override fun getRunningAppNames(): List<ProcessInfo> {
        println(os)
        println(os.processes)
        // Get all processes
        return os.processes.map { process ->
            ProcessInfo(
                name = process.name,
                // Logic: If UID is < 1000, it's usually a system service on Linux
                isSystem = process.userID.toInt() < 1000,
                // Check if it's actually consuming significant memory/resources
                isForeground = process.residentSetSize > 0
            )
        }
    }
}
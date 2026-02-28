package com.example.procrastination_detection.helpers

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        TODO("VERSION.SDK_INT < Q")
    }
    return mode == AppOpsManager.MODE_ALLOWED
}
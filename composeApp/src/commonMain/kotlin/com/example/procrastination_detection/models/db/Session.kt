package com.example.procrastination_detection.models.db

import androidx.room.Entity
import androidx.room.PrimaryKey

data class Session(
    val id: String,
    val name: String,

    val rule: Rule,
    val processes: List<MonitoredProcess>
);


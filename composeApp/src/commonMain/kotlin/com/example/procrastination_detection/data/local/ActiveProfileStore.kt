package com.example.procrastination_detection.data.local

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic contract for persisting the active profile ID.
 * A single string key stored in DataStore — intentionally kept separate from Room
 * because it is a singleton preference, not a queryable list row.
 */
expect class ActiveProfileStore() {
    val activeProfileIdFlow: Flow<String?>
    suspend fun setActiveProfileId(id: String)
}

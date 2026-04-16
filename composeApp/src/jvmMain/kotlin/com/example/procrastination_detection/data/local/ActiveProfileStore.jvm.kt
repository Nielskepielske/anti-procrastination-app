package com.example.procrastination_detection.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

actual class ActiveProfileStore {

    private val dataStore: DataStore<Preferences> = createDataStore()

    actual val activeProfileIdFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PROFILE_ID]
    }

    actual suspend fun setActiveProfileId(id: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_PROFILE_ID] = id
        }
    }

    companion object {
        private val KEY_ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")

        private fun createDataStore(): DataStore<Preferences> {
            val userHome = System.getProperty("user.home")
            val appDataDir = File(userHome, ".local/share/ProcrastinationAware")
            if (!appDataDir.exists()) appDataDir.mkdirs()
            
            return PreferenceDataStoreFactory.create(
                produceFile = { File(appDataDir, "active_profile.preferences_pb") }
            )
        }
    }
}

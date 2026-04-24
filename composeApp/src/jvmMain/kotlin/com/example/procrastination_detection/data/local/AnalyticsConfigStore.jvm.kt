package com.example.procrastination_detection.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.procrastination_detection.domain.model.analytics.DashboardBlockConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

actual class AnalyticsConfigStore {
    private val dataStore: DataStore<Preferences> = createDataStore()

    actual val configFlow: Flow<List<DashboardBlockConfig>> = dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_CONFIG] ?: return@map emptyList()
        try {
            Json.decodeFromString<List<DashboardBlockConfig>>(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    actual suspend fun saveConfig(config: List<DashboardBlockConfig>) {
        val jsonStr = Json.encodeToString(config)
        dataStore.edit { prefs ->
            prefs[KEY_CONFIG] = jsonStr
        }
    }

    companion object {
        private val KEY_CONFIG = stringPreferencesKey("analytics_config")

        private fun createDataStore(): DataStore<Preferences> {
            val userHome = System.getProperty("user.home")
            val appDataDir = File(userHome, ".local/share/ProcrastinationAware")
            if (!appDataDir.exists()) appDataDir.mkdirs()
            
            return PreferenceDataStoreFactory.create(
                produceFile = { File(appDataDir, "analytics_config.preferences_pb") }
            )
        }
    }
}

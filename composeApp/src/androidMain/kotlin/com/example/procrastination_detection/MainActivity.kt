package com.example.procrastination_detection

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.procrastination_detection.database.getDatabaseBuilder
import com.example.procrastination_detection.database.getRoomDatabase
import com.example.procrastination_detection.repositories.LocalAppRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Build the Database (applicationContext is perfectly valid here!)
        val dbBuilder = getDatabaseBuilder(applicationContext)
        val database = getRoomDatabase(dbBuilder)

        val appContainer = AppContainer(
            processDao = database.processDao(),
            sessionDao = database.sessionDao(),
            ruleDao = database.ruleDao(),
            categoryDao = database.categoryDao()
        )

        // 3. Pass it to your shared App composable
        setContent {
            App(appContainer = appContainer)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    //App()
}
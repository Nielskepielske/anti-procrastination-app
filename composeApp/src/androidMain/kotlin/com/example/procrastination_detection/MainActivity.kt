package com.example.procrastination_detection

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.procrastination_detection.database.getDatabaseBuilder
import com.example.procrastination_detection.database.getRoomDatabase
import com.example.procrastination_detection.domain.pipeline.compaction.CompactionScheduler
import com.example.procrastination_detection.helpers.AndroidAppBridge
import com.example.procrastination_detection.platform.background.AndroidCompactionScheduler
import com.example.procrastination_detection.repositories.LocalAppRepository
import com.example.procrastination_detection.ui.UsagePermissionScreen
import org.koin.dsl.module

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val androidModule = module { single<CompactionScheduler> { AndroidCompactionScheduler(context = get()) } }

//        AndroidAppBridge.applicationContext = this.applicationContext
//
//        // 1. Build the Database (applicationContext is perfectly valid here!)
//        val dbBuilder = getDatabaseBuilder(applicationContext)
//        val database = getRoomDatabase(dbBuilder)
//
//        val appContainer = AppContainer(
//            processDao = database.processDao(),
//            sessionDao = database.sessionDao(),
//            ruleDao = database.ruleDao(),
//            categoryDao = database.categoryDao()
//        )
//
//        // 3. Pass it to your shared App composable
//        setContent {
//            UsagePermissionScreen {
//                App(appContainer = appContainer)
//            }
//        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    //App()
}
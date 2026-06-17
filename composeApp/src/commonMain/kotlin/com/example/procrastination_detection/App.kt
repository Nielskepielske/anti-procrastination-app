package com.example.procrastination_detection

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.procrastination_detection.navigation.Screen
import com.example.procrastination_detection.ui.dashboard.DashboardScreen
import com.example.procrastination_detection.ui.dictionary.DictionaryHubScreen
import com.example.procrastination_detection.ui.profile.ProfileManagerScreen
import com.example.procrastination_detection.ui.analytics.AnalyticsScreen
import com.example.procrastination_detection.ui.analytics.FlexibleAnalyticsScreen
import com.example.procrastination_detection.ui.theme.AppTheme
import com.example.procrastination_detection.domain.session.SessionManager
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch


@Composable
fun App() {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

  val sessionManager: SessionManager = koinInject()
  val orphanedSession by sessionManager.orphanedSessionFlow.collectAsState()
  val scope = rememberCoroutineScope()

  AppTheme {
    Scaffold(
      bottomBar = {
        NavigationBar {
          NavigationBarItem(
            icon = { Icon(Icons.Default.Home, "Dashboard") },
            label = { Text("Live") },
            selected = currentRoute?.contains("Dashboard") == true,
            onClick = { navController.navigate(Screen.Dashboard) }
          )
          NavigationBarItem(
            icon = { Icon(Icons.Default.List, "Dictionary") },
            label = { Text("Dictionary") },
            selected = currentRoute?.contains("DictionaryHub") == true,
            onClick = { navController.navigate(Screen.DictionaryHub) }
          )
          NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, "Profiles") },
            label = { Text("Profiles") },
            selected = currentRoute?.contains("ProfileManager") == true,
            onClick = { navController.navigate(Screen.ProfileManager) }
          )
          NavigationBarItem(
            icon = { Icon(Icons.Default.Info, "Analytics") },
            label = { Text("Stats") },
            selected = currentRoute?.contains("Analytics") == true,
            onClick = { navController.navigate(Screen.Analytics) }
          )
        }
      }
    ) { innerPadding ->
      // The Router
      NavHost(
        navController = navController,
        startDestination = Screen.Dashboard,
        modifier = Modifier.padding(innerPadding)
      ) {
        composable<Screen.Dashboard> {
          // Koin automatically creates the DashboardViewModel here!
          DashboardScreen(viewModel = koinViewModel())
        }
        composable<Screen.DictionaryHub> {
          DictionaryHubScreen(viewModel = koinViewModel())
        }
        composable<Screen.ProfileManager> {
          ProfileManagerScreen(viewModel = koinViewModel())
        }
        composable<Screen.Analytics> {
          FlexibleAnalyticsScreen(viewModel = koinViewModel())
        }
      }
    }
    
    // Blocking overlay for orphaned sessions
    if (orphanedSession != null) {
      Dialog(
        onDismissRequest = { /* Blocked */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
      ) {
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
          Column(modifier = Modifier.padding(24.dp)) {
            Text("Recovery: Active Session Detected", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            Text("An active tracking session was detected from a previous run. The app may have crashed or was closed unexpectedly.")
            Spacer(modifier = Modifier.height(24.dp))
            androidx.compose.foundation.layout.Row(
              horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
              modifier = Modifier.fillMaxWidth()
            ) {
              TextButton(onClick = { scope.launch { sessionManager.finishOrphanedSession() } }) {
                Text("Finish Session & Archive")
              }
              Spacer(modifier = Modifier.width(8.dp))
              androidx.compose.material3.Button(onClick = { scope.launch { sessionManager.continueOrphanedSession() } }) {
                Text("Continue Tracking")
              }
            }
          }
        }
      }
    }
  }
}

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
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun App() {
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

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
//          AnalyticsScreen(viewModel = koinViewModel())
          FlexibleAnalyticsScreen(viewModel = koinViewModel())
        }
      }
    }
  }
}

package com.example.procrastination_detection

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.procrastination_detection.engine.TrackingEngine
import com.example.procrastination_detection.interfaces.AppRepository
import com.example.procrastination_detection.navigation.Screen
import com.example.procrastination_detection.pages.home.AppListViewModel
import com.example.procrastination_detection.pages.home.HomeScreen
import com.example.procrastination_detection.pages.library.AppLibraryScreen
import com.example.procrastination_detection.pages.library.AppLibraryViewModel
import com.example.procrastination_detection.ui.theme.AppTheme


@Composable
fun App(repository: AppRepository) {
  val navController = rememberNavController()
  // Observe the current route to highlight the correct tab
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination

  val trackingEngine = remember { TrackingEngine(repository) }

  AppTheme {
    Scaffold(
      bottomBar = {
        NavigationBar {
          NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            // This is a simple way to check if we are on the Home screen
            selected = currentDestination?.route?.contains("Home") == true,
            onClick = { navController.navigate(Screen.Home) }
          )
          NavigationBarItem(
            icon = { Icon(Icons.Default.List, contentDescription = "Library") },
            label = { Text("Library") },
            selected = currentDestination?.route?.contains("AppLibrary") == true,
            onClick = { navController.navigate(Screen.AppLibrary) }
          )
          NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Rules") },
            label = { Text("Rules") },
            selected = currentDestination?.route?.contains("RulesManager") == true,
            onClick = { navController.navigate(Screen.RulesManager) }
          )
        }
      }
    ) { innerPadding ->
      // The NavHost sits INSIDE the Scaffold padding
      NavHost(
        navController = navController,
        startDestination = Screen.Home,
        modifier = Modifier.padding(innerPadding)
      ) {
        composable<Screen.Home> {
          HomeScreen(viewModel = viewModel { AppListViewModel(repository, trackingEngine) })
        }
        composable<Screen.AppLibrary> {
          AppLibraryScreen(viewModel = viewModel { AppLibraryViewModel(repository) })
        }
        composable<Screen.RulesManager> {
          // We will build this next!
          Text("Rules Manager Coming Soon", modifier = Modifier.fillMaxSize())
        }
      }
    }
  }
}

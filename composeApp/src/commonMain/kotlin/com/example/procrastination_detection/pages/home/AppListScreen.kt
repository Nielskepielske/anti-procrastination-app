package com.example.procrastination_detection.pages.home

// commonMain
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.procrastination_detection.database.RuleDao
import com.example.procrastination_detection.repositories.LocalAppRepository

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.procrastination_detection.repositories.TempAppRepository

@Composable
fun AppListScreen(
  // 1. Tell Compose how to build the ViewModel
  viewModel: AppListViewModel = viewModel {
    // For testing, we just inject the TempAppRepository.
    // Later, you'll swap this with LocalAppRepository.
    AppListViewModel(repository = TempAppRepository())
  }
) {
  // 2. Observe the Flow. Whenever the repo updates, this triggers a recomposition.
  val processes by viewModel.monitoredProcesses.collectAsState()

  // Optional: Start the monitoring loop when the screen appears
  LaunchedEffect(Unit) {
    viewModel.start()
  }

  // 3. Draw the UI
  LazyColumn {
    items(
      items = processes,
      key = { it.id } // Helps Compose animate and track list items efficiently
    ) { monitoredProcess ->

      // Just a simple text display for now
      Text(
        text = "${monitoredProcess.process.name} - ${monitoredProcess.totalSeconds}s",
      )

    }
  }
}

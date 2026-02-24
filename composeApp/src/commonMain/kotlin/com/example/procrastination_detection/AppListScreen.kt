package com.example.procrastination_detection

// commonMain
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.procrastination_detection.interfaces.AppScanner

@Composable
fun AppListScreen(scanner: AppScanner) {
  // This creates or retrieves the existing ViewModel
  val viewModel: AppListViewModel = viewModel { AppListViewModel(scanner) }

  val appList by viewModel.apps2.collectAsState()

  val state = rememberLazyListState()

  Box(modifier = Modifier.fillMaxWidth()) {
    LazyColumn(
      state = state,
    ) {
      items(appList
      ) { appName ->
        Text(text = "${appName}")
      }
    }
  }

}

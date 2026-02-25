package com.example.procrastination_detection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.procrastination_detection.interfaces.AppRepository
import com.example.procrastination_detection.pages.home.AppListScreen
import com.example.procrastination_detection.pages.home.AppListViewModel
import com.example.procrastination_detection.repositories.LocalAppRepository
import org.jetbrains.compose.resources.painterResource

import procrastination_detection.composeapp.generated.resources.Res
import procrastination_detection.composeapp.generated.resources.compose_multiplatform


@Composable
fun App(repository: AppRepository) {
  val navController = rememberNavController()
  MaterialTheme {
    // We pass the repository into the ViewModel Factory here
    AppListScreen(
      viewModel = viewModel { AppListViewModel(repository) }
    )
  }
}

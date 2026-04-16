package com.example.procrastination_detection.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.procrastination_detection.domain.model.Category

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val currentApp by viewModel.currentAppFlow.collectAsState("Waiting for activity...")
    val currentCategory by viewModel.currentCategoryFlow.collectAsState(Category.UNCATEGORIZED)
    val isTracking by viewModel.isTrackingFlow.collectAsState(false)

    val categoryColor = when (currentCategory) {
        Category.DISTRACTING   -> MaterialTheme.colorScheme.error
        Category.PRODUCTIVE    -> MaterialTheme.colorScheme.primary
        Category.AMBIGUOUS     -> MaterialTheme.colorScheme.tertiary
        else                   -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Center
    ) {
        // Title
        Text(
            text = "Active Profile: Deep Work", // Later, this comes from ViewModel
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(32.dp))

        // The Live Data Card
        Card(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Center
            ) {
                Text(text = "Current Focus", style = MaterialTheme.typography.labelLarge)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentApp,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Colored Category Badge
                Surface(
                    color = categoryColor.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = currentCategory.name,
                        color = categoryColor,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // The Master Control Button
        Button(
            onClick = {
                if (isTracking) viewModel.stopTracking() else viewModel.startTracking()
            },
            modifier = Modifier.width(200.dp).height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isTracking) "Stop Tracking" else "Start Tracking",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
package com.example.procrastination_detection.pages.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension

@Composable
fun HomeScreen(viewModel: AppListViewModel) {
    // 1. Observe the State
    val processes by viewModel.monitoredProcesses.collectAsState(initial = emptyList())
    val activeApp by viewModel.currentActiveApp.collectAsState()
    val isProcrastinating by viewModel.isProcrastinating.collectAsState()
    val consecutiveSeconds by viewModel.consecutiveSeconds.collectAsState()

    val isTracking by viewModel.isTracking.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.toggleTracking() },
                // Change color to Red when tracking (to signify 'Stop') and Primary when paused
                containerColor = if (isTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(
                    imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isTracking) "Stop Session" else "Start Session"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // --- HEADER: Current Status ---
            StatusCard(activeApp = activeApp, isProcrastinating = isProcrastinating, consecutiveSeconds = consecutiveSeconds.toInt())

            Spacer(modifier = Modifier.Companion.height(24.dp))

            Text(
                text = "Session Stats",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Companion.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.Companion.height(8.dp))

            // --- BODY: List of Apps ---
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(processes, key = { it.id }) { process ->
                    AppStatRow(
                        appName = process.process.name,
                        seconds = process.totalSeconds,
                        isProductive = process.process.category.isProductive
                    )
                }
            }
        }
    }
}

@Composable
fun StatusCard(activeApp: String, isProcrastinating: Boolean, consecutiveSeconds: Int = 0) {
    val statusColor = if (isProcrastinating) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val statusText = if (isProcrastinating) "PROCRASTINATING" else "FOCUSING"

    Card(
        modifier = Modifier.Companion.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ){
            val (column, test) = createRefs()
            Text(
                text = consecutiveSeconds.toString(),
                modifier = Modifier.constrainAs(test) {
                centerVerticallyTo(parent)
                end.linkTo(parent.end, margin = 16.dp)
            },
               style = MaterialTheme.typography.headlineMedium,
            )
            Column(
                modifier = Modifier.constrainAs(column) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end, margin = 50.dp)
                    width = Dimension.fillToConstraints
                }
            ) {
                Text(text = "Currently Using", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = activeApp,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

        }
    }
}

@Composable
fun AppStatRow(appName: String, seconds: Long, isProductive: Boolean) {
    val minutes = seconds / 60
    val indicatorColor = if (isProductive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Row(
        modifier = Modifier.Companion
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Companion.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.Companion.CenterVertically) {
            // Little color dot
            Box(
                modifier = Modifier.Companion.size(12.dp)
                    .background(indicatorColor, RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.Companion.width(12.dp))
            Text(text = appName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Companion.Medium)
        }
        Text(text = "${minutes}m ${seconds % 60}s", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}
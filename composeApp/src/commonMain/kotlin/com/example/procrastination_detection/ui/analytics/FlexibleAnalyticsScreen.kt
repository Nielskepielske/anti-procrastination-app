package com.example.procrastination_detection.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.reflect.KClass
import com.example.procrastination_detection.domain.model.analytics.*
import com.example.procrastination_detection.ui.analytics.components.*

// --- The Registry Map ---
val chartRegistry = mapOf<KClass<out ChartData>, @Composable (ChartData) -> Unit>(
    ChartData.Progress::class to { data -> ProgressChartComposable(data as ChartData.Progress) },
    ChartData.Bar::class to { data -> BarChartComposable(data as ChartData.Bar) },
    ChartData.Line::class to { data -> LineChartComposable(data as ChartData.Line) }
)

// --- Main Screen ---
@Composable
fun FlexibleAnalyticsScreen(viewModel: FlexibleAnalyticsViewModel) {
    val blocks by viewModel.blocks.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(blocks, key = { it.id }) { block ->
            DashboardBlockWrapper(
                block = block,
                onTimeRangeChanged = { newRange -> viewModel.updateTimeRange(block.id, newRange) }
            )
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

// --- The Card Wrapper ---
@Composable
fun DashboardBlockWrapper(
    block: DashboardBlock,
    onTimeRangeChanged: (TimeRange) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header & Time Range Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(block.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                // Button Bar
                Row(modifier = Modifier.background(Color.Gray.copy(alpha=0.1f), RoundedCornerShape(8.dp))) {
                    TimeRange.entries.forEach { range ->
                        val isSelected = block.timeRange == range
                        Text(
                            text = range.displayName,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onTimeRangeChanged(range) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Render Chart Data using Registry
            when (block) {
                is SingleChartBlock -> {
                    if (block.chartData == null) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val renderer = chartRegistry[block.chartData::class]
                        renderer?.invoke(block.chartData)
                    }
                }
                is CombinedChartBlock -> {
                    Text("Combination view not yet implemented")
                }
            }
        }
    }
}
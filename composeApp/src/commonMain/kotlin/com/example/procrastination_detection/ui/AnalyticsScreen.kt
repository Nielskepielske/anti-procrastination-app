package com.example.procrastination_detection.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.procrastination_detection.domain.model.Category
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.procrastination_detection.data.local.HourlyCount

@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel) {
    val todayBreakdown by viewModel.todayBreakdown.collectAsState()
    val focusScore by viewModel.focusScore.collectAsState()
    val switchesPerHour by viewModel.appSwitchesPerHour.collectAsState()
    val weekSummary by viewModel.weekSummary.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            AnalyticsHeader(focusScore)
        }

        item {
            TodayCategoryBreakdown(todayBreakdown)
        }

        item {
            TopAppsChart(todayBreakdown.take(5))
        }

        item {
            SwitchFrequencyChart(switchesPerHour)
        }

        item {
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun AnalyticsHeader(score: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    drawArc(
                        color = Color.Gray.copy(alpha = 0.2f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFF4CAF50))),
                        startAngle = -90f,
                        sweepAngle = (score.toFloat() / 100f) * 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$score%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Focus", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column {
                Text("Daily Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                    Text(" +12% vs yesterday", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your focus score is excellent today. You've minimized distractions effectively.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TodayCategoryBreakdown(rows: List<UsageRow>) {
    val totalSeconds = rows.sumOf { it.totalSeconds }.coerceAtLeast(1L)
    val productive = rows.filter { it.category == Category.PRODUCTIVE }.sumOf { it.totalSeconds }.toFloat() / totalSeconds
    val distracting = rows.filter { it.category == Category.DISTRACTING }.sumOf { it.totalSeconds }.toFloat() / totalSeconds
    val other = 1f - productive - distracting

    Column {
        Text("Time Distribution", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Box(Modifier.fillMaxHeight().weight(productive.coerceAtLeast(0.01f)).background(Color(0xFF4CAF50)))
            Box(Modifier.fillMaxHeight().weight(distracting.coerceAtLeast(0.01f)).background(Color(0xFFF44336)))
            Box(Modifier.fillMaxHeight().weight(other.coerceAtLeast(0.01f)).background(Color(0xFFFFC107)))
        }

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LegendItem("Productive", Color(0xFF4CAF50), (productive * 100).toInt())
            LegendItem("Distracting", Color(0xFFF44336), (distracting * 100).toInt())
            LegendItem("Ambiguous", Color(0xFFFFC107), (other * 100).toInt())
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color, percentage: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(6.dp))
        Text("$label ($percentage%)", style = MaterialTheme.typography.labelSmall)
    }
}

private fun formatDuration(totalSeconds: Long): String {
    return when {
        totalSeconds < 60 -> "${totalSeconds}s"
        totalSeconds % 60 == 0L -> "${totalSeconds / 60}m"
        else -> "${totalSeconds / 60}m ${totalSeconds % 60}s"
    }
}

@Composable
fun TopAppsChart(topRows: List<UsageRow>) {
    val maxTime = topRows.firstOrNull()?.totalSeconds?.toFloat()?.coerceAtLeast(1f) ?: 1f
    
    Column {
        Text("Top Activities", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        topRows.forEach { row ->
            Column(Modifier.padding(vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(row.processName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(formatDuration(row.totalSeconds), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(row.totalSeconds.toFloat() / maxTime)
                        .height(8.dp)
                        .background(
                            color = when(row.category) {
                                Category.PRODUCTIVE -> Color(0xFF4CAF50)
                                Category.DISTRACTING -> Color(0xFFF44336)
                                else -> Color(0xFFFFC107)
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun SwitchFrequencyChart(counts: List<HourlyCount>) {
    val maxCount = counts.maxOfOrNull { it.frequency }?.toFloat()?.coerceAtLeast(5f) ?: 5f
    
    Column {
        Text("App Switch Frequency", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Context switching over 24 hours", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepX = size.width / 24f
                val path = Path().apply {
                    moveTo(0f, size.height)
                    for (hour in 0..23) {
                        val count = counts.find { it.hourBucket % 24 == hour.toLong() }?.frequency?.toFloat() ?: 0f
                        val x = hour * stepX
                        val y = size.height - (count / maxCount * size.height)
                        if (hour == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                
                // Fill
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    fillPath,
                    brush = Brush.verticalGradient(listOf(Color(0xFF2196F3).copy(alpha = 0.3f), Color.Transparent))
                )
                
                // Line
                drawPath(
                    path,
                    color = Color(0xFF2196F3),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Grid lines (horizontal)
                for (i in 0..2) {
                    val y = size.height * (i / 2f)
                    drawLine(Color.Gray.copy(alpha = 0.2f), Offset(0f, y), Offset(size.width, y))
                }
            }
        }
        
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("00:00", style = MaterialTheme.typography.labelSmall)
            Text("12:00", style = MaterialTheme.typography.labelSmall)
            Text("23:59", style = MaterialTheme.typography.labelSmall)
        }
    }
}

package com.example.procrastination_detection.ui.analytics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.procrastination_detection.domain.model.analytics.ChartData

@Composable
fun ProgressChartComposable(data: ChartData.Progress) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
            Canvas(modifier = Modifier.size(100.dp)) {
                drawArc(
                    color = Color.Gray.copy(alpha = 0.2f),
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFF4CAF50))),
                    startAngle = -90f, sweepAngle = (data.percentage.toFloat() / 100f) * 360f,
                    useCenter = false, style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${data.percentage}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(data.label, style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        Column {
            Text("Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                Text(" ${data.detail}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun BarChartComposable(data: ChartData.Bar) {
    Column {
        data.items.forEach { item ->
            Column(Modifier.padding(vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(item.detail, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (data.maxValue > 0) item.value / data.maxValue else 0f)
                        .height(8.dp)
                        .background(color = item.color, shape = RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
fun LineChartComposable(data: ChartData.Line) {
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (data.points.isEmpty()) return@Canvas

                val stepX = size.width / (data.points.size.coerceAtLeast(2) - 1)
                val path = Path().apply {
                    data.points.forEachIndexed { index, point ->
                        val x = index * stepX
                        val y = size.height - (point / data.maxPoint * size.height)
                        if (index == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }

                // Line
                drawPath(
                    path, color = Color(0xFF2196F3),
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
            data.labels.forEach { label ->
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
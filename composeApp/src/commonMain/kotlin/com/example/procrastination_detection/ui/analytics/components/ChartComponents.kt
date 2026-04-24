package com.example.procrastination_detection.ui.analytics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.FloatLinearAxisModel
import io.github.koalaplot.core.xygraph.DefaultPoint
import io.github.koalaplot.core.line.LinePlot
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi

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

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun LineChartComposable(data: ChartData.Line) {
    if (data.lines.isEmpty() || data.lines.all { it.points.isEmpty() }) {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Text("No data", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        return
    }

    val maxPoints = data.lines.maxOf { it.points.size }.coerceAtLeast(2)
    val safeMax = data.maxPoint.coerceAtLeast(1f)

    Column {
        XYGraph<Float, Float>(
            xAxisModel = FloatLinearAxisModel(
                range = 0f..(maxPoints - 1).toFloat(),
                allowZooming = true,
                allowPanning = true
            ),
            yAxisModel = FloatLinearAxisModel(
                range = 0f..safeMax,
                allowZooming = true,
                allowPanning = true
            ),
            modifier = Modifier.fillMaxWidth().height(160.dp),
            xAxisLabels = { value: Float ->
                val step = (maxPoints - 1).coerceAtLeast(1).toFloat()
                val positions = if (data.labels.size > 1)
                    data.labels.indices.map { it * step / (data.labels.size - 1) }
                else listOf(0f)
                
                val nearest = positions.minByOrNull { kotlin.math.abs(it - value) } ?: 0f
                val labelIdx = positions.indexOf(nearest).coerceIn(0, data.labels.size - 1)
                
                val labelText = if (kotlin.math.abs(value - nearest) < 0.1f) {
                    data.labels.getOrElse(labelIdx) { "" }
                } else {
                    ""
                }
                Text(labelText, style = MaterialTheme.typography.labelSmall)
            },
            xAxisTitle = {},
            yAxisLabels = { value: Float ->
                val labelText = if (value == 0f) "" else "${value.toInt()}"
                Text(labelText, style = MaterialTheme.typography.labelSmall)
            },
            yAxisTitle = {}
        ) {
            for (lineData in data.lines) {
                if (lineData.points.isNotEmpty()) {
                    val points = lineData.points.mapIndexed { i, v ->
                        DefaultPoint<Float, Float>(i.toFloat(), v)
                    }
                    LinePlot<Float, Float>(
                        data = points,
                        lineStyle = LineStyle(
                            brush = SolidColor(lineData.color),
                            strokeWidth = 2.5.dp
                        ),
                        symbol = { point ->
                            // Small circle with hover tooltip showing the exact Y value
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .hoverableElement {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF212121),
                                            shadowElevation = 4.dp
                                        ) {
                                            Text(
                                                text = "%.2f".format(point.y),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(7.dp)) {
                                    drawCircle(color = lineData.color)
                                }
                            }
                        }
                    )
                }
            }
        }

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            data.lines.forEach { lineData ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(lineData.color, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(lineData.name, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

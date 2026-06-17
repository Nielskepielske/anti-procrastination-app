package com.example.procrastination_detection.ui.analytics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.constraintlayout.compose.Dimension.Companion.value
import com.example.procrastination_detection.domain.model.analytics.ChartData
import com.example.procrastination_detection.domain.model.EscalationLevel
import com.example.procrastination_detection.ui.analytics.strategy.formatBucketTimestamp
import io.github.koalaplot.core.ChartLayout
import io.github.koalaplot.core.gestures.GestureConfig
import io.github.koalaplot.core.xygraph.FloatLinearAxisModel
import io.github.koalaplot.core.xygraph.DefaultPoint
import io.github.koalaplot.core.line.LinePlot2
import io.github.koalaplot.core.style.AreaStyle
import io.github.koalaplot.core.style.KoalaPlotTheme
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.AxisContent
import io.github.koalaplot.core.xygraph.CategoryAxisModel
import io.github.koalaplot.core.xygraph.GridStyle
import io.github.koalaplot.core.xygraph.Point
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.XYGraphScope
import io.github.koalaplot.core.xygraph.rememberAxisStyle
import kotlin.math.abs

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
                    brush = Brush.sweepGradient(
                        listOf(
                            data.color ?: Color(0xFF4CAF50),
                            data.color?.copy(alpha = 0.5f) ?: Color(0xFF8BC34A),
                            data.color ?: Color(0xFF4CAF50)
                        )
                    ),
                    startAngle = -90f, sweepAngle = (data.percentage.toFloat() / 100f) * 360f,
                    useCenter = false, style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${data.percentage}${data.valueSuffix}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(data.label, style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        Column {
            Text("Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val tintColor = data.color ?: Color(0xFF4CAF50)
                Icon(Icons.Default.TrendingUp, null, tint = tintColor, modifier = Modifier.size(16.dp))
                Text(" ${data.detail}", style = MaterialTheme.typography.bodySmall, color = tintColor)
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
    if (data.lines.isEmpty() || data.lines.all { it.points.isEmpty() }) {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Text("No data", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        return
    }

    val baseTime = data.xCategories.firstOrNull() ?: 0L
    val totalDuration = ((data.xCategories.lastOrNull() ?: 0L) - baseTime).toFloat()

    // Determine default view span based on bucket sizes
    val bucketSpan = if (data.xCategories.size > 1) data.xCategories[1] - data.xCategories[0] else 0L
    val viewSpan = when {
        bucketSpan <= 60_000L -> 3_600_000f       // Hourly: 1 hour view
        bucketSpan <= 3_600_000L -> 86_400_000f     // Daily: 24 hours view
        else -> 604_800_000f                       // Weekly: 7 days view
    }

    // Min and max bounds for zooming
    val minZoomWidth = when {
        bucketSpan <= 60_000L -> 3_600_000f       // min zoom 1 hour
        bucketSpan <= 3_600_000L -> 86_400_000f     // min zoom 1 day
        else -> 604_800_000f                       // min zoom 7 days
    }
    val maxZoomWidth = totalDuration.coerceAtLeast(minZoomWidth)

    // Initial viewport range should show the LAST viewSpan of the data
    var viewportStart by remember(baseTime, totalDuration) {
        mutableStateOf((totalDuration - viewSpan).coerceAtLeast(0f))
    }
    var viewportEnd by remember(baseTime, totalDuration) {
        mutableStateOf(totalDuration)
    }

    // Recalculate local max Y-value based only on points in the visible viewport range
    val visibleMax = data.lines.flatMap { dataset ->
        dataset.points.filterIndexed { index, _ ->
            val timestamp = data.xCategories.getOrNull(index) ?: 0L
            val normalizedX = (timestamp - baseTime).toFloat()
            normalizedX >= viewportStart && normalizedX <= viewportEnd
        }
    }.maxOrNull() ?: 5f

    val safeMax = if (data.valueSuffix == "%") 100f else (visibleMax * 1.1f).coerceAtLeast(5f)
    val animatedSafeMax by animateFloatAsState(targetValue = safeMax, animationSpec = tween(300))

    var graphWidthPx by remember { mutableStateOf(1f) }

    val xAxisModel = FloatLinearAxisModel(
        range = viewportStart..viewportEnd,
        minimumMajorTickSpacing = 50.dp
    )
    val yAxisModel = FloatLinearAxisModel(
        range = 0f..animatedSafeMax,
        minimumMajorTickSpacing = 50.dp
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(baseTime, totalDuration) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val currentWidth = viewportEnd - viewportStart
                    
                    // Handle Zoom
                    if (zoom != 1f) {
                        val newWidth = (currentWidth / zoom).coerceIn(minZoomWidth, maxZoomWidth)
                        
                        // We want to zoom relative to the centroid (the point between fingers)
                        val centroidFraction = (centroid.x / graphWidthPx).coerceIn(0f, 1f)
                        val focusTime = viewportStart + centroidFraction * currentWidth
                        
                        var targetStart = focusTime - centroidFraction * newWidth
                        var targetEnd = targetStart + newWidth
                        
                        // Keep in bounds [0..totalDuration]
                        if (targetStart < 0f) {
                            targetStart = 0f
                            targetEnd = newWidth
                        } else if (targetEnd > totalDuration) {
                            targetEnd = totalDuration
                            targetStart = (totalDuration - newWidth).coerceAtLeast(0f)
                        }
                        
                        viewportStart = targetStart
                        viewportEnd = targetEnd
                    }
                    
                    // Handle Pan
                    if (zoom == 1f && pan.x != 0f) {
                        val panDelta = -pan.x / graphWidthPx * currentWidth
                        val newStart = (viewportStart + panDelta).coerceIn(0f, (totalDuration - currentWidth).coerceAtLeast(0f))
                        viewportStart = newStart
                        viewportEnd = newStart + currentWidth
                    }
                }
            }
    ) {
        XYGraph(
            xAxisModel = xAxisModel,
            yAxisModel = yAxisModel,
            xAxisContent = AxisContent(
                labels = { normalizedX ->
                    val actualTimestamp = baseTime + normalizedX.toLong()
                    Text(
                        text = formatBucketTimestamp(actualTimestamp, totalDuration.toLong()),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.wrapContentWidth(unbounded = true),
                        maxLines = 1,
                        overflow = TextOverflow.Visible
                    )
                },
                title = {},
                style = rememberAxisStyle()
            ),
            yAxisContent = AxisContent(
                labels = { AxisLabel("${it.toInt()}${data.valueSuffix}") },
                title = {},
                style = rememberAxisStyle()
            ),
            gridStyle = GridStyle(
                horizontalMajorStyle = LineStyle(
                    brush = SolidColor(Color.Gray.copy(alpha = 0.2f)),
                    strokeWidth = 1.dp
                ),
                horizontalMinorStyle = null,
                verticalMajorStyle = LineStyle(
                    brush = SolidColor(Color.Gray.copy(alpha = 0.2f)),
                    strokeWidth = 1.dp
                ),
                verticalMinorStyle = null
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(horizontal = 16.dp)
                .onGloballyPositioned { graphWidthPx = it.size.width.toFloat() },
        ) {
            data.lines.forEach { dataset ->
                val plotPoints = dataset.points.mapIndexedNotNull { index, yValue ->
                    val timestamp = data.xCategories.getOrNull(index)
                    if (timestamp != null) {
                        val normalizedX = (timestamp - baseTime).toFloat()
                        Point(normalizedX, yValue)
                    } else {
                        null
                    }
                }

                LinePlot2(
                    data = plotPoints,
                    lineStyle = LineStyle(brush = SolidColor(dataset.color), strokeWidth = 2.dp),
                    symbol = { point ->
                        var isHovered by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            when (event.type) {
                                                PointerEventType.Enter -> isHovered = true
                                                PointerEventType.Exit -> isHovered = false
                                            }
                                        }
                                    }
                                }
                                .zIndex(10f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isHovered) Color.White else dataset.color,
                                modifier = Modifier.size(8.dp)
                            ) {}

                            if (isHovered) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.DarkGray.copy(alpha = 0.9f),
                                    modifier = Modifier
                                        .offset(y = if (animatedSafeMax - point.y <= 3f) 12.dp else (-12).dp)
                                        .wrapContentSize()
                                ) {
                                    Text(
                                        text = "${point.y.toInt()}${data.valueSuffix}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }

            // Draw Intervention Overlays
            data.overlays.forEach { overlay ->
                val normalizedX = (overlay.timestamp - baseTime).toFloat()
                
                // Only draw if within bounds (give a little padding)
                if (normalizedX >= (viewportStart - 1000f) && normalizedX <= (viewportEnd + 1000f)) {
                    val lineColor = when (overlay.aggressionLevel) {
                        EscalationLevel.GENTLE -> Color(0xFF14B8A6).copy(alpha = 0.5f) // Teal
                        EscalationLevel.FIRM -> Color(0xFFF59E0B).copy(alpha = 0.5f) // Amber
                        EscalationLevel.AGGRESSIVE -> Color(0xFFF43F5E).copy(alpha = 0.5f) // Rose
                    }
                    
                    val linePoints = listOf(
                        Point(normalizedX, 0f),
                        Point(normalizedX, animatedSafeMax)
                    )
                    
                    LinePlot2(
                        data = linePoints,
                        lineStyle = LineStyle(brush = SolidColor(lineColor), strokeWidth = 2.dp),
                        symbol = { null }
                    )
                }
            }
        }

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 16.dp, end = 16.dp),
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

@Composable
fun AxisTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        title,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier,
    )
}

@Composable
fun AxisLabel(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        label,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
    )
}

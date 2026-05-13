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
import androidx.constraintlayout.compose.Dimension.Companion.value
import com.example.procrastination_detection.domain.model.analytics.ChartData
import com.example.procrastination_detection.ui.analytics.strategy.formatBucketTimestamp
import io.github.koalaplot.core.ChartLayout
import io.github.koalaplot.core.gestures.GestureConfig
import io.github.koalaplot.core.xygraph.FloatLinearAxisModel
import io.github.koalaplot.core.xygraph.DefaultPoint
import io.github.koalaplot.core.line.LinePlot2
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

    // 2. Define upper bound for Y-axis dynamically
    val safeMax = if (data.valueSuffix == "%") 100f else (data.maxPoint * 1.1f).coerceAtLeast(5f)

    // 3. X-Axis represents your categorical labels directly
    val xAxisModel = CategoryAxisModel(data.xCategories)
    val yAxisModel = FloatLinearAxisModel(range = 0f..safeMax, minimumMajorTickSpacing = 50.dp)
    val totalDurationMillis = if (data.xCategories.isNotEmpty()) {
        data.xCategories.last() - data.xCategories.first()
    } else {
        0L
    }

    val targetLabelCount = 6
    val step = (data.xCategories.size / targetLabelCount).coerceAtLeast(1)

    XYGraph(
        xAxisModel = xAxisModel,
        yAxisModel = yAxisModel,
        xAxisContent = AxisContent(
            labels = { timestamp ->
                val index = data.xCategories.indexOf(timestamp)

                // 2. Only draw every nth label
                if (index % step == 0 || index == data.xCategories.lastIndex) {
                    // 3. DO NOT use the default AxisLabel. Use a custom Text composable
                    // and force it to be unbounded so it doesn't truncate to "1..."
                    Text(
                        text = formatBucketTimestamp(timestamp, totalDurationMillis),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, // Or your preferred color
                        style = MaterialTheme.typography.bodySmall,
                        // THIS IS THE MAGIC LINE:
                        modifier = Modifier.wrapContentWidth(unbounded = true),
                        maxLines = 1,
                        overflow = TextOverflow.Visible // Force it to draw outside its tiny box
                    )
                }
            },
            title = {},
            style = rememberAxisStyle()
        ),
        yAxisContent = AxisContent(
            labels = { AxisLabel(it.toString()) },
            title = {},
            style = rememberAxisStyle()
        ),
// 1. Configure the Horizontal Grid Lines
        gridStyle = GridStyle(
            horizontalMajorStyle = LineStyle(
                // Dim the color by dropping the alpha to 20%
                brush = SolidColor(Color.Gray.copy(alpha = 0.2f)),
                strokeWidth = 1.dp
            ),
            // Optional: Set to null if you want to completely hide minor lines
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
            .padding(horizontal = 16.dp),
    ) {
        data.lines.forEach { dataset ->

            // 3. Map the points using the unique Long timestamps
            val plotPoints = dataset.points.mapIndexedNotNull { index, yValue ->
                val timestamp = data.xCategories.getOrNull(index)
                if (timestamp != null) {
                    // Point<Long, Float> - absolutely unique, no overlapping!
                    Point(timestamp, yValue)
                } else {
                    null
                }
            }

            LinePlot2(
                data = plotPoints,
                lineStyle = LineStyle(brush = SolidColor(dataset.color), strokeWidth = 2.dp),

                // ADD THIS: The symbol lambda is called for every single point on the line
                symbol = { point ->
                    // 1. Remember the hover state for this specific dot
                    var isHovered by remember { mutableStateOf(false) }

                    // 2. Create an invisible hit-box that is slightly larger than the dot
                    // to make it easier for the user to trigger the hover
                    Box(
                        modifier = Modifier
                            .size(24.dp) // Generous hit-area
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        // Update state based on mouse enter/exit
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
                        // 3. Draw the actual visible dot on the line
                        Surface(
                            shape = CircleShape,
                            // Optional: Make the dot light up when hovered
                            color = if (isHovered) Color.White else dataset.color,
                            modifier = Modifier.size(8.dp)
                        ) {}

                        // 4. If hovered, draw the floating tooltip!
                        if (isHovered) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.DarkGray.copy(alpha = 0.9f),
                                // Push the tooltip UP so the mouse cursor doesn't block it
                                modifier = Modifier
                                    .offset(y = if(safeMax - point.y  <= 3) 12.dp else (-12).dp)
                                    .wrapContentSize()
                            ) {
                                Text(
                                    // Display the exact Y value
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

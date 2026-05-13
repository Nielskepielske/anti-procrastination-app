package com.example.procrastination_detection.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.runtime.mutableStateMapOf
import kotlin.reflect.KClass
import com.example.procrastination_detection.domain.model.analytics.*
import com.example.procrastination_detection.ui.analytics.components.*

/** Parses a hex color string like "#2196F3" into a Compose Color. Returns null on failure. */
fun hexToColor(hex: String): Color? = try {
    val clean = hex.trimStart('#')
    val value = clean.toLong(16)
    // If 6 hex digits, prepend full alpha
    val argb = if (clean.length == 6) (0xFF000000L or value) else value
    Color(argb.toInt())
} catch (e: Exception) {
    null
}

@Composable
fun ColorPicker(
    selectedColorHex: String?,
    onColorSelected: (String?) -> Unit,
    size: Int = 30
) {
    val colorPalette = listOf(
        null to "Default",
        "#2196F3" to "Blue",
        "#E91E63" to "Pink",
        "#4CAF50" to "Green",
        "#FF9800" to "Orange",
        "#9C27B0" to "Purple",
        "#F44336" to "Red",
        "#00BCD4" to "Cyan"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        colorPalette.forEach { (hex, _) ->
            val swatchColor = if (hex == null) MaterialTheme.colorScheme.onSurface
            else hexToColor(hex) ?: Color.Gray
            val isSelected = selectedColorHex == hex
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(if (hex == null) MaterialTheme.colorScheme.surfaceVariant else swatchColor)
                    .border(if (isSelected) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { onColorSelected(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (hex == null) {
                    Text("D", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


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

    var showAddDialog by remember { mutableStateOf(false) }

    // Drag and Drop state
    val blockBounds = remember { mutableStateMapOf<String, Rect>() }
    var draggedBlockId by remember { mutableStateOf<String?>(null) }
    var dropTargetId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    if (showAddDialog) {
        AddBlockDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false },
            onAdd = { title, strategyId, sensorId ->
                viewModel.addBlock(title, strategyId, sensorId)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                showAddDialog = true
            }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(16.dp)) }
            items(blocks, key = { it.id }) { block ->
                DashboardBlockWrapper(
                    block = block,
                    allBlocks = blocks,
                    viewModel = viewModel,
                    onTimeRangeChanged = { newRange -> viewModel.updateTimeRange(block.id, newRange) },
                    blockBounds = blockBounds,
                    draggedBlockId = draggedBlockId,
                    dropTargetId = dropTargetId,
                    dragOffset = dragOffset,
                    onDragStart = { id ->
                        draggedBlockId = id
                        dragOffset = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        dragOffset += dragAmount
                        val draggedBounds = blockBounds[draggedBlockId]
                        if (draggedBounds != null) {
                            val pointerGlobal = draggedBounds.topLeft + change.position
                            dropTargetId = blockBounds.entries.find { (id, bounds) ->
                                id != draggedBlockId && bounds.contains(pointerGlobal)
                            }?.key
                        }
                    },
                    onDragEnd = {
                        if (draggedBlockId != null && dropTargetId != null && draggedBlockId != dropTargetId) {
                            if (viewModel.canCombine(draggedBlockId!!, dropTargetId!!)) {
                                viewModel.combineBlocks(draggedBlockId!!, dropTargetId!!, "Combined Chart")
                            }
                        }
                        draggedBlockId = null
                        dropTargetId = null
                        dragOffset = Offset.Zero
                    },
                    onDragCancel = {
                        draggedBlockId = null
                        dropTargetId = null
                        dragOffset = Offset.Zero
                    }
                )
            }
            item { Spacer(Modifier.height(80.dp)) } // padding for FAB
        }
    }
}

// --- The Card Wrapper ---
@Composable
fun DashboardBlockWrapper(
    block: DashboardBlock,
    allBlocks: List<DashboardBlock>,
    viewModel: FlexibleAnalyticsViewModel,
    onTimeRangeChanged: (TimeRange) -> Unit,
    blockBounds: MutableMap<String, Rect>,
    draggedBlockId: String?,
    dropTargetId: String?,
    dragOffset: Offset,
    onDragStart: (String) -> Unit,
    onDrag: (androidx.compose.ui.input.pointer.PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    var showMergeMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        when (block) {
            is SingleChartBlock -> {
                EditBlockDialog(
                    block = block,
                    viewModel = viewModel,
                    onDismiss = { showEditDialog = false },
                    onSave = { newTitle, newStrategy, newColorHex ->
                        viewModel.renameBlock(block.id, newTitle)
                        viewModel.editBlockStrategy(block.id, newStrategy)
                        viewModel.editBlockColor(block.id, newColorHex)
                        showEditDialog = false
                    },
                    onDelete = { viewModel.deleteBlock(block.id) }
                )
            }

            is CombinedChartBlock -> {
                EditCombinedBlockDialog(
                    block = block,
                    viewModel = viewModel,
                    onDismiss = { showEditDialog = false }
                )
            }
        }
    }

    val isDragged = draggedBlockId == block.id
    val isTarget = dropTargetId == block.id && viewModel.canCombine(draggedBlockId ?: "", block.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                blockBounds[block.id] = coordinates.boundsInWindow()
            }
            .pointerInput(block.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart(block.id) },
                    onDrag = { change, dragAmount -> 
                        change.consume()
                        onDrag(change, dragAmount) 
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel
                )
            }
            .then(
                if (isDragged) Modifier
                    .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                    .zIndex(10f)
                else Modifier.zIndex(0f)
            )
            .then(
                if (isTarget) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDragged) 0.8f else 0.5f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header & Time Range Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(block.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                // Button Bar & Merge Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp))) {
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

                    val otherBlocks = allBlocks.filter { other ->
                        other.id != block.id && viewModel.canCombine(block.id, other.id)
                    }
                    if (otherBlocks.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { showMergeMenu = true }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Merge with block")
                            }
                            DropdownMenu(expanded = showMergeMenu, onDismissRequest = { showMergeMenu = false }) {
                                otherBlocks.forEach { other ->
                                    DropdownMenuItem(
                                        text = { Text("Merge with ${other.title}") },
                                        onClick = {
                                            viewModel.combineBlocks(block.id, other.id, "Combined Chart")
                                            showMergeMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = { showEditDialog = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit block")
                    }
                    if (block is CombinedChartBlock) {
                        IconButton(onClick = { viewModel.unmergeBlock(block.id) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.RemoveCircle, contentDescription = "Unmerge block")
                        }
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
                        // Apply custom color if set
                        val customColor = block.colorHex?.let { hexToColor(it) }
                        val tintedData = if (customColor != null && block.chartData is ChartData.Line) {
                            (block.chartData as ChartData.Line).copy(
                                lines = block.chartData.lines.map { it.copy(color = customColor) }
                            )
                        } else {
                            block.chartData
                        }
                        val renderer = chartRegistry[tintedData::class]
                        renderer?.invoke(tintedData)
                    }
                }

                is CombinedChartBlock -> {
                    // Check if all children have loaded data and are Line charts
                    val allLoaded = block.childBlocks.all { it.chartData != null }
                    val allLines = allLoaded && block.childBlocks.all { it.chartData is ChartData.Line }

                    if (!allLoaded) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (allLines) {
                        // Merge them into a single multi-line chart!
                        val colors =
                            listOf(Color.Blue, Color.Red, Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF9C27B0))
                        val mergedLines = block.childBlocks.flatMapIndexed { childIndex, child ->
                            val lines = (child.chartData as ChartData.Line).lines
                            val customColor = child.colorHex?.let { hexToColor(it) }

                            lines.mapIndexed { lineIndex, line ->
                                val color = customColor ?: colors[(childIndex * 10 + lineIndex) % colors.size]
                                line.copy(color = color, name = "${child.title} - ${line.name}")
                            }
                        }
                        val maxPoint = block.childBlocks.maxOf { (it.chartData as ChartData.Line).maxPoint }
                        val labels = (block.childBlocks.first().chartData as ChartData.Line).xCategories

                        val mergedChartData = ChartData.Line(mergedLines, maxPoint, labels)
                        LineChartComposable(mergedChartData)
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            block.childBlocks.forEachIndexed { index, child ->
                                Text(
                                    child.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                val renderer = chartRegistry[child.chartData!!::class]
                                renderer?.invoke(child.chartData!!)

                                if (index < block.childBlocks.size - 1) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 32.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBlockDialog(
    viewModel: FlexibleAnalyticsViewModel,
    onDismiss: () -> Unit,
    onAdd: (String, String, String?) -> Unit
) {
    var title by remember { mutableStateOf("New Block") }

    val allSensors by viewModel.availableSensors.collectAsState()
    val sensorOptions = remember(allSensors) { listOf(Pair("all", "All Sensors")) + allSensors }
    var selectedSensor by remember { mutableStateOf<String?>("all") }
    var sensorExpanded by remember { mutableStateOf(false) }

    val strategies = viewModel.strategiesForSensor(selectedSensor)
    var selectedStrategy by remember(selectedSensor) { mutableStateOf(strategies.firstOrNull()?.first ?: "") }
    var strategyExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Analytics Block") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Block Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Sensor Dropdown First
                ExposedDropdownMenuBox(
                    expanded = sensorExpanded,
                    onExpandedChange = { sensorExpanded = it }
                ) {
                    OutlinedTextField(
                        value = sensorOptions.find { it.first == selectedSensor }?.second ?: "All Sensors",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sensor Source") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sensorExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = sensorExpanded,
                        onDismissRequest = { sensorExpanded = false }
                    ) {
                        sensorOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.second) },
                                onClick = {
                                    selectedSensor = if (option.first == "all") null else option.first
                                    sensorExpanded = false
                                }
                            )
                        }
                    }
                }

                // Strategy Dropdown Second
                ExposedDropdownMenuBox(
                    expanded = strategyExpanded,
                    onExpandedChange = { strategyExpanded = it }
                ) {
                    OutlinedTextField(
                        value = strategies.find { it.first == selectedStrategy }?.second ?: selectedStrategy,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Metric") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = strategyExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = strategyExpanded,
                        onDismissRequest = { strategyExpanded = false }
                    ) {
                        strategies.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.second) },
                                onClick = {
                                    selectedStrategy = option.first
                                    strategyExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedStrategy.isNotEmpty()) {
                        onAdd(title, selectedStrategy, selectedSensor)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBlockDialog(
    block: SingleChartBlock,
    viewModel: FlexibleAnalyticsViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit,
    onDelete: () -> Unit
) {
    val allSensors by viewModel.availableSensors.collectAsState()
    val sensorName =
        if (block.sensorId == null) "All Sensors" else allSensors.find { it.first == block.sensorId }?.second
            ?: block.sensorId

    val strategies = viewModel.strategiesForSensor(block.sensorId)
    var selectedStrategy by remember { mutableStateOf(block.dataType) }
    var strategyExpanded by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf(block.title) }

    // Color palette swatches
    val colorPalette = listOf(
        null to "Default",
        "#2196F3" to "Blue",
        "#E91E63" to "Pink",
        "#4CAF50" to "Green",
        "#FF9800" to "Orange",
        "#9C27B0" to "Purple",
        "#F44336" to "Red",
        "#00BCD4" to "Cyan"
    )
    var selectedColorHex by remember { mutableStateOf(block.colorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Block: ${block.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("Block Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sensorName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sensor Source (Fixed)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                )

                ExposedDropdownMenuBox(
                    expanded = strategyExpanded,
                    onExpandedChange = { strategyExpanded = it }
                ) {
                    OutlinedTextField(
                        value = strategies.find { it.first == selectedStrategy }?.second ?: selectedStrategy,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Metric") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = strategyExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = strategyExpanded,
                        onDismissRequest = { strategyExpanded = false }
                    ) {
                        strategies.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.second) },
                                onClick = {
                                    selectedStrategy = option.first
                                    strategyExpanded = false
                                }
                            )
                        }
                    }
                }

                // Color Picker
                Text(
                    "Line Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ColorPicker(
                    selectedColorHex = selectedColorHex,
                    onColorSelected = { selectedColorHex = it }
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onDelete(); onDismiss() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
                TextButton(onClick = { onSave(editedTitle, selectedStrategy, selectedColorHex) }) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCombinedBlockDialog(
    block: CombinedChartBlock,
    viewModel: FlexibleAnalyticsViewModel,
    onDismiss: () -> Unit
) {
    var editedTitle by remember { mutableStateOf(block.title) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Combination") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { 
                        editedTitle = it
                        viewModel.renameBlock(block.id, it)
                    },
                    label = { Text("Combined Block Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                items(block.childBlocks) { child ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.8f
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(child.title, style = MaterialTheme.typography.titleSmall)
                                IconButton(onClick = { viewModel.removeChildFromCombined(block.id, child.id) }) {
                                    Icon(
                                        Icons.Default.RemoveCircle,
                                        "Remove sensor",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            val strategies = viewModel.strategiesForSensor(child.sensorId)
                            var strategyExpanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = strategyExpanded,
                                onExpandedChange = { strategyExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = strategies.find { it.first == child.dataType }?.second ?: child.dataType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Metric") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = strategyExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = strategyExpanded,
                                    onDismissRequest = { strategyExpanded = false }
                                ) {
                                    strategies.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.second) },
                                            onClick = {
                                                viewModel.updateChildInCombined(
                                                    block.id,
                                                    child.id,
                                                    option.first,
                                                    child.colorHex
                                                )
                                                strategyExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Text("Color", style = MaterialTheme.typography.labelSmall)
                            ColorPicker(
                                selectedColorHex = child.colorHex,
                                onColorSelected = {
                                    viewModel.updateChildInCombined(
                                        block.id,
                                        child.id,
                                        child.dataType,
                                        it
                                    )
                                },
                                size = 24
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = { viewModel.deleteBlock(block.id); onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete Entire Combination")
                    }
                }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

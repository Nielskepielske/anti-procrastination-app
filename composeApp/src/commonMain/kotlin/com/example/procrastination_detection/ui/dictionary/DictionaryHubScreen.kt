package com.example.procrastination_detection.ui.dictionary

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.procrastination_detection.data.local.entity.InboxEntity
import com.example.procrastination_detection.data.local.entity.RuleEntity
import com.example.procrastination_detection.domain.model.Category
import com.example.procrastination_detection.domain.repository.RuleType
import com.example.procrastination_detection.domain.trigger.ActionTrigger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryHubScreen(viewModel: DictionaryViewModel) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val inboxItems by viewModel.inboxFlow.collectAsState()
    val savedRules by viewModel.savedRulesFlow.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (selectedTabIndex == 1) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Add, "Add Rule")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)) }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Inbox (${inboxItems.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Saved Rules", fontWeight = FontWeight.Bold) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (selectedTabIndex == 0) {
                    InboxContent(inboxItems, viewModel)
                } else {
                    SavedRulesContent(savedRules, viewModel)
                }
            }
        }

        if (showAddDialog) {
            AddRuleDialog(
                availableTriggers = viewModel.availableTriggers,
                onDismiss = { showAddDialog = false },
                onAdd = { condition, category, isExact, triggerId ->
                    viewModel.addManualRule(condition, category, isExact, triggerId)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun InboxContent(inboxItems: List<InboxEntity>, viewModel: DictionaryViewModel) {
    if (inboxItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Inbox is clear!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Newly detected apps and websites will appear here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 88.dp) // Scroll padding to clear FAB clipping
        ) {
            items(inboxItems, key = { it.id }) { item ->
                InboxCard(item = item, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun InboxCard(item: InboxEntity, viewModel: DictionaryViewModel) {
    val isDark = isSystemInDarkTheme()
    val suggestedColor = when (item.suggestedCategory) {
        Category.PRODUCTIVE  -> if (isDark) Color(0xFF34D399) else Color(0xFF10B981)
        Category.DISTRACTING -> MaterialTheme.colorScheme.error
        else                 -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = item.contextStr,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = suggestedColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.suggestedCategory.name,
                        color = suggestedColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                Text(
                    text = "via ${item.discoveredByStrategy}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.approveInboxItem(item, Category.PRODUCTIVE) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF34D399) else Color(0xFF10B981)
                    ),
                    modifier = Modifier.weight(1.0f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Productive", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { viewModel.approveInboxItem(item, Category.DISTRACTING) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1.0f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Distracting", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { viewModel.dismissInboxItem(item) },
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                ) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun SavedRulesContent(savedRules: List<RuleEntity>, viewModel: DictionaryViewModel) {
    var ruleToEdit by remember { mutableStateOf<RuleEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<Category?>(null) } // null = All

    val isDark = isSystemInDarkTheme()

    val filteredRules = remember(savedRules, searchQuery, selectedCategoryFilter) {
        savedRules.filter { rule ->
            val matchesSearch = rule.condition.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryFilter == null || rule.category == selectedCategoryFilter
            matchesSearch && matchesCategory
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- Search bar textfield ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            placeholder = { Text("Search rules...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        )

        // --- Category Quick Filters Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedCategoryFilter == null,
                onClick = { selectedCategoryFilter = null },
                label = { Text("All") },
                shape = RoundedCornerShape(8.dp)
            )
            FilterChip(
                selected = selectedCategoryFilter == Category.PRODUCTIVE,
                onClick = { selectedCategoryFilter = Category.PRODUCTIVE },
                label = { Text("🟢 Productive") },
                shape = RoundedCornerShape(8.dp)
            )
            FilterChip(
                selected = selectedCategoryFilter == Category.DISTRACTING,
                onClick = { selectedCategoryFilter = Category.DISTRACTING },
                label = { Text("🔴 Distracting") },
                shape = RoundedCornerShape(8.dp)
            )
            FilterChip(
                selected = selectedCategoryFilter == Category.AMBIGUOUS,
                onClick = { selectedCategoryFilter = Category.AMBIGUOUS },
                label = { Text("🟡 Ambiguous") },
                shape = RoundedCornerShape(8.dp)
            )
        }

        // --- List Content ---
        if (filteredRules.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1.0f), contentAlignment = Alignment.Center) {
                Text(
                    text = if (savedRules.isEmpty()) "No saved rules yet. Add one!" else "No matching rules found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 88.dp), // Clear bottom scroll for FAB
                modifier = Modifier.weight(1.0f)
            ) {
                items(filteredRules, key = { it.id }) { rule ->
                    val categoryColor = when (rule.category) {
                        Category.PRODUCTIVE  -> if (isDark) Color(0xFF34D399) else Color(0xFF10B981)
                        Category.DISTRACTING -> MaterialTheme.colorScheme.error
                        else                 -> MaterialTheme.colorScheme.tertiary
                    }

                    // Choose leading visual anchor icon depending on matching strategy
                    val strategyIcon = when (rule.ruleType) {
                        "PROCESS_CONTAINS" -> Icons.Default.Build
                        "BROWSER_PROCESS"  -> Icons.Default.Share
                        else               -> Icons.Default.List
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = if (isDark) 0.dp else 4.dp,
                                shape = RoundedCornerShape(16.dp),
                                clip = false
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Visual Anchor Icon + Content Info
                            Row(
                                modifier = Modifier.weight(1.0f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    color = categoryColor.copy(alpha = 0.1f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = strategyIcon,
                                            contentDescription = null,
                                            tint = categoryColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = rule.condition,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Pill shaped category badge
                                        Surface(
                                            color = categoryColor.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.15f))
                                        ) {
                                            Text(
                                                text = rule.category.name,
                                                color = categoryColor,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "· ${rule.ruleType.replace("_", " ").lowercase()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            // Action buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { ruleToEdit = rule }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteRule(rule) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    ruleToEdit?.let { rule ->
        EditRuleDialog(
            rule = rule,
            availableTriggers = viewModel.availableTriggers,
            onDismiss = { ruleToEdit = null },
            onSave = { condition, category, ruleType, triggerId ->
                viewModel.editRule(rule, condition, category, ruleType, triggerId)
                ruleToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleDialog(
    availableTriggers: List<ActionTrigger>,
    onDismiss: () -> Unit,
    onAdd: (String, Category, RuleType, String?) -> Unit
) {
    var condition by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.PRODUCTIVE) }
    var selectedRuleType by remember { mutableStateOf(RuleType.TITLE_CONTAINS) }
    var ruleTypeExpanded by remember { mutableStateOf(false) }
    var triggerExpanded by remember { mutableStateOf(false) }
    var selectedTriggerId by remember { mutableStateOf<String?>(null) }

    val categorySelectionEnabled = selectedRuleType != RuleType.BROWSER_PROCESS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = condition,
                    onValueChange = { condition = it },
                    label = { Text("Condition value") },
                    supportingText = { Text(selectedRuleType.description, style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Rule type selector - removed deprecated ExposedDropdownMenu API usages
                ExposedDropdownMenuBox(
                    expanded = ruleTypeExpanded,
                    onExpandedChange = { ruleTypeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedRuleType.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Match strategy") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(ruleTypeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = ruleTypeExpanded,
                        onDismissRequest = { ruleTypeExpanded = false }
                    ) {
                        RuleType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(type.label, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            type.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedRuleType = type
                                    ruleTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Category chips
                AnimatedVisibility(visible = categorySelectionEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedCategory == Category.PRODUCTIVE,
                            onClick = { selectedCategory = Category.PRODUCTIVE; selectedTriggerId = null },
                            label = { Text("Productive") }
                        )
                        FilterChip(
                            selected = selectedCategory == Category.DISTRACTING,
                            onClick = { selectedCategory = Category.DISTRACTING; selectedTriggerId = null },
                            label = { Text("Distracting") }
                        )
                        FilterChip(
                            selected = selectedCategory == Category.AMBIGUOUS,
                            onClick = { selectedCategory = Category.AMBIGUOUS; selectedTriggerId = null },
                            label = { Text("Ambiguous") }
                        )
                    }
                }

                val effectiveCategory = if (selectedRuleType == RuleType.BROWSER_PROCESS) Category.AMBIGUOUS else selectedCategory
                val compatibleTriggers = availableTriggers.filter { it.category == effectiveCategory }
                
                if (compatibleTriggers.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = triggerExpanded,
                        onExpandedChange = { triggerExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedTriggerId ?: "None",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Sensor Trigger (Optional)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(triggerExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = triggerExpanded,
                            onDismissRequest = { triggerExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    selectedTriggerId = null
                                    triggerExpanded = false
                                }
                            )
                            compatibleTriggers.forEach { trigger ->
                                DropdownMenuItem(
                                    text = { Text(trigger.id) },
                                    onClick = {
                                        selectedTriggerId = trigger.id
                                        triggerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val finalCategory = if (selectedRuleType == RuleType.BROWSER_PROCESS) Category.AMBIGUOUS else selectedCategory
            Button(
                onClick = { onAdd(condition, finalCategory, selectedRuleType, selectedTriggerId) },
                enabled = condition.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Save Rule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRuleDialog(
    rule: RuleEntity,
    availableTriggers: List<ActionTrigger>,
    onDismiss: () -> Unit,
    onSave: (String, Category, RuleType, String?) -> Unit
) {
    var condition by remember { mutableStateOf(rule.condition) }
    var selectedCategory by remember { mutableStateOf(rule.category) }
    val initialType = runCatching { RuleType.valueOf(rule.ruleType) }.getOrDefault(RuleType.TITLE_CONTAINS)
    var selectedRuleType by remember { mutableStateOf(initialType) }
    var ruleTypeExpanded by remember { mutableStateOf(false) }
    var triggerExpanded by remember { mutableStateOf(false) }
    var selectedTriggerId by remember { mutableStateOf(rule.triggerId) }

    val categorySelectionEnabled = selectedRuleType != RuleType.BROWSER_PROCESS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = condition,
                    onValueChange = { condition = it },
                    label = { Text("Condition value") },
                    supportingText = { Text(selectedRuleType.description, style = MaterialTheme.typography.labelSmall) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Refactored ExposedDropdownMenu API
                ExposedDropdownMenuBox(
                    expanded = ruleTypeExpanded,
                    onExpandedChange = { ruleTypeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedRuleType.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Match strategy") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(ruleTypeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = ruleTypeExpanded,
                        onDismissRequest = { ruleTypeExpanded = false }
                    ) {
                        RuleType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(type.label, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            type.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedRuleType = type
                                    ruleTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = categorySelectionEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Category.values().forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category; selectedTriggerId = null },
                                label = { Text(category.label, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                }

                val effectiveCategory = if (selectedRuleType == RuleType.BROWSER_PROCESS) Category.AMBIGUOUS else selectedCategory
                val compatibleTriggers = availableTriggers.filter { it.category == effectiveCategory }

                if (compatibleTriggers.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = triggerExpanded,
                        onExpandedChange = { triggerExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedTriggerId ?: "None",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Sensor Trigger (Optional)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(triggerExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = triggerExpanded,
                            onDismissRequest = { triggerExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    selectedTriggerId = null
                                    triggerExpanded = false
                                }
                            )
                            compatibleTriggers.forEach { trigger ->
                                DropdownMenuItem(
                                    text = { Text(trigger.id) },
                                    onClick = {
                                        selectedTriggerId = trigger.id
                                        triggerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val finalCategory = if (selectedRuleType == RuleType.BROWSER_PROCESS) Category.AMBIGUOUS else selectedCategory
            Button(
                onClick = { onSave(condition, finalCategory, selectedRuleType, selectedTriggerId) },
                enabled = condition.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Update Rule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
package com.example.procrastination_detection.ui.dictionary

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.procrastination_detection.data.local.InboxEntity
import com.example.procrastination_detection.data.local.RuleEntity
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Filled.Add, "Add Rule")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Inbox (${inboxItems.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Saved Rules", fontWeight = FontWeight.SemiBold) }
                )
            }

            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                Text("Inbox is clear!", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Newly detected apps and websites will appear here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(inboxItems, key = { it.id }) { item ->
                InboxCard(item = item, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun InboxCard(item: InboxEntity, viewModel: DictionaryViewModel) {
    val suggestedColor = when (item.suggestedCategory) {
        Category.PRODUCTIVE -> MaterialTheme.colorScheme.primary
        Category.DISTRACTING -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }

    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(item.contextStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(suggestedColor))
                Text(
                    "Suggested: ${item.suggestedCategory} (via ${item.discoveredByStrategy})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.approveInboxItem(item, Category.PRODUCTIVE) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) { Text("Productive") }
                Button(
                    onClick = { viewModel.approveInboxItem(item, Category.DISTRACTING) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) { Text("Distracting") }
                OutlinedButton(
                    onClick = { viewModel.dismissInboxItem(item) },
                    modifier = Modifier.weight(1f)
                ) { Text("Dismiss") }
            }
        }
    }
}

@Composable
fun SavedRulesContent(savedRules: List<RuleEntity>, viewModel: DictionaryViewModel) {
    var ruleToEdit by remember { mutableStateOf<RuleEntity?>(null) }

    if (savedRules.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No saved rules yet. Add one!", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(savedRules, key = { it.id }) { rule ->
                val categoryColor = when (rule.category) {
                    Category.PRODUCTIVE -> MaterialTheme.colorScheme.primary
                    Category.DISTRACTING -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.secondary
                }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.condition, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(categoryColor))
                                Text(
                                    "${rule.category} · ${rule.ruleType.replace("_", " ").lowercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = { ruleToEdit = rule }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.deleteRule(rule) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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

    // BROWSER_PROCESS always has category AMBIGUOUS — the OCR engine determines the real category
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

                // Rule type selector
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
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                                        Text(type.description, style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                // Category chips — hidden for BROWSER_PROCESS since category isn't relevant there
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
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                enabled = condition.isNotBlank()
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
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                                        Text(type.description, style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                enabled = condition.isNotBlank()
            ) { Text("Update Rule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
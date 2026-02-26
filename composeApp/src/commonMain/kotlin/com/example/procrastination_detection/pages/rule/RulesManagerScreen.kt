package com.example.procrastination_detection.pages.rule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.procrastination_detection.ui.components.CategorySearchDialog

@Composable
fun RulesManagerScreen(viewModel: RulesViewModel) {
    // We only need to collect the rules for the list view
    val rules by viewModel.rules.collectAsState()
    val filteredCategories by viewModel.filteredCategories.collectAsState()

    if (viewModel.isCreatingRule) {
        // --- MODE: CREATE A NEW RULE ---
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Create New Rule", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = viewModel.draftRuleName,
                onValueChange = viewModel::updateRuleName,
                label = { Text("Rule Name (e.g., Deep Work)") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            // --- THE ADD CATEGORY BUTTON ROW ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, // Pushes text left, button right
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Configure Categories:", style = MaterialTheme.typography.titleMedium)

                Button(onClick = { viewModel.openCategoryDialog() }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Category", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Category")
                }
            }

            // Scrollable list of category configurations
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(viewModel.draftLines) { draft ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {

                            // Top Row: Category Name + Productive Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(draft.category.name, style = MaterialTheme.typography.titleMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (draft.isProductive) "Productive" else "Tracked")
                                    Switch(
                                        checked = draft.isProductive,
                                        onCheckedChange = { isProd ->
                                            viewModel.updateDraftLine(
                                                draft.category, isProd, draft.maxConsecutiveMins, draft.maxTotalMins
                                            )
                                        },
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }

                            // Bottom Row: Time Limits (Only visible if Unproductive!)
                            if (!draft.isProductive) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = draft.maxConsecutiveMins,
                                        onValueChange = { newValue ->
                                            viewModel.updateDraftLine(draft.category, draft.isProductive, newValue, draft.maxTotalMins)
                                        },
                                        label = { Text("Max Consec (min)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = draft.maxTotalMins,
                                        onValueChange = { newValue ->
                                            viewModel.updateDraftLine(draft.category, draft.isProductive, draft.maxConsecutiveMins, newValue)
                                        },
                                        label = { Text("Max Total (min)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { viewModel.cancelCreation() }) { Text("Cancel") }
                Button(onClick = { viewModel.saveRule() }) { Text("Save Rule") }
            }

            // --- RENDER THE DIALOG IF OPEN ---
            if (viewModel.showCategoryDialog) {
                CategorySearchDialog(
                    searchQuery = viewModel.categorySearchQuery,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    filteredCategories = filteredCategories,
                    onCategorySelected = viewModel::addCategoryLine,
                    onCreateNewCategory = viewModel::createAndAddCategory,
                    onDismiss = viewModel::closeCategoryDialog
                )
            }
        }
    } else {
        // --- MODE: VIEW EXISTING RULES ---
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { viewModel.startCreatingRule() }) {
                    Icon(Icons.Default.Add, contentDescription = "Create New Rule")
                }
            }
        ) { paddingValues ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
                item {
                    Text("Your Rules", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                }
                items(rules) { rule ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rule.name, style = MaterialTheme.typography.titleMedium)
                                Text("${rule.lines.size} category limits configured", style = MaterialTheme.typography.bodySmall)
                            }

                            // Edit and Delete Buttons
                            Row {
                                IconButton(onClick = { viewModel.startEditingRule(rule) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Rule")
                                }
                                IconButton(onClick = { viewModel.deleteRule(rule) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Rule", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
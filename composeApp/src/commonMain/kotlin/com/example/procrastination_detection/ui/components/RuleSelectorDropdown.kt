package com.example.procrastination_detection.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import com.example.procrastination_detection.pages.home.AppListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleSelectorDropdown(viewModel: AppListViewModel) {
    val availableRules by viewModel.availableRules.collectAsState()
    val selectedRule = viewModel.selectedRule

    // Controls whether the dropdown menu is open or closed
    var expanded by remember { mutableStateOf(false) }

    // Only show the dropdown if the user has actually created rules
    if (availableRules.isNotEmpty()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            // The visible text field that users click
            OutlinedTextField(
                value = selectedRule?.name ?: "Select a Rule",
                onValueChange = {}, // Read-only
                readOnly = true,
                label = { Text("Active Rule") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor() // Binds the menu to this text field
            )

            // The popup list of rules
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableRules.forEach { rule ->
                    DropdownMenuItem(
                        text = { Text(rule.name) },
                        onClick = {
                            viewModel.onRuleSelected(rule)
                            expanded = false
                        }
                    )
                }
            }
        }
    } else {
        // Fallback if no rules exist yet
        Text(
            text = "Go to the Rules tab to create your first rule!",
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.error
        )
    }
}
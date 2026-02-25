package com.example.procrastination_detection.pages.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.procrastination_detection.models.db.Category
import com.example.procrastination_detection.models.db.Process

@Composable
fun AppLibraryScreen(viewModel: AppLibraryViewModel) {
    val apps by viewModel.apps.collectAsState()
    val categories by viewModel.categories.collectAsState()

    // State to track which app we are currently editing (if any)
    var appToEdit by remember { mutableStateOf<Process?>(null) }

    Scaffold { padding ->
        LazyColumn(contentPadding = padding) {
            items(apps, key = { it.id }) { app ->
                ListItem(
                    headlineContent = { Text(app.name) },
                    supportingContent = { Text("Category: ${app.category.name}") },
                    modifier = Modifier.clickable { appToEdit = app }
                )
                HorizontalDivider()
            }
        }
    }

    // The Category Picker Dialog
    if (appToEdit != null) {
        AlertDialog(
            onDismissRequest = { appToEdit = null },
            title = { Text("Categorize ${appToEdit?.name}") },
            text = {
                Column {
                    categories.forEach { category ->
                        TextButton(
                            onClick = {
                                viewModel.assignCategoryToApp(appToEdit!!, category)
                                appToEdit = null // Close dialog
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(category.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { appToEdit = null }) { Text("Cancel") }
            }
        )
    }
}


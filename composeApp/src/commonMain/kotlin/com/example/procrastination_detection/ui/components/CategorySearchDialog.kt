package com.example.procrastination_detection.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.procrastination_detection.models.db.Category

@Composable
fun CategorySearchDialog(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filteredCategories: List<Category>,
    onCategorySelected: (Category) -> Unit,
    onCreateNewCategory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Category Limit") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Search Categories") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    // Show existing matches
                    items(filteredCategories) { category ->
                        TextButton(
                            onClick = { onCategorySelected(category) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(category.name)
                        }
                    }

                    // If no exact match exists, show the "Create" button at the bottom
                    val exactMatchExists = filteredCategories.any { it.name.equals(searchQuery, ignoreCase = true) }
                    if (searchQuery.isNotBlank() && !exactMatchExists) {
                        item {
                            Button(
                                onClick = { onCreateNewCategory(searchQuery) },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Text("Create new: '$searchQuery'")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = "Close") }
        }
    )
}
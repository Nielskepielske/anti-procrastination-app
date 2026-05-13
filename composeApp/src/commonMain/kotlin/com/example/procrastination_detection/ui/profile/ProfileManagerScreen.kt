package com.example.procrastination_detection.ui.profile

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.procrastination_detection.domain.model.EscalationLevel
import com.example.procrastination_detection.domain.model.FocusProfile

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ProfileManagerScreen(viewModel: ProfileViewModel) {
    val allProfiles by viewModel.allProfiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    
    var showEditDialog by remember { mutableStateOf<FocusProfile?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Focus Profiles",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Button(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New Profile")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Select an active profile to determine how the app tracks and intervenes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(allProfiles) { profile ->
                ProfileCard(
                    profile = profile,
                    isActive = profile.id == activeProfile?.id,
                    onActivate = { viewModel.setActiveProfile(profile.id) },
                    onEdit = { showEditDialog = profile },
                    onDelete = { viewModel.deleteProfile(profile.id) }
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateProfileDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name -> 
                viewModel.createProfile(name)
                showCreateDialog = false
            }
        )
    }

    showEditDialog?.let { profile ->
        EditProfileDialog(
            profile = profile,
            availableSensors = viewModel.availableSensors.map { it.id },
            availableStrategies = viewModel.availableStrategies,
            onDismiss = { showEditDialog = null },
            onSave = { updated ->
                viewModel.saveProfile(updated)
                showEditDialog = null
            }
        )
    }
}

@Composable
fun ProfileCard(
    profile: FocusProfile,
    isActive: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val surfaceColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onActivate() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isActive) Icons.Default.CheckCircle else Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${profile.thresholdMinutes} min threshold • ${profile.escalationLevel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun CreateProfileDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Profile") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Profile Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onCreate(name) }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditProfileDialog(
    profile: FocusProfile,
    availableSensors: List<String>,
    availableStrategies: List<String>,
    onDismiss: () -> Unit,
    onSave: (FocusProfile) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var threshold by remember { mutableStateOf(profile.thresholdMinutes.toFloat()) }
    var escalationLevel by remember { mutableStateOf(profile.escalationLevel) }
    var selectedSensors by remember { mutableStateOf(profile.requiredSensorIds.toSet()) }
    var strategyMap by remember { mutableStateOf(profile.strategyMap) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.85f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text("Edit Profile: ${profile.name}") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Text("General Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Profile Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Procrastination Threshold: ${threshold.toInt()} minutes")
                    Slider(
                        value = threshold,
                        onValueChange = { threshold = it },
                        valueRange = 1f..60f,
                        steps = 59
                    )
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                }

                item {
                    Text("Default Escalation Level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        EscalationLevel.values().forEach { level ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = escalationLevel == level, onClick = { escalationLevel = level })
                                Text(level.name, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                }

                item {
                    Text("Intervention Matrix", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Select which strategies fire at each level", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }

                items(EscalationLevel.values()) { level ->
                    var expanded by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(level.name, fontWeight = FontWeight.Medium)
                                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                            }
                            if (expanded) {
                                Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                    availableStrategies.forEach { strategyId ->
                                        val isChecked = strategyMap[level]?.contains(strategyId) == true
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    val current = strategyMap[level]?.toMutableList() ?: mutableListOf()
                                                    if (checked) current.add(strategyId) else current.remove(strategyId)
                                                    strategyMap = strategyMap + (level to current)
                                                }
                                            )
                                            Text(strategyId, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                    Text("Active Sensors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    availableSensors.forEach { sensorId ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = selectedSensors.contains(sensorId),
                                onCheckedChange = { checked ->
                                    selectedSensors = if (checked) selectedSensors + sensorId else selectedSensors - sensorId
                                }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(sensorId)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(profile.copy(
                    name = name,
                    thresholdMinutes = threshold.toInt(),
                    escalationLevel = escalationLevel,
                    strategyMap = strategyMap,
                    requiredSensorIds = selectedSensors.toList()
                ))
            }) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
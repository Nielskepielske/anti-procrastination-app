package com.example.procrastination_detection.ui.profile

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.procrastination_detection.domain.model.EscalationLevel
import com.example.procrastination_detection.domain.model.FocusProfile
import com.example.procrastination_detection.domain.model.CsvGranularity

/**
 * Converts system snake_case names like "LINUX_NUDGE" or "WINDOW_TRACKER"
 * into friendly capitalized titles like "Linux Nudge" or "Window Tracker".
 */
fun formatSystemName(name: String): String {
    return name.replace("_", " ")
        .lowercase()
        .split(" ")
        .joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
}

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
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        // --- 1. Header Section ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Focus Profiles",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Button(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("New Profile", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = "Select an active profile to determine how the app tracks and intervenes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        // --- 2. Focus Profiles List ---
        if (allProfiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No focus profiles found. Click 'New Profile' to add one.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp), // Clear bottom space for accessibility
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
            availableSensors = viewModel.availableSensors.map { it.type.name },
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
    val isDark = isSystemInDarkTheme()
    
    // Choose beautiful elevated card colors
    val cardBackground = if (isActive) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        if (isDark) MaterialTheme.colorScheme.surface else Color.White
    }

    val borderStroke = if (isActive) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isActive) (if (isDark) 0.dp else 8.dp) else (if (isDark) 0.dp else 4.dp),
                shape = RoundedCornerShape(20.dp),
                clip = false
            )
            .clickable { onActivate() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = borderStroke
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant gradient graphic icon representation
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary, 
                                MaterialTheme.colorScheme.tertiary
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${profile.thresholdMinutes} min threshold • ${formatSystemName(profile.escalationLevel.name)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!isActive) {
                    TextButton(onClick = onActivate) {
                        Text("Set Active", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = onDelete) {
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

@Composable
fun CreateProfileDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Create", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var csvGranularity by remember { mutableStateOf(profile.csvGranularity) }

    val isDark = isSystemInDarkTheme()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.9f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { 
            Text(
                text = "Edit Profile: ${profile.name}", 
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            ) 
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // Section 1: General Settings
                item {
                    Text(
                        text = "General Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Profile Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = "Procrastination Threshold: ${threshold.toInt()} minutes",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Slider(
                        value = threshold,
                        onValueChange = { threshold = it },
                        valueRange = 1f..60f,
                        steps = 59
                    )
                }

                // Section 2: Default Escalation Level using modern chips instead of Radio Buttons
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Default Escalation Level",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EscalationLevel.values().forEach { level ->
                            FilterChip(
                                selected = escalationLevel == level,
                                onClick = { escalationLevel = level },
                                label = { Text(formatSystemName(level.name)) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.0f)
                            )
                        }
                    }
                }

                // Section 2.5: CSV Data Granularity
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Data Retention Granularity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "How much sensor data to keep when a session finishes. High granularity takes more space.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CsvGranularity.values().forEach { granularity ->
                            FilterChip(
                                selected = csvGranularity == granularity,
                                onClick = { csvGranularity = granularity },
                                label = { Text(formatSystemName(granularity.name)) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.0f)
                            )
                        }
                    }
                }

                // Section 3: Intervention Matrix collapsible tiles
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Intervention Matrix",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Select which strategies trigger at each severity level.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(EscalationLevel.values()) { level ->
                    var expanded by remember { mutableStateOf(false) }
                    
                    val cardBg = if (expanded) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    } else {
                        if (isDark) MaterialTheme.colorScheme.surface else Color.White
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = if (expanded) 0.dp else 2.dp,
                                shape = RoundedCornerShape(12.dp),
                                clip = false
                            ),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = !expanded }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val icon = when (level) {
                                        EscalationLevel.GENTLE    -> Icons.Default.Notifications
                                        EscalationLevel.FIRM      -> Icons.Default.Warning
                                        EscalationLevel.AGGRESSIVE -> Icons.Default.Lock
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = formatSystemName(level.name),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            if (expanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    availableStrategies.forEach { strategyId ->
                                        val isChecked = strategyMap[level]?.contains(strategyId) == true
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val current = strategyMap[level]?.toMutableList() ?: mutableListOf()
                                                    if (!isChecked) current.add(strategyId) else current.remove(strategyId)
                                                    strategyMap = strategyMap + (level to current)
                                                }
                                                .padding(vertical = 4.dp)
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    val current = strategyMap[level]?.toMutableList() ?: mutableListOf()
                                                    if (checked) current.add(strategyId) else current.remove(strategyId)
                                                    strategyMap = strategyMap + (level to current)
                                                }
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = formatSystemName(strategyId),
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: Active Sensors
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Active Sensors",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableSensors.forEach { sensorId ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val isEnabled = selectedSensors.contains(sensorId)
                                        selectedSensors = if (!isEnabled) selectedSensors + sensorId else selectedSensors - sensorId
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Switch(
                                    checked = selectedSensors.contains(sensorId),
                                    onCheckedChange = { checked ->
                                        selectedSensors = if (checked) selectedSensors + sensorId else selectedSensors - sensorId
                                    }
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = formatSystemName(sensorId),
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(profile.copy(
                        name = name,
                        thresholdMinutes = threshold.toInt(),
                        escalationLevel = escalationLevel,
                        strategyMap = strategyMap,
                        requiredSensorIds = selectedSensors.toList(),
                        csvGranularity = csvGranularity
                    ))
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
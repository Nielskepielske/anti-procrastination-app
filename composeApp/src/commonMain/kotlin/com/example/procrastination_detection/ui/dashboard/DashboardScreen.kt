package com.example.procrastination_detection.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.procrastination_detection.domain.model.Category
import com.example.procrastination_detection.domain.sensor.SensorType
import com.example.procrastination_detection.domain.model.EscalationLevel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val currentApp by viewModel.currentAppFlow.collectAsState("Waiting for activity...")
    val currentCategory by viewModel.currentCategoryFlow.collectAsState(Category.UNCATEGORIZED)
    val isTracking by viewModel.isTrackingFlow.collectAsState(false)
    val activeProfileName by viewModel.activeProfileNameFlow.collectAsState("No Profile")
    val activeSensors by viewModel.activeSensorsFlow.collectAsState(emptyList())
    val telemetry by viewModel.liveTelemetryFlow.collectAsState()
    val orphanedSession by viewModel.orphanedSessionFlow.collectAsState()
    val activeSession by viewModel.activeSessionFlow.collectAsState(null)
    val aggressionHeat by viewModel.aggressionHeatFlow.collectAsState(0)
    val escalationLevel by viewModel.escalationLevelFlow.collectAsState(EscalationLevel.GENTLE)
    var showStopConfirmation by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val categoryColor = when (currentCategory) {
        Category.DISTRACTING   -> MaterialTheme.colorScheme.error
        Category.PRODUCTIVE    -> if (isDark) Color(0xFF34D399) else Color(0xFF10B981) // Dedicated Emerald Success Greens
        Category.AMBIGUOUS     -> MaterialTheme.colorScheme.tertiary
        else                   -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 560.dp)
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        // --- 1. Brand & Active Profile Header ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Anti-procrastination-blueprint",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Active Profile: $activeProfileName",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Dynamic Active Sensors Badges
            if (isTracking && activeSensors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    activeSensors.forEach { sensorType ->
                        val sensorIcon = when (sensorType) {
                            SensorType.WINDOW_TRACKER -> Icons.Default.Home
                            SensorType.MOUSE_TRACKER -> Icons.Default.Info
                            SensorType.BROWSER_ANALYSER_SENSOR -> Icons.Default.Search
                            else -> Icons.Default.Help
                        }
                        val sensorLabel = when (sensorType) {
                            SensorType.WINDOW_TRACKER -> "Window Tracker"
                            SensorType.MOUSE_TRACKER -> "Mouse Tracker"
                            SensorType.BROWSER_ANALYSER_SENSOR -> "Browser OCR"
                            else -> sensorType.name
                        }
                        
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Emerald pulsing indicator
                                val infiniteTransition = rememberInfiniteTransition(label = "pulseSensor")
                                val sensorAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 1.0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "sensorAlpha"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF34D399))
                                        .alpha(sensorAlpha)
                                )
                                Icon(
                                    imageVector = sensorIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = sensorLabel,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. The Glassmorphic Live Data Console ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDark) 0.dp else 12.dp, 
                    shape = RoundedCornerShape(24.dp),
                    clip = false
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.08f else 0.05f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header of the card showing Status Title and Pulse Dot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CURRENT FOCUS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Breathing Live indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val pulseDotColor = if (isTracking) {
                            if (isDark) Color(0xFF34D399) else Color(0xFF10B981)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        }

                        if (isTracking) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.2f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulseAlpha"
                            )

                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(pulseDotColor)
                                    .alpha(pulseAlpha)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(pulseDotColor)
                            )
                        }

                        Text(
                            text = if (isTracking) "LIVE" else "PAUSED",
                            color = pulseDotColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Elegant app display widget
                Surface(
                    color = categoryColor.copy(alpha = 0.08f),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val mainIcon = when (currentCategory) {
                            Category.PRODUCTIVE    -> Icons.Default.CheckCircle
                            Category.DISTRACTING   -> Icons.Default.Warning
                            Category.AMBIGUOUS     -> Icons.Default.Info
                            else                   -> Icons.Default.Refresh
                        }
                        Icon(
                            imageVector = if (isTracking) mainIcon else Icons.Default.Refresh,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (isTracking) currentApp else "Focus Tracking Paused",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isTracking) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Semantic Pill Badge
                Surface(
                    color = categoryColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val badgeIcon = when (currentCategory) {
                            Category.PRODUCTIVE    -> Icons.Default.CheckCircle
                            Category.DISTRACTING   -> Icons.Default.Warning
                            Category.AMBIGUOUS     -> Icons.Default.Info
                            else                   -> Icons.Default.Help
                        }
                        Icon(
                            imageVector = if (isTracking) badgeIcon else Icons.Default.Notifications,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isTracking) currentCategory.name else "STANDBY",
                            color = categoryColor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- 2.5. Aggression Heat Level Visualizer ---
        if (isTracking) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (isDark) 0.dp else 8.dp, 
                        shape = RoundedCornerShape(20.dp),
                        clip = false
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.9f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.08f else 0.05f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INTERVENTION AGGRESSION HEAT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Surface(
                            color = when {
                                aggressionHeat == 0 -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                aggressionHeat < 3 -> Color(0xFF14B8A6).copy(alpha = 0.15f) // Teal
                                aggressionHeat < 6 -> Color(0xFFF59E0B).copy(alpha = 0.15f) // Amber
                                else -> Color(0xFFF43F5E).copy(alpha = 0.15f) // Rose
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Heat Score: $aggressionHeat",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    aggressionHeat == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                                    aggressionHeat < 3 -> Color(0xFF14B8A6)
                                    aggressionHeat < 6 -> Color(0xFFF59E0B)
                                    else -> Color(0xFFF43F5E)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Segmented Visualizer Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isGentleActive = aggressionHeat > 0
                        val isFirmActive = aggressionHeat >= 3
                        val isAggressiveActive = aggressionHeat >= 6

                        // Gentle Segment (Teal)
                        AggressionSegment(
                            label = "GENTLE",
                            isActive = isGentleActive,
                            color = Color(0xFF14B8A6),
                            isHighest = isGentleActive && !isFirmActive,
                            modifier = Modifier.weight(1f)
                        )

                        // Firm Segment (Amber)
                        AggressionSegment(
                            label = "FIRM",
                            isActive = isFirmActive,
                            color = Color(0xFFF59E0B),
                            isHighest = isFirmActive && !isAggressiveActive,
                            modifier = Modifier.weight(1f)
                        )

                        // Aggressive Segment (Rose)
                        AggressionSegment(
                            label = "AGGRESSIVE",
                            isActive = isAggressiveActive,
                            color = Color(0xFFF43F5E),
                            isHighest = isAggressiveActive,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Helpful Hint / Tips about active strategies
                    val (statusLabel, statusDesc, statusColor) = when {
                        aggressionHeat == 0 -> Triple(
                            "Calm & Productive", 
                            "System is in standby mode. Keep up the good focus!", 
                            MaterialTheme.colorScheme.primary
                        )
                        aggressionHeat < 3 -> Triple(
                            "Gentle Interventions Active", 
                            "Subtle desktop nudges are active to gently keep you on track.", 
                            Color(0xFF14B8A6)
                        )
                        aggressionHeat < 6 -> Triple(
                            "Firm Interventions Active", 
                            "Window opacity starts fading. Bring focus back to proceed normally.", 
                            Color(0xFFF59E0B)
                        )
                        else -> Triple(
                            "CRITICAL ESCALATION ACTIVE", 
                            "App killer is armed! Save work and close distracting activities immediately!", 
                            Color(0xFFF43F5E)
                        )
                    }

                    Surface(
                        color = statusColor.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    aggressionHeat == 0 -> Icons.Default.CheckCircle
                                    aggressionHeat < 3 -> Icons.Default.Notifications
                                    aggressionHeat < 6 -> Icons.Default.Warning
                                    else -> Icons.Default.Lock
                                },
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = statusLabel,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = statusColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = statusDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. The Live Telemetry Card (Active Session Stats) ---
        if (isTracking) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (isDark) 0.dp else 8.dp, 
                        shape = RoundedCornerShape(20.dp),
                        clip = false
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.9f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.08f else 0.05f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "LIVE TELEMETRY CONSOLE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Double column display for metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column: Mouse state
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "MOUSE STATE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            val mouseDistStr = "${(telemetry.mouseDistanceTraveled * 10).toInt() / 10.0} px"
                            TelemetryItem(
                                label = "Distance Traveled",
                                value = mouseDistStr
                            )
                            TelemetryItem(
                                label = "Current State",
                                value = if (telemetry.isMouseIdle) "Hovering / Idle" else "Active / Moving"
                            )
                        }

                        // Right Column: Pipeline stats
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "PIPELINE MONITOR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TelemetryItem(
                                label = "Events Registered",
                                value = "${telemetry.totalEventsProcessed} payloads"
                            )
                            TelemetryItem(
                                label = "Dynamic Scans",
                                value = if (activeSensors.contains(SensorType.BROWSER_ANALYSER_SENSOR)) "OCR Scanning" else "Standby"
                            )
                        }
                    }

                    if (telemetry.currentUrl.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "DEEP SCAN ACTIVE DOMAIN",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                Text(
                                    text = telemetry.currentUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Tactile Master Control Button ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (activeSession == null) {
                Button(
                    onClick = { viewModel.startTracking() },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(56.dp)
                        .shadow(
                            elevation = if (isDark) 0.dp else 6.dp,
                            shape = RoundedCornerShape(28.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Tracking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val isPaused = activeSession?.status == "PAUSED"
                    Button(
                        onClick = {
                            if (isPaused) viewModel.resumeTracking() else viewModel.pauseTracking()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .shadow(
                                elevation = if (isDark) 0.dp else 6.dp,
                                shape = RoundedCornerShape(28.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPaused) "Resume" else "Pause",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = { showStopConfirmation = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .shadow(
                                elevation = if (isDark) 0.dp else 6.dp,
                                shape = RoundedCornerShape(28.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Stop",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = if (isTracking) "Sensor pipelines actively recording desktop context." else "Click above to launch tracking sensors.",
                style = Modifier.alpha(0.7f).let { MaterialTheme.typography.bodySmall },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
    
    // Orphaned Session Modal Overlay
    if (orphanedSession != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                // Disable background interactions
                .pointerInput(Unit) {},
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .widthIn(max = 400.dp)
                    .padding(32.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Unfinished Session Found",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "It looks like the app was closed unexpectedly while a tracking session was active. What would you like to do with this session?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.continueOrphanedSession() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Continue Session")
                    }
                    OutlinedButton(
                        onClick = { viewModel.finishOrphanedSession() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text("Finish & Export Session")
                    }
                }
            }
        }
    }

    // Stop Tracking Confirmation Modal
    if (showStopConfirmation) {
        AlertDialog(
            onDismissRequest = { showStopConfirmation = false },
            title = {
                Text("Stop Tracking?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to stop tracking? This saves all the data to a CSV and discontinues the current session.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.stopTracking()
                        showStopConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Stop Session")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
}

@Composable
private fun TelemetryItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AggressionSegment(
    label: String,
    isActive: Boolean,
    color: Color,
    isHighest: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.12f,
        animationSpec = tween(500)
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulseHighest")
    val pulseAlpha by if (isHighest) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                color = if (isActive) color.copy(alpha = animatedAlpha * pulseAlpha) else Color.Gray.copy(alpha = 0.08f)
            )
            .border(
                width = 1.dp,
                color = if (isActive) color.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}
package com.kalendar.app.ui.eventview

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kalendar.app.data.local.entity.ReminderTime
import com.kalendar.app.data.local.entity.RepeatRule
import com.kalendar.app.ui.theme.BrandBlue
import com.kalendar.app.util.DateUtils

/**
 * Event Detail Screen with full details, guests, event type, and share action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: Long,
    viewModel: EventViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit
) {
    val uiState by viewModel.eventDetailState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        viewModel.loadEventDetail(eventId)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Event?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEvent(eventId) { onDeleted() }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Share Event
                    IconButton(onClick = {
                        val event = uiState.event ?: return@IconButton
                        val shareText = buildString {
                            appendLine("📅 ${event.title}")
                            appendLine("⏰ ${DateUtils.formatDate(DateUtils.toLocalDate(event.startTime))}")
                            if (!event.isAllDay) {
                                appendLine("⏱️ ${DateUtils.getTimeRangeText(event.startTime, event.endTime)}")
                            }
                            if (event.location.isNotBlank()) {
                                appendLine("📍 ${event.location}")
                            }
                            if (event.description.isNotBlank()) {
                                appendLine("📝 ${event.description}")
                            }
                            appendLine("\nShared via Kalendar")
                        }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Event"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }

                    // Edit
                    IconButton(onClick = { uiState.event?.let { onEdit(it.id) } }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }

                    // Delete
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        val event = uiState.event
        val calendar = uiState.calendar

        if (event != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                // Event Type Badge if set
                if (event.eventType.isNotBlank() && event.eventType != "Event") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BrandBlue.copy(alpha = 0.12f),
                        contentColor = BrandBlue,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = when (event.eventType) {
                                "Birthday" -> "🎂 Birthday"
                                "Meeting" -> "💼 Meeting"
                                "Holiday" -> "🏖️ Holiday"
                                "Task" -> "📋 Task"
                                "Anniversary" -> "🎉 Anniversary"
                                else -> event.eventType
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Title
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Date + Time
                Text(
                    text = DateUtils.getDayOfWeekName(DateUtils.toLocalDate(event.startTime)) +
                            " · " + DateUtils.formatDate(DateUtils.toLocalDate(event.startTime)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!event.isAllDay) {
                    Text(
                        text = DateUtils.getTimeRangeText(event.startTime, event.endTime),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "All day",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                // Calendar
                if (calendar != null) {
                    DetailRow(
                        icon = Icons.Filled.Circle,
                        iconTint = Color(calendar.color),
                        label = "Calendar",
                        value = calendar.name
                    )
                }

                // Guests
                if (event.guests.isNotBlank()) {
                    DetailRow(
                        icon = Icons.Filled.People,
                        label = "Guests",
                        value = event.guests
                    )
                }

                // Time Zone
                if (event.timeZone.isNotBlank()) {
                    DetailRow(
                        icon = Icons.Filled.Language,
                        label = "Time Zone",
                        value = event.timeZone
                    )
                }

                // Location
                if (event.location.isNotBlank()) {
                    DetailRow(
                        icon = Icons.Filled.LocationOn,
                        label = "Location",
                        value = event.location
                    )
                }

                // Notes
                if (event.description.isNotBlank()) {
                    DetailRow(
                        icon = Icons.Filled.Description,
                        label = "Notes",
                        value = event.description
                    )
                }

                // Repeat
                DetailRow(
                    icon = Icons.Filled.Repeat,
                    label = "Repeat",
                    value = when (event.repeatRule) {
                        RepeatRule.NONE -> "Doesn't repeat"
                        RepeatRule.DAILY -> "Daily"
                        RepeatRule.WEEKLY -> "Weekly"
                        RepeatRule.MONTHLY -> "Monthly"
                        RepeatRule.YEARLY -> "Yearly"
                    }
                )

                // Reminder
                DetailRow(
                    icon = Icons.Filled.Notifications,
                    label = "Reminder",
                    value = when (event.reminder) {
                        ReminderTime.NONE -> "No reminder"
                        ReminderTime.MINUTES_5 -> "5 minutes before"
                        ReminderTime.MINUTES_15 -> "15 minutes before"
                        ReminderTime.MINUTES_30 -> "30 minutes before"
                        ReminderTime.HOURS_1 -> "1 hour before"
                        ReminderTime.DAYS_1 -> "1 day before"
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        } else if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

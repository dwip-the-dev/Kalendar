package com.kalendar.app.ui.eventview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kalendar.app.ui.components.EventCard
import com.kalendar.app.ui.theme.BrandBlue
import com.kalendar.app.util.DateUtils
import java.time.LocalDate
import java.time.ZoneId

/**
 * Events List View — Tab 3
 * Displays upcoming events for today and into the future.
 */
@Composable
fun EventsListScreen(
    viewModel: EventViewModel,
    onEventClick: (Long) -> Unit,
    onCreateEvent: () -> Unit
) {
    val uiState by viewModel.eventsListState.collectAsStateWithLifecycle()

    val today = remember { LocalDate.now() }
    val todayStartMillis = remember(today) {
        today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    // Filter to only today and future events
    val upcomingEvents = remember(uiState.events, todayStartMillis) {
        uiState.events.filter { event ->
            event.endTime >= todayStartMillis || event.startTime >= todayStartMillis
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateEvent,
                containerColor = BrandBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create event")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Screen Header
            Text(
                text = "Events",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 32.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            if (upcomingEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No upcoming events",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap + to create a new event",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                // Group events by date
                val groupedEvents = remember(upcomingEvents) {
                    upcomingEvents.groupBy { event ->
                        DateUtils.toLocalDate(event.startTime)
                    }.toSortedMap()
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    groupedEvents.forEach { (date, events) ->
                        item(key = "header_$date") {
                            val isToday = date == today
                            Text(
                                text = if (isToday) "TODAY" else DateUtils.formatDate(date).uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = if (isToday) BrandBlue
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(
                                    start = 4.dp,
                                    top = 16.dp,
                                    bottom = 4.dp
                                )
                            )
                        }

                        items(events, key = { it.id }) { event ->
                            val calendarColor = uiState.calendars[event.calendarId]?.let {
                                Color(it.color)
                            } ?: BrandBlue

                            EventCard(
                                event = event,
                                calendarColor = calendarColor,
                                onClick = { onEventClick(event.id) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

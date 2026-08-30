package com.kalendar.app.ui.dayview

import android.app.DatePickerDialog
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kalendar.app.data.local.KalendarDatabase
import com.kalendar.app.data.local.entity.EventEntity
import com.kalendar.app.ui.components.EventCard
import com.kalendar.app.ui.components.HeroImage
import com.kalendar.app.ui.theme.BrandBlue
import com.kalendar.app.ui.theme.frostedPill
import com.kalendar.app.ui.theme.glassmorphic
import com.kalendar.app.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val INITIAL_DAY_PAGE = 5000

/**
 * Day View Screen matching Mockup #1 & #5.
 * 
 * Features:
 * - 120Hz smooth HorizontalPager day-by-day swipe with ZERO oscillation
 * - Full hero image card with Pexels API seasonal image
 * - Pulsing frosted glass event count indicator badge
 * - Interactive expandable bottom sheet showing all today's events
 * - Instant Jump to Date without pager lockup or feedback loops
 * - Prefilled event creation on swiped date
 * - Beautifully rounded 22dp 3-dot overflow menu with Share action
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayViewScreen(
    viewModel: DayViewViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToCalculateDate: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onEventClick: (Long) -> Unit,
    onCreateEvent: (LocalDate) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showEventsSheet by remember { mutableStateOf(false) }

    val baseDate = remember { LocalDate.now() }
    val pagerState = rememberPagerState(
        initialPage = INITIAL_DAY_PAGE,
        pageCount = { 10000 }
    )

    // Current page date derived from settled page
    val currentDayOffset = (pagerState.currentPage - INITIAL_DAY_PAGE).toLong()
    val currentSwipedDate = remember(pagerState.currentPage) { baseDate.plusDays(currentDayOffset) }

    // 1. External date changes (Jump to Date, Year View click) scroll pager to target
    LaunchedEffect(uiState.selectedDate) {
        val targetOffset = (uiState.selectedDate.toEpochDay() - baseDate.toEpochDay()).toInt()
        val targetPage = INITIAL_DAY_PAGE + targetOffset
        if (pagerState.currentPage != targetPage && targetPage in 0 until 10000) {
            pagerState.scrollToPage(targetPage)
        }
    }

    // 2. Swipes update ViewModel ONLY once settled — prevents any oscillating feedback loops
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collectLatest { settledPage ->
                val dayOffset = (settledPage - INITIAL_DAY_PAGE).toLong()
                val settledDate = baseDate.plusDays(dayOffset)
                if (settledDate != uiState.selectedDate) {
                    viewModel.selectDate(settledDate)
                }
            }
    }

    var showJumpToDatePicker by remember { mutableStateOf(false) }

    if (showJumpToDatePicker) {
        com.kalendar.app.ui.components.KalendarDatePickerDialog(
            initialDate = uiState.selectedDate,
            onDateSelected = { pickedDate ->
                viewModel.selectDate(pickedDate)
            },
            onDismiss = { showJumpToDatePicker = false }
        )
    }

    // Query events count for active date
    var currentDayEvents by remember { mutableStateOf<List<EventEntity>>(emptyList()) }
    LaunchedEffect(uiState.selectedDate) {
        withContext(Dispatchers.IO) {
            val db = KalendarDatabase.getInstance(context)
            val startMillis = DateUtils.toEpochMillis(uiState.selectedDate, LocalTime.MIN)
            val endMillis = DateUtils.toEpochMillis(uiState.selectedDate, LocalTime.MAX)
            val events = db.eventDao().getEventsForTimeRangeOnce(startMillis, endMillis)
            withContext(Dispatchers.Main) {
                currentDayEvents = events
            }
        }
    }

    // Pulse animation for event indicator dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Events Bottom Sheet for Current Day
    if (showEventsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEventsSheet = false },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = DateUtils.getDayOfWeekName(uiState.selectedDate) + ", " + uiState.selectedDate.format(DateTimeFormatter.ofPattern("d MMMM")),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (currentDayEvents.isEmpty()) "No events scheduled" else "${currentDayEvents.size} event(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            showEventsSheet = false
                            onCreateEvent(uiState.selectedDate)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddCircle,
                            contentDescription = "Add Event",
                            tint = BrandBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (currentDayEvents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No events for this day. Tap + to add one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(currentDayEvents, key = { it.id }) { event ->
                            val calColor = uiState.calendars[event.calendarId]?.let { Color(it.color) } ?: BrandBlue
                            EventCard(
                                event = event,
                                calendarColor = calColor,
                                onClick = {
                                    showEventsSheet = false
                                    onEventClick(event.id)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = { onCreateEvent(uiState.selectedDate) }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Create Event",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // Sleek rounded 22dp 3-dot overflow menu
                        MaterialTheme(
                            shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(18.dp))
                        ) {
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier
                                    .width(220.dp)
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(18.dp),
                                containerColor = MaterialTheme.colorScheme.surface,
                                shadowElevation = 8.dp
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Today", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Today,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = BrandBlue
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        viewModel.selectDate(LocalDate.now())
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Search for events", fontSize = 15.sp) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToSearch()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Jump to date", fontSize = 15.sp) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.CalendarMonth,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showJumpToDatePicker = true
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Calculate date", fontSize = 15.sp) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Calculate,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToCalculateDate()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Share", fontSize = 15.sp) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Share,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        val eventCount = currentDayEvents.size
                                        val eventSummary = if (eventCount > 0) {
                                            "\nEvents ($eventCount):\n" + currentDayEvents.joinToString("\n") { "• ${it.title} (${if (it.isAllDay) "All day" else DateUtils.formatTime(it.startTime)})" }
                                        } else {
                                            "\nNo events scheduled."
                                        }
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "📅 Date: ${uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))}$eventSummary\n\nShared with Kalendar"
                                            )
                                            type = "text/plain"
                                        }
                                        context.startActivity(
                                            Intent.createChooser(sendIntent, "Share Date & Events")
                                        )
                                    }
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                DropdownMenuItem(
                                    text = { Text("Settings", fontSize = 15.sp) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Settings,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToSettings()
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            val dayOffset = (page - INITIAL_DAY_PAGE).toLong()
            val pageDate = remember(page) { baseDate.plusDays(dayOffset) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // Hero Image Card with smooth click interaction
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.88f)
                        .clip(RoundedCornerShape(28.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showEventsSheet = true }
                ) {
                    HeroImage(
                        date = pageDate,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Bottom of hero image: Frosted Glass Event count indicator badge
                    AnimatedVisibility(
                        visible = currentDayEvents.isNotEmpty() && pageDate == uiState.selectedDate,
                        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 },
                        exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.Transparent,
                            modifier = Modifier
                                .frostedPill()
                                .clickable { showEventsSheet = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE53935))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (currentDayEvents.size == 1) "1 event today" else "${currentDayEvents.size} events today",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        letterSpacing = 0.2.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.kalendar.app.ui.calculatedate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kalendar.app.ui.components.KalendarDatePickerDialog
import com.kalendar.app.ui.theme.BrandBlue
import com.kalendar.app.ui.theme.glassmorphic
import com.kalendar.app.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class CalcTab {
    CALCULATE_DATE,
    INTERVAL,
    CONVERT
}

enum class Direction {
    FORWARDS,
    BACKWARDS
}

/**
 * Calculate Date Screen matching Mockup #4 with GPU-accelerated pickers and frosted glass styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculateDateScreen(
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(CalcTab.CALCULATE_DATE) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(30)) }
    var daysInput by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(Direction.FORWARDS) }
    var calculatedResult by remember { mutableStateOf<String?>(null) }
    var showDirectionMenu by remember { mutableStateOf(false) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    if (showStartDatePicker) {
        KalendarDatePickerDialog(
            initialDate = startDate,
            onDateSelected = { startDate = it },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        KalendarDatePickerDialog(
            initialDate = endDate,
            onDateSelected = { endDate = it },
            onDismiss = { showEndDatePicker = false }
        )
    }

    val fullDateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            // Blue Calculate Button at bottom matching Mockup #4
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Button(
                    onClick = {
                        when (selectedTab) {
                            CalcTab.CALCULATE_DATE -> {
                                val count = daysInput.toLongOrNull() ?: 0L
                                val resultDate = if (direction == Direction.FORWARDS) {
                                    startDate.plusDays(count)
                                } else {
                                    startDate.minusDays(count)
                                }
                                calculatedResult = resultDate.format(fullDateFormatter)
                            }
                            CalcTab.INTERVAL -> {
                                val daysBetween = ChronoUnit.DAYS.between(startDate, endDate)
                                calculatedResult = "$daysBetween days between dates"
                            }
                            CalcTab.CONVERT -> {
                                val epochMillis = DateUtils.getDayStartMillis(startDate)
                                calculatedResult = "Epoch: $epochMillis ms\nDay of Year: ${startDate.dayOfYear}"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text(
                        text = "Calculate",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        ),
                        color = Color.White
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Large Title
            Text(
                text = "Calculate date",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 32.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )

            // Segmented 3-pill Control matching Mockup #4
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .glassmorphic(shape = RoundedCornerShape(20.dp), containerAlpha = 0.5f, borderAlpha = 0.2f)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SegmentedPill(
                    label = "Calculate date",
                    isSelected = selectedTab == CalcTab.CALCULATE_DATE,
                    onClick = {
                        selectedTab = CalcTab.CALCULATE_DATE
                        calculatedResult = null
                    },
                    modifier = Modifier.weight(1.3f)
                )
                SegmentedPill(
                    label = "Interval",
                    isSelected = selectedTab == CalcTab.INTERVAL,
                    onClick = {
                        selectedTab = CalcTab.INTERVAL
                        calculatedResult = null
                    },
                    modifier = Modifier.weight(1f)
                )
                SegmentedPill(
                    label = "Convert",
                    isSelected = selectedTab == CalcTab.CONVERT,
                    onClick = {
                        selectedTab = CalcTab.CONVERT
                        calculatedResult = null
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            when (selectedTab) {
                CalcTab.CALCULATE_DATE -> {
                    // Start Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartDatePicker = true }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Start",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = startDate.format(fullDateFormatter),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Select start date",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Card matching Mockup #4: "Enter the number of days" with "Forwards ≍"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphic(shape = RoundedCornerShape(16.dp), containerAlpha = 0.85f, borderAlpha = 0.25f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextField(
                                value = daysInput,
                                onValueChange = { daysInput = it.filter { ch -> ch.isDigit() } },
                                placeholder = {
                                    Text(
                                        "Enter the number of days",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true
                            )

                            // Direction selector dropdown
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clickable { showDirectionMenu = true }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (direction == Direction.FORWARDS) "Forwards" else "Backwards",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Filled.UnfoldMore,
                                        contentDescription = "Direction",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showDirectionMenu,
                                    onDismissRequest = { showDirectionMenu = false },
                                    shape = RoundedCornerShape(16.dp),
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    shadowElevation = 8.dp
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Forwards") },
                                        onClick = {
                                            direction = Direction.FORWARDS
                                            showDirectionMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Backwards") },
                                        onClick = {
                                            direction = Direction.BACKWARDS
                                            showDirectionMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                CalcTab.INTERVAL -> {
                    // Start Date Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartDatePicker = true }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Start date",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = startDate.format(fullDateFormatter),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Select start date",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // End Date Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEndDatePicker = true }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "End date",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = endDate.format(fullDateFormatter),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Select end date",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                CalcTab.CONVERT -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartDatePicker = true }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Selected date",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = startDate.format(fullDateFormatter),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Select date",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Calculation Result Display
            calculatedResult?.let { result ->
                Spacer(modifier = Modifier.height(28.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphic(shape = RoundedCornerShape(16.dp), containerAlpha = 0.85f, borderAlpha = 0.25f)
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "RESULT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = BrandBlue
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = result,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.surface
                else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 13.sp
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

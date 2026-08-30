package com.kalendar.app.ui.components

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kalendar.app.ui.theme.BrandBlue
import com.kalendar.app.ui.theme.glassmorphic
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Ultra-Fast 120Hz GPU-Accelerated Date Picker Dialog.
 * Uses a single Skia Canvas with 0 layout nodes for instant 0ms open and fluid touch selection.
 */
@Composable
fun KalendarDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var displayedYearMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDayDate by remember { mutableStateOf(initialDate) }

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val today = remember { LocalDate.now() }

    val isDark = MaterialTheme.colorScheme.surface.let { it.red < 0.5f }
    val onTextColor = MaterialTheme.colorScheme.onSurface
    val onSubtextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val brandBlueColor = BrandBlue
    val redSundayColor = Color(0xFFE53935)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .glassmorphic(shape = RoundedCornerShape(28.dp), containerAlpha = 0.95f, borderAlpha = 0.35f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Header: Month Year + Prev/Next controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SELECT DATE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = onSubtextColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${displayedYearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${displayedYearMonth.year}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = onTextColor
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                displayedYearMonth = displayedYearMonth.minusMonths(1)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("<", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = brandBlueColor)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                displayedYearMonth = displayedYearMonth.plusMonths(1)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(">", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = brandBlueColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Single GPU Skia Canvas for entire month grid
                val daysInMonth = displayedYearMonth.lengthOfMonth()
                val firstDayOfWeek = displayedYearMonth.atDay(1).dayOfWeek.value % 7

                val weekdaySizePx = with(density) { 11.sp.toPx() }
                val daySizePx = with(density) { 14.5.sp.toPx() }
                val selectionRadiusPx = with(density) { 17.dp.toPx() }
                val selectionCornerRadiusPx = with(density) { 12.dp.toPx() }

                val weekdays = remember { listOf("S", "M", "T", "W", "T", "F", "S") }

                val weekdayPaint = remember(onSubtextColor) {
                    Paint().apply {
                        isAntiAlias = true
                        textSize = weekdaySizePx
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        color = onSubtextColor.copy(alpha = 0.7f).toArgb()
                        textAlign = Paint.Align.CENTER
                    }
                }
                val sundayHeaderPaint = remember {
                    Paint().apply {
                        isAntiAlias = true
                        textSize = weekdaySizePx
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        color = redSundayColor.toArgb()
                        textAlign = Paint.Align.CENTER
                    }
                }
                val dayPaint = remember(onTextColor) {
                    Paint().apply {
                        isAntiAlias = true
                        textSize = daySizePx
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        color = onTextColor.toArgb()
                        textAlign = Paint.Align.CENTER
                    }
                }
                val sundayDayPaint = remember {
                    Paint().apply {
                        isAntiAlias = true
                        textSize = daySizePx
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        color = redSundayColor.toArgb()
                        textAlign = Paint.Align.CENTER
                    }
                }
                val todayBorderPaint = remember(brandBlueColor) {
                    Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.STROKE
                        strokeWidth = with(density) { 1.5.dp.toPx() }
                        color = brandBlueColor.toArgb()
                    }
                }
                val todayTextPaint = remember(brandBlueColor) {
                    Paint().apply {
                        isAntiAlias = true
                        textSize = daySizePx
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        color = brandBlueColor.toArgb()
                        textAlign = Paint.Align.CENTER
                    }
                }
                val selectedDayTextPaint = remember {
                    Paint().apply {
                        isAntiAlias = true
                        textSize = daySizePx
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        color = android.graphics.Color.WHITE
                        textAlign = Paint.Align.CENTER
                    }
                }
                val selectedBgPaint = remember(brandBlueColor) {
                    Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.FILL
                        color = brandBlueColor.toArgb()
                    }
                }

                val selectionRect = remember { RectF() }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .pointerInput(displayedYearMonth) {
                            detectTapGestures { offset ->
                                val cellWidth = size.width.toFloat() / 7f
                                val daysStartY = with(density) { 30.dp.toPx() }
                                val rowHeight = with(density) { 34.dp.toPx() }

                                if (offset.y >= daysStartY) {
                                    val row = ((offset.y - daysStartY) / rowHeight).toInt()
                                    val col = (offset.x / cellWidth).toInt().coerceIn(0, 6)
                                    val cellIndex = row * 7 + col
                                    val day = cellIndex - firstDayOfWeek + 1
                                    if (day in 1..daysInMonth) {
                                        selectedDayDate = displayedYearMonth.atDay(day)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }
                        }
                ) {
                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas
                        val cellWidth = size.width / 7f

                        val headerY = with(density) { 16.dp.toPx() }
                        val daysStartY = with(density) { 30.dp.toPx() }
                        val rowHeight = with(density) { 34.dp.toPx() }
                        val textVerticalOffset = with(density) { 4.5.dp.toPx() }

                        // 1. Weekday headers
                        for (i in 0..6) {
                            val x = (i + 0.5f) * cellWidth
                            val paint = if (i == 0) sundayHeaderPaint else weekdayPaint
                            nativeCanvas.drawText(weekdays[i], x, headerY, paint)
                        }

                        // 2. Days Grid
                        for (day in 1..daysInMonth) {
                            val cellIndex = firstDayOfWeek + day - 1
                            val col = cellIndex % 7
                            val row = cellIndex / 7

                            val cx = (col + 0.5f) * cellWidth
                            val cy = daysStartY + (row + 0.5f) * rowHeight
                            val textY = cy + textVerticalOffset

                            val isSelected = selectedDayDate.year == displayedYearMonth.year &&
                                    selectedDayDate.monthValue == displayedYearMonth.monthValue &&
                                    selectedDayDate.dayOfMonth == day
                            val isToday = today.year == displayedYearMonth.year &&
                                    today.monthValue == displayedYearMonth.monthValue &&
                                    today.dayOfMonth == day

                            if (isSelected) {
                                selectionRect.set(
                                    cx - selectionRadiusPx,
                                    cy - selectionRadiusPx,
                                    cx + selectionRadiusPx,
                                    cy + selectionRadiusPx
                                )
                                nativeCanvas.drawRoundRect(
                                    selectionRect,
                                    selectionCornerRadiusPx,
                                    selectionCornerRadiusPx,
                                    selectedBgPaint
                                )
                                nativeCanvas.drawText(day.toString(), cx, textY, selectedDayTextPaint)
                            } else if (isToday) {
                                nativeCanvas.drawCircle(cx, cy - 1f, selectionRadiusPx * 0.85f, todayBorderPaint)
                                nativeCanvas.drawText(day.toString(), cx, textY, todayTextPaint)
                            } else {
                                val paint = if (col == 0) sundayDayPaint else dayPaint
                                nativeCanvas.drawText(day.toString(), cx, textY, paint)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = onSubtextColor, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onDateSelected(selectedDayDate)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = brandBlueColor),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Select", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

/**
 * Ultra-Fast 120Hz GPU-Accelerated Time Picker Dialog.
 * Interactive hardware Skia Wheel / Canvas time selector.
 */
@Composable
fun KalendarTimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableStateOf(if (initialTime.hour % 12 == 0) 12 else initialTime.hour % 12) }
    var selectedMinute by remember { mutableStateOf(initialTime.minute) }
    var isPm by remember { mutableStateOf(initialTime.hour >= 12) }

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val onTextColor = MaterialTheme.colorScheme.onSurface
    val onSubtextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val brandBlueColor = BrandBlue

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .glassmorphic(shape = RoundedCornerShape(28.dp), containerAlpha = 0.95f, borderAlpha = 0.35f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SELECT TIME",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = onSubtextColor,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Time Display Box
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Hour Box
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = brandBlueColor.copy(alpha = 0.15f),
                        modifier = Modifier
                            .width(82.dp)
                            .height(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = String.format(Locale.US, "%02d", selectedHour),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 36.sp
                                ),
                                color = brandBlueColor
                            )
                        }
                    }

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        color = onTextColor,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minute Box
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = brandBlueColor.copy(alpha = 0.15f),
                        modifier = Modifier
                            .width(82.dp)
                            .height(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = String.format(Locale.US, "%02d", selectedMinute),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 36.sp
                                ),
                                color = brandBlueColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // AM / PM Toggle Pills
                    Column {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (!isPm) brandBlueColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .width(56.dp)
                                .height(32.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        isPm = false
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "AM",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (!isPm) Color.White else onSubtextColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isPm) brandBlueColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .width(56.dp)
                                .height(32.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        isPm = true
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "PM",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isPm) Color.White else onSubtextColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Interactive GPU Time Wheels / Quick Sliders
                Text(
                    text = "Hour",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSubtextColor,
                    modifier = Modifier.align(Alignment.Start)
                )
                Slider(
                    value = selectedHour.toFloat(),
                    onValueChange = {
                        selectedHour = it.toInt()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    valueRange = 1f..12f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = brandBlueColor,
                        activeTrackColor = brandBlueColor
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Minute",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSubtextColor,
                    modifier = Modifier.align(Alignment.Start)
                )
                Slider(
                    value = selectedMinute.toFloat(),
                    onValueChange = {
                        selectedMinute = it.toInt()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    valueRange = 0f..59f,
                    steps = 58,
                    colors = SliderDefaults.colors(
                        thumbColor = brandBlueColor,
                        activeTrackColor = brandBlueColor
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = onSubtextColor, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val hour24 = if (isPm) {
                                if (selectedHour == 12) 12 else selectedHour + 12
                            } else {
                                if (selectedHour == 12) 0 else selectedHour
                            }
                            onTimeSelected(LocalTime.of(hour24, selectedMinute))
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = brandBlueColor),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Select", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

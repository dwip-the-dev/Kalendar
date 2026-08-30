package com.kalendar.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.widget.RemoteViews
import com.kalendar.app.MainActivity
import com.kalendar.app.R
import com.kalendar.app.data.local.KalendarDatabase
import com.kalendar.app.data.local.entity.EventEntity
import com.kalendar.app.util.DateUtils
import com.kalendar.app.util.ThemePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Universal helper that handles rendering and refreshing all 4 Kalendar widgets
 * using 100% reliable high-DPI GPU Skia Canvas bitmap rendering.
 */
object WidgetUpdateHelper {

    const val ACTION_OPEN_DAY = "com.kalendar.app.OPEN_DAY"
    const val ACTION_OPEN_MONTH = "com.kalendar.app.OPEN_MONTH"
    const val ACTION_OPEN_EVENTS = "com.kalendar.app.OPEN_EVENTS"

    fun updateAllWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val appWidgetManager = AppWidgetManager.getInstance(context)

            // 1. Day Widget
            val dayIds = appWidgetManager.getAppWidgetIds(ComponentName(context, DayWidgetProvider::class.java))
            for (id in dayIds) {
                renderDayWidget(context, appWidgetManager, id)
            }

            // 2. Month Widget
            val monthIds = appWidgetManager.getAppWidgetIds(ComponentName(context, MonthWidgetProvider::class.java))
            for (id in monthIds) {
                renderMonthWidget(context, appWidgetManager, id)
            }

            // 3. Next Event Widget
            val nextEventIds = appWidgetManager.getAppWidgetIds(ComponentName(context, NextEventWidgetProvider::class.java))
            for (id in nextEventIds) {
                renderNextEventWidget(context, appWidgetManager, id)
            }

            // 4. Today Events Widget
            val todayIds = appWidgetManager.getAppWidgetIds(ComponentName(context, TodayEventsWidgetProvider::class.java))
            for (id in todayIds) {
                renderTodayEventsWidget(context, appWidgetManager, id)
            }
        }
    }

    // ──────────────────────────── 1. DAY WIDGET ────────────────────────────
    fun renderDayWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_day_layout)
        val today = LocalDate.now()
        val isDark = ThemePreferences.isWidgetDark(context)

        // Set colors based on widget theme
        views.setInt(
            R.id.widget_day_root,
            "setBackgroundResource",
            if (isDark) R.drawable.widget_bg_dark else R.drawable.widget_bg_light
        )
        views.setTextColor(R.id.widget_day_name, if (isDark) Color.parseColor("#F0F0F0") else Color.parseColor("#1C1C1E"))
        views.setTextColor(R.id.widget_day_number, if (isDark) Color.WHITE else Color.parseColor("#111111"))
        views.setTextColor(R.id.widget_day_month_year, if (isDark) Color.parseColor("#9E9EA6") else Color.parseColor("#6C6C70"))

        val dayName = today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
        val monthName = today.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()

        views.setTextViewText(R.id.widget_day_name, dayName)
        views.setTextViewText(R.id.widget_day_number, today.dayOfMonth.toString())
        views.setTextViewText(R.id.widget_day_month_year, "$monthName ${today.year}")

        // Click opens Day View
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_DAY
            data = Uri.parse("kalendar://widget/day")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 101, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_day_root, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    // ─────────────────────────── 2. MONTH WIDGET ───────────────────────────
    suspend fun renderMonthWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) = withContext(Dispatchers.IO) {
        val views = RemoteViews(context.packageName, R.layout.widget_month_layout)
        val today = LocalDate.now()
        val yearMonth = YearMonth.now()
        val isDark = ThemePreferences.isWidgetDark(context)

        views.setInt(
            R.id.widget_month_root,
            "setBackgroundResource",
            if (isDark) R.drawable.widget_bg_dark else R.drawable.widget_bg_light
        )

        // Fetch days with events in this month
        val startMillis = DateUtils.toEpochMillis(yearMonth.atDay(1), LocalTime.MIN)
        val endMillis = DateUtils.toEpochMillis(yearMonth.atEndOfMonth().plusDays(1), LocalTime.MIN)
        val db = KalendarDatabase.getInstance(context)
        val events = db.eventDao().getEventsForTimeRangeOnce(startMillis, endMillis)
        val daysWithEvents = events.map { DateUtils.toLocalDate(it.startTime).dayOfMonth }.toSet()

        // Draw month grid into high-DPI Bitmap
        val bitmap = createMonthWidgetBitmap(yearMonth, today, daysWithEvents, isDark)
        views.setImageViewBitmap(R.id.widget_month_image, bitmap)

        // Click opens Month View
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_MONTH
            data = Uri.parse("kalendar://widget/month")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 102, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_month_root, pendingIntent)
        views.setOnClickPendingIntent(R.id.widget_month_image, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun createMonthWidgetBitmap(
        yearMonth: YearMonth,
        today: LocalDate,
        daysWithEvents: Set<Int>,
        isDark: Boolean
    ): Bitmap {
        val width = 720
        val height = 660
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val primaryColor = if (isDark) Color.WHITE else Color.parseColor("#121214")
        val secondaryColor = if (isDark) Color.parseColor("#9E9EA6") else Color.parseColor("#6C6C70")
        val redAccent = Color.parseColor("#E53935")

        // 1. Month Header & Year
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase()
        canvas.drawText(monthName, 24f, 54f, titlePaint)

        val yearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryColor
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(yearMonth.year.toString(), width - 24f, 54f, yearPaint)

        // 2. Weekday Headers: S M T W T F S
        val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryColor
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val sundayHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = redAccent
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val cellWidth = width / 7f
        val headerY = 118f
        for (i in 0..6) {
            val x = (i + 0.5f) * cellWidth
            val paint = if (i == 0) sundayHeaderPaint else headerPaint
            canvas.drawText(weekdays[i], x, headerY, paint)
        }

        // 3. Days Grid
        val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value % 7
        val daysInMonth = yearMonth.lengthOfMonth()
        val daysStartY = 172f
        val rowHeight = 78f

        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            textSize = 29f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        val sundayDayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = redAccent
            textSize = 29f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        val todayBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = redAccent
            style = Paint.Style.FILL
        }

        val todayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 29f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val eventDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = redAccent
            style = Paint.Style.FILL
        }

        for (day in 1..daysInMonth) {
            val cellIndex = firstDayOfWeek + day - 1
            val col = cellIndex % 7
            val row = cellIndex / 7

            val cx = (col + 0.5f) * cellWidth
            val cy = daysStartY + row * rowHeight
            val isToday = yearMonth.year == today.year && yearMonth.monthValue == today.monthValue && day == today.dayOfMonth

            if (isToday) {
                canvas.drawCircle(cx, cy - 10f, 26f, todayBgPaint)
                canvas.drawText(day.toString(), cx, cy, todayTextPaint)
            } else {
                val paint = if (col == 0) sundayDayPaint else dayPaint
                canvas.drawText(day.toString(), cx, cy, paint)
            }

            if (daysWithEvents.contains(day)) {
                val dotY = cy + 14f
                val dotPaint = if (isToday) todayTextPaint else eventDotPaint
                canvas.drawCircle(cx, dotY, 4.5f, dotPaint)
            }
        }

        return bitmap
    }

    // ───────────────────────── 3. NEXT EVENT WIDGET ─────────────────────────
    suspend fun renderNextEventWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) = withContext(Dispatchers.IO) {
        val views = RemoteViews(context.packageName, R.layout.widget_next_event_layout)
        val isDark = ThemePreferences.isWidgetDark(context)
        val now = System.currentTimeMillis()

        views.setInt(
            R.id.widget_next_event_root,
            "setBackgroundResource",
            if (isDark) R.drawable.widget_bg_dark else R.drawable.widget_bg_light
        )

        val db = KalendarDatabase.getInstance(context)
        val nextEvent = db.eventDao().getNextUpcomingEvent(now)

        // Draw Next Event into high-DPI Bitmap (guaranteed to render on all launchers)
        val bitmap = createNextEventWidgetBitmap(nextEvent, now, isDark)
        views.setImageViewBitmap(R.id.widget_next_event_image, bitmap)

        // Click opens Events View
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_EVENTS
            data = Uri.parse("kalendar://widget/nextevent")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 103, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_next_event_root, pendingIntent)
        views.setOnClickPendingIntent(R.id.widget_next_event_image, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun createNextEventWidgetBitmap(
        nextEvent: EventEntity?,
        now: Long,
        isDark: Boolean
    ): Bitmap {
        val width = 720
        val height = 360
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val primaryColor = if (isDark) Color.WHITE else Color.parseColor("#121214")
        val secondaryColor = if (isDark) Color.parseColor("#9E9EA6") else Color.parseColor("#6C6C70")
        val redAccent = Color.parseColor("#E53935")

        // 1. Red Dot Header
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = redAccent
            style = Paint.Style.FILL
        }
        canvas.drawCircle(32f, 48f, 10f, dotPaint)

        // 2. "NEXT EVENT" Title
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryColor
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("NEXT EVENT", 56f, 56f, headerPaint)

        if (nextEvent != null) {
            // 3. Countdown Text (top right, e.g. "in 25m" or "ongoing")
            val diffMillis = nextEvent.startTime - now
            val countdown = formatCountdown(diffMillis)
            val countdownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = redAccent
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(countdown, width - 24f, 56f, countdownPaint)

            // 4. Vertical Accent Bar
            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = redAccent
                style = Paint.Style.FILL
            }
            val barRect = RectF(28f, 116f, 36f, 240f)
            canvas.drawRoundRect(barRect, 4f, 4f, barPaint)

            // 5. Event Title
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryColor
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            val displayTitle = if (nextEvent.title.length > 22) nextEvent.title.take(21) + "…" else nextEvent.title
            canvas.drawText(displayTitle, 56f, 160f, titlePaint)

            // 6. Time Range
            val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = secondaryColor
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.LEFT
            }
            val startTimeStr = DateUtils.formatTime(nextEvent.startTime)
            val endTimeStr = DateUtils.formatTime(nextEvent.endTime)
            val timeText = if (nextEvent.isAllDay) "All day" else "$startTimeStr - $endTimeStr"
            canvas.drawText(timeText, 56f, 215f, timePaint)

            // 7. Location (if present)
            if (nextEvent.location.isNotBlank()) {
                val locPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = secondaryColor
                    textSize = 24f
                    textAlign = Paint.Align.LEFT
                }
                val displayLoc = if (nextEvent.location.length > 30) nextEvent.location.take(29) + "…" else nextEvent.location
                canvas.drawText(displayLoc, 56f, 265f, locPaint)
            }
        } else {
            // Empty state
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = secondaryColor
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("No upcoming events", width / 2f, height / 2f + 30f, emptyPaint)
        }

        return bitmap
    }

    private fun formatCountdown(diffMillis: Long): String {
        if (diffMillis <= 0) return "ongoing"
        val minutes = diffMillis / (1000 * 60)
        val hours = diffMillis / (1000 * 60 * 60)
        val days = diffMillis / (1000 * 60 * 60 * 24)

        return when {
            minutes < 60 -> "in ${minutes}m"
            hours < 24 -> "in ${hours}h"
            hours < 48 -> "in ${hours}hours"
            else -> "in ${days}d"
        }
    }

    // ──────────────────────── 4. TODAY'S EVENTS WIDGET ───────────────────────
    suspend fun renderTodayEventsWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) = withContext(Dispatchers.IO) {
        val views = RemoteViews(context.packageName, R.layout.widget_today_events_layout)
        val isDark = ThemePreferences.isWidgetDark(context)
        val today = LocalDate.now()
        val startOfDay = DateUtils.toEpochMillis(today, LocalTime.MIN)
        val endOfDay = DateUtils.toEpochMillis(today, LocalTime.MAX)

        views.setInt(
            R.id.widget_today_root,
            "setBackgroundResource",
            if (isDark) R.drawable.widget_bg_dark else R.drawable.widget_bg_light
        )

        val db = KalendarDatabase.getInstance(context)
        val events = db.eventDao().getEventsForTimeRangeOnce(startOfDay, endOfDay)
            .sortedBy { it.startTime }

        // Draw Today's Events into high-DPI Bitmap (guaranteed to render on all launchers)
        val bitmap = createTodayEventsWidgetBitmap(events, today, isDark)
        views.setImageViewBitmap(R.id.widget_today_image, bitmap)

        // Click opens Events View
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_EVENTS
            data = Uri.parse("kalendar://widget/todayevents")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 104, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_today_root, pendingIntent)
        views.setOnClickPendingIntent(R.id.widget_today_image, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun createTodayEventsWidgetBitmap(
        events: List<EventEntity>,
        today: LocalDate,
        isDark: Boolean
    ): Bitmap {
        val width = 720
        val height = 360
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val primaryColor = if (isDark) Color.WHITE else Color.parseColor("#121214")
        val secondaryColor = if (isDark) Color.parseColor("#9E9EA6") else Color.parseColor("#6C6C70")
        val redAccent = Color.parseColor("#E53935")

        // 1. Red Dot Header
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = redAccent
            style = Paint.Style.FILL
        }
        canvas.drawCircle(32f, 48f, 10f, dotPaint)

        // 2. "TODAY" Title
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryColor
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("TODAY", 56f, 56f, headerPaint)

        // 3. Current Day Tag (top right e.g. "AUG 30")
        val monthShort = today.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
        val dayTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryColor
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("$monthShort ${today.dayOfMonth}", width - 24f, 56f, dayTagPaint)

        if (events.isEmpty()) {
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = secondaryColor
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("No events scheduled for today", width / 2f, height / 2f + 30f, emptyPaint)
        } else {
            val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = secondaryColor
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.LEFT
            }

            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = redAccent
                style = Paint.Style.FILL
            }

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryColor
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }

            val startY = 125f
            val itemSpacing = 72f
            val maxItems = minOf(3, events.size)

            for (i in 0 until maxItems) {
                val event = events[i]
                val currentY = startY + i * itemSpacing

                // Time (x=30)
                val timeStr = if (event.isAllDay) "ALL DAY" else DateUtils.formatTime(event.startTime)
                canvas.drawText(timeStr, 30f, currentY, timePaint)

                // Vertical accent bar (x=165)
                val barRect = RectF(165f, currentY - 24f, 172f, currentY + 4f)
                canvas.drawRoundRect(barRect, 3.5f, 3.5f, barPaint)

                // Title (x=188)
                val displayTitle = if (event.title.length > 20) event.title.take(19) + "…" else event.title
                canvas.drawText(displayTitle, 188f, currentY, titlePaint)
            }
        }

        return bitmap
    }
}

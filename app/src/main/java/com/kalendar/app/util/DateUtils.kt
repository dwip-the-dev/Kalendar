package com.kalendar.app.util

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Date formatting and conversion utilities.
 */
object DateUtils {

    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    private val timeFormatter24 = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d")

    fun formatTime(epochMillis: Long): String {
        val time = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        return time.format(timeFormatter)
    }

    fun formatTime24(epochMillis: Long): String {
        val time = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        return time.format(timeFormatter24)
    }

    fun formatDate(date: LocalDate): String {
        return date.format(dateFormatter)
    }

    fun formatShortDate(date: LocalDate): String {
        return date.format(shortDateFormatter)
    }

    fun getDayOfWeekName(date: LocalDate): String {
        return date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()
    }

    fun getMonthName(date: LocalDate): String {
        return date.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase()
    }

    fun getMonthYearLabel(date: LocalDate): String {
        return "${getMonthName(date)} ${date.year}"
    }

    fun toEpochMillis(date: LocalDate, time: LocalTime): Long {
        return date.atTime(time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    fun toLocalDate(epochMillis: Long): LocalDate {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    fun toLocalTime(epochMillis: Long): LocalTime {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
    }

    fun toLocalDateTime(epochMillis: Long): LocalDateTime {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }

    fun getDayStartMillis(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getDayEndMillis(date: LocalDate): Long {
        return date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getTimeRangeText(startMillis: Long, endMillis: Long): String {
        return "${formatTime(startMillis)} — ${formatTime(endMillis)}"
    }

    fun isToday(date: LocalDate): Boolean = date == LocalDate.now()

    fun getDaysInMonth(year: Int, month: Int): Int {
        return YearMonth.of(year, month).lengthOfMonth()
    }

    fun getFirstDayOfWeek(year: Int, month: Int): DayOfWeek {
        return LocalDate.of(year, month, 1).dayOfWeek
    }
}

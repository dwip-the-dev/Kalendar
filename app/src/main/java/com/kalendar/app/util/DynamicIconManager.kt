package com.kalendar.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.kalendar.app.receiver.AlarmReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Manages 24-hour dynamic calendar app icons that update daily according to the date
 * in both White and Black theme modes.
 */
object DynamicIconManager {

    const val ACTION_UPDATE_ICON = "com.kalendar.app.UPDATE_ICON"

    /**
     * Updates the active launcher activity-alias to match today's date (1..31) and theme (White/Dark).
     * Ensures MainActivity remains fully enabled so widgets and notifications can always launch it.
     */
    fun updateAppIcon(context: Context) {
        try {
            val pm = context.packageManager
            val pkg = context.packageName
            val today = LocalDate.now()
            val day = today.dayOfMonth.coerceIn(1, 31)
            val iconTheme = ThemePreferences.getAppIconTheme(context)
            val isDark = iconTheme == "DARK"

            val activeAliasName = if (isDark) {
                "$pkg.MainActivityDarkDay$day"
            } else {
                "$pkg.MainActivityDay$day"
            }

            val targetComponent = ComponentName(pkg, activeAliasName)

            // 1. Enable the active target launcher alias first
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // 2. Ensure base MainActivity is ALWAYS enabled so widgets, pending intents, and shortcuts work!
            val mainComponent = ComponentName(pkg, "$pkg.MainActivity")
            if (pm.getComponentEnabledSetting(mainComponent) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                pm.setComponentEnabledSetting(
                    mainComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            // 3. Disable all other 61 inactive aliases
            for (d in 1..31) {
                val whiteAlias = "$pkg.MainActivityDay$d"
                val darkAlias = "$pkg.MainActivityDarkDay$d"

                if (whiteAlias != activeAliasName) {
                    val comp = ComponentName(pkg, whiteAlias)
                    if (pm.getComponentEnabledSetting(comp) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        pm.setComponentEnabledSetting(
                            comp,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                }

                if (darkAlias != activeAliasName) {
                    val comp = ComponentName(pkg, darkAlias)
                    if (pm.getComponentEnabledSetting(comp) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        pm.setComponentEnabledSetting(
                            comp,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Schedules a daily midnight alarm to update the dynamic app icon for the new day.
     */
    fun scheduleDailyMidnightUpdate(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_UPDATE_ICON
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                9999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Next midnight (00:00:05)
            val nextMidnight = LocalDateTime.now()
                .plusDays(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(5)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextMidnight,
                pendingIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

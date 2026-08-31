package com.kalendar.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.work.*
import com.kalendar.app.MainActivity
import com.kalendar.app.receiver.AlarmReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Manages 24-hour dynamic calendar app icons that update daily according to the date
 * in both White and Black theme modes.
 */
object DynamicIconManager {

    private const val TAG = "DynamicIconManager"
    const val ACTION_UPDATE_ICON = "com.kalendar.app.UPDATE_ICON"
    private const val PREFS_NAME = "dynamic_icon_prefs"
    private const val KEY_LAST_ALIAS = "last_enabled_alias"

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

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastAlias = prefs.getString(KEY_LAST_ALIAS, null)

            // If already enabled and matching, only verify and return
            val targetComponent = ComponentName(pkg, activeAliasName)
            val targetState = pm.getComponentEnabledSetting(targetComponent)

            if (lastAlias == activeAliasName && targetState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                // Ensure base MainActivity is also enabled
                val mainComponent = ComponentName(pkg, "$pkg.MainActivity")
                if (pm.getComponentEnabledSetting(mainComponent) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    pm.setComponentEnabledSetting(
                        mainComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
                return
            }

            Log.i(TAG, "Updating dynamic app icon to: $activeAliasName (Day: $day, Theme: $iconTheme)")

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

            // 3. Disable all other 61 inactive aliases cleanly
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

            // 4. Save current active alias in SharedPreferences
            prefs.edit().putString(KEY_LAST_ALIAS, activeAliasName).apply()

        } catch (e: Exception) {
            Log.e(TAG, "Error updating dynamic app icon", e)
        }
    }

    /**
     * Schedules a daily midnight alarm and background worker to update the dynamic app icon.
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

            // Next midnight (00:00:02)
            val nextMidnight = LocalDateTime.now()
                .plusDays(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(2)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val showIntent = Intent(context, MainActivity::class.java)
                    val showPendingIntent = PendingIntent.getActivity(
                        context,
                        9998,
                        showIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(nextMidnight, showPendingIntent),
                        pendingIntent
                    )
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextMidnight,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    nextMidnight,
                    pendingIntent
                )
            }

            // Also schedule periodic background WorkManager check as a fail-safe
            schedulePeriodicWorker(context)

        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling midnight icon update", e)
        }
    }

    /**
     * Periodic WorkManager fail-safe to guarantee dynamic icon stays synced on date changes
     * even if device was asleep or killed by OEM battery management.
     */
    fun schedulePeriodicWorker(context: Context) {
        try {
            val request = PeriodicWorkRequestBuilder<DailyIconUpdateWorker>(2, TimeUnit.HOURS)
                .setConstraints(Constraints.NONE)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "kalendar_daily_icon_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling periodic icon worker", e)
        }
    }
}

/**
 * Worker that ensures the launcher icon and widgets are up to date with today's date.
 */
class DailyIconUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            DynamicIconManager.updateAppIcon(context)
            DynamicIconManager.scheduleDailyMidnightUpdate(context)
            com.kalendar.app.widget.WidgetUpdateHelper.updateAllWidgets(context)
            NotificationHelper.rescheduleAllUpcomingReminders(context)
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}

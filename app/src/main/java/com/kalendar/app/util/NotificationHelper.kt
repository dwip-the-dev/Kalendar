package com.kalendar.app.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kalendar.app.MainActivity
import com.kalendar.app.R
import com.kalendar.app.data.local.KalendarDatabase
import com.kalendar.app.data.local.entity.EventEntity
import com.kalendar.app.receiver.AlarmReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles real Android system notifications, exact alarm scheduling, and instant reminder testing.
 */
object NotificationHelper {

    private const val TAG = "NotificationHelper"
    const val CHANNEL_ID = "kalendar_event_reminders_v2"
    const val CHANNEL_NAME = "Event Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for upcoming Kalendar events and reminders"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 200, 250)
                    enableLights(true)
                    setSound(soundUri, audioAttributes)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            } catch (e: Throwable) {
                try {
                    val fallbackChannel = NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Notifications for upcoming Kalendar events and reminders"
                    }
                    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.createNotificationChannel(fallbackChannel)
                } catch (e2: Throwable) {
                    e2.printStackTrace()
                }
            }
        }
    }

    /**
     * Schedules an exact system alarm to notify the user of an event at its reminder offset.
     */
    fun scheduleEventReminder(context: Context, event: EventEntity) {
        val reminderMinutes = event.reminder.toMinutes()
        if (event.reminder == com.kalendar.app.data.local.entity.ReminderTime.NONE) return

        val triggerTime = event.startTime - (reminderMinutes * 60 * 1000L)
        val now = System.currentTimeMillis()
        if (triggerTime <= now) return

        createNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_EVENT_ID, event.id)
            putExtra(AlarmReceiver.EXTRA_EVENT_TITLE, event.title)
            putExtra(AlarmReceiver.EXTRA_EVENT_LOCATION, event.location)
            putExtra(AlarmReceiver.EXTRA_EVENT_START_TIME, event.startTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val showIntent = Intent(context, MainActivity::class.java)
                val showPendingIntent = PendingIntent.getActivity(
                    context,
                    event.id.toInt(),
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled reminder for '${event.title}' at epoch $triggerTime")
        } catch (e: Exception) {
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to schedule reminder for '${event.title}'", e2)
            }
        }
    }

    /**
     * Reschedules reminders for all upcoming events in the next 30 days.
     */
    fun rescheduleAllUpcomingReminders(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val thirtyDaysLater = now + (30L * 24 * 60 * 60 * 1000L)
                val db = KalendarDatabase.getInstance(context)
                val upcomingEvents = db.eventDao().getEventsForTimeRangeOnce(now, thirtyDaysLater)

                Log.d(TAG, "Rescheduling reminders for ${upcomingEvents.size} upcoming events")
                for (event in upcomingEvents) {
                    if (event.reminder != com.kalendar.app.data.local.entity.ReminderTime.NONE) {
                        scheduleEventReminder(context, event)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling upcoming reminders", e)
            }
        }
    }

    fun cancelEventReminder(context: Context, eventId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun triggerTestNotification(context: Context) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Kalendar Reminder Test")
            .setContentText("Notifications and reminder alarms are working perfectly!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 200, 250))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(9999, notification)
    }
}

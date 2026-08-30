package com.kalendar.app.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.kalendar.app.MainActivity
import com.kalendar.app.R
import com.kalendar.app.util.DateUtils
import com.kalendar.app.util.DynamicIconManager
import com.kalendar.app.util.NotificationHelper
import com.kalendar.app.widget.WidgetUpdateHelper

/**
 * BroadcastReceiver triggered by AlarmManager to post heads-up notifications for upcoming events,
 * update 24-hour dynamic app icons, and refresh widgets on date change.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_EVENT_TITLE = "extra_event_title"
        const val EXTRA_EVENT_LOCATION = "extra_event_location"
        const val EXTRA_EVENT_START_TIME = "extra_event_start_time"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        // Handle dynamic icon update or date/timezone changes
        if (action == DynamicIconManager.ACTION_UPDATE_ICON ||
            action == Intent.ACTION_DATE_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_BOOT_COMPLETED
        ) {
            DynamicIconManager.updateAppIcon(context)
            DynamicIconManager.scheduleDailyMidnightUpdate(context)
            WidgetUpdateHelper.updateAllWidgets(context)
            return
        }

        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, 0L)
        if (eventId == 0L) return

        val title = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: "Upcoming Event"
        val location = intent.getStringExtra(EXTRA_EVENT_LOCATION) ?: ""
        val startTime = intent.getLongExtra(EXTRA_EVENT_START_TIME, System.currentTimeMillis())

        val timeString = DateUtils.formatTime(startTime)
        val contentText = if (location.isNotBlank()) "$timeString • $location" else timeString

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            eventId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 200, 250))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.mipmap.ic_launcher,
                "Open Kalendar",
                pendingIntent
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(eventId.toInt(), notification)
    }
}

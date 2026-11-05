package com.example.medicheckreminder.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.medicheckreminder.R

/**
 * Posts the full-screen alarm for an unanswered dose. With the screen off or locked, Android opens
 * [DoseAlarmActivity] straight away; while the phone is in use it shows a heads-up notification
 * that opens the same screen.
 *
 * https://developer.android.com/develop/ui/views/notifications/time-sensitive
 */
object AlarmNotifier {

    private const val TAG = "AlarmNotifier"
    private const val CHANNEL_ID = "medicheck_alarms"

    fun notificationId(doseId: String) = doseId.hashCode() + 47

    fun show(context: Context, source: Intent, doseId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Notification permission not granted; alarm not shown")
            return
        }
        ensureChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !manager.canUseFullScreenIntent()) {
            Log.d(TAG, "Full-screen intents are not allowed; the alarm shows as a heads-up notification")
        }

        val alarmScreen = Intent(context, DoseAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtras(source.extras ?: android.os.Bundle())
        }
        val fullScreen = PendingIntent.getActivity(
            context,
            notificationId(doseId),
            alarmScreen,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val name = source.getStringExtra(DoseReminderReceiver.EXTRA_NAME).orEmpty()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pill)
            .setContentTitle(context.getString(R.string.alarm_title))
            .setContentText(context.getString(R.string.alarm_notification_text, name))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .setOngoing(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId(doseId), notification)
            Log.d(TAG, "Full-screen alarm shown for $name")
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission was revoked", e)
        }
    }

    fun cancel(context: Context, doseId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(doseId))
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
            )
            enableVibration(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

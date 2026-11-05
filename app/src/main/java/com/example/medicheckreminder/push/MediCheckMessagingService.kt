package com.example.medicheckreminder.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.medicheckreminder.MainActivity
import com.example.medicheckreminder.MediCheckApp
import com.example.medicheckreminder.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives Firebase Cloud Messaging pushes. While the app is open, messages arrive here and are
 * shown on the "Updates and alerts" channel. While it is closed, Android shows the notification
 * part itself on the same channel (set as the default in the manifest), and the data part,
 * including `screen`, is handed to [MainActivity] when it is tapped.
 *
 * Adapted from https://firebase.google.com/docs/cloud-messaging/android/receive
 */
class MediCheckMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token")
        PushTokenRegistrar.save(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val settings = (application as MediCheckApp).container.settingsRepository.snapshot()
        if (!settings.pushEnabled) {
            Log.d(TAG, "Push received but push notifications are off in Settings")
            return
        }
        val push = PushRoutes.parse(message.notification?.title, message.notification?.body, message.data)
        if (push == null) {
            Log.d(TAG, "Push had no title or body; nothing to show")
            return
        }
        Log.d(TAG, "Push received: screen=${push.screen}")
        show(this, push)
    }

    companion object {
        private const val TAG = "MediCheckPush"
        const val CHANNEL_ID = "medicheck_updates"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.push_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        fun show(context: Context, push: PushMessage) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Notification permission not granted; push not shown")
                return
            }
            ensureChannel(context)
            val open = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                push.screen?.let { putExtra(PushRoutes.EXTRA_SCREEN, it) }
            }
            val pending = PendingIntent.getActivity(
                context,
                (push.title + push.body).hashCode(),
                open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pill)
                .setContentTitle(push.title)
                .setContentText(push.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(push.body))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
            } catch (e: SecurityException) {
                Log.e(TAG, "Notification permission was revoked", e)
            }
        }
    }
}

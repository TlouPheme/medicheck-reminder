package com.example.medicheckreminder.reminder

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
import com.example.medicheckreminder.R
import com.example.medicheckreminder.domain.model.Medication
import com.example.medicheckreminder.domain.refill.RefillCalculator
import com.example.medicheckreminder.push.PushRoutes
import java.text.DateFormat
import java.util.Date

/** Low-stock reminder for the refill calculator. Shown once, when the supply drops to a week or less. */
object RefillNotifier {

    private const val TAG = "RefillNotifier"
    private const val CHANNEL_ID = "medicheck_refills"

    fun showLowStock(context: Context, medication: Medication) {
        val days = RefillCalculator.daysRemaining(medication) ?: return
        if (!canNotify(context)) {
            Log.d(TAG, "Notifications are off; low stock for ${medication.name} not shown")
            return
        }
        ensureChannel(context)
        val refillDate = RefillCalculator.refillDateMillis(medication, System.currentTimeMillis())
            ?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) }
            .orEmpty()
        val text = if (days == 0) {
            context.getString(R.string.refill_notification_out, medication.name)
        } else {
            context.resources.getQuantityString(R.plurals.refill_notification_text, days, days, refillDate)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pill)
            .setContentTitle(context.getString(R.string.refill_notification_title, medication.name))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(("refill" + medication.id).hashCode(), notification)
            Log.d(TAG, "Low stock reminder shown for ${medication.name}: $days days left")
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission was revoked", e)
        }
    }

    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.refill_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(PushRoutes.EXTRA_SCREEN, PushRoutes.MEDICATIONS)
        }
        return PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

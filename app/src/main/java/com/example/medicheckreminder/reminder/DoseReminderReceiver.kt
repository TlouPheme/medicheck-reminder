package com.example.medicheckreminder.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.medicheckreminder.MainActivity
import com.example.medicheckreminder.MediCheckApp
import com.example.medicheckreminder.R
import com.example.medicheckreminder.domain.model.Dose
import com.example.medicheckreminder.ui.settings.SettingIds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DoseReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val doseId = intent.getStringExtra(EXTRA_DOSE_ID) ?: return
        val pending = goAsync()
        val app = context.applicationContext as MediCheckApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_TAKEN -> {
                        DoseAlarmScheduler.clearFollowUps(app, doseId)
                        record(app, intent, Dose.Status.TAKEN)
                        NotificationManagerCompat.from(app).cancel(doseId.hashCode())
                    }
                    ACTION_SNOOZE -> {
                        val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10).coerceAtLeast(1)
                        DoseAlarmScheduler.rememberSnooze(
                            app,
                            doseId,
                            System.currentTimeMillis() + minutes * 60_000L
                        )
                        record(app, intent, Dose.Status.SNOOZED)
                        NotificationManagerCompat.from(app).cancel(doseId.hashCode())
                    }
                    ACTION_ESCALATE -> showEscalation(app, intent, doseId)
                    else -> {
                        showDose(app, intent, doseId)
                        if (intent.getBooleanExtra(EXTRA_ESCALATION, false)) {
                            val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10).coerceAtLeast(1)
                            DoseAlarmScheduler.rememberEscalation(
                                app,
                                doseId,
                                System.currentTimeMillis() + minutes * 60_000L
                            )
                        }
                    }
                }
                val medications = app.container.medicationRepository.observeAll().first()
                val logs = app.container.doseLogRepository.observeAll().first()
                val settings = app.container.settingsRepository.snapshot()
                DoseAlarmScheduler.reschedule(app, medications, logs, settings)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun record(app: MediCheckApp, intent: Intent, status: Dose.Status) {
        val doseId = intent.getStringExtra(EXTRA_DOSE_ID) ?: return
        app.container.doseLogRepository.record(
            Dose(
                id = doseId,
                medicationName = intent.getStringExtra(EXTRA_NAME).orEmpty(),
                dosage = intent.getStringExtra(EXTRA_DOSAGE).orEmpty(),
                scheduledTime = intent.getStringExtra(EXTRA_TIME).orEmpty(),
                status = status,
                medicationId = intent.getStringExtra(EXTRA_MEDICATION_ID).orEmpty(),
                dateKey = intent.getStringExtra(EXTRA_DATE).orEmpty()
            )
        )
    }

    private fun showDose(context: Context, source: Intent, doseId: String) {
        if (!canNotify(context)) return
        ensureChannel(context, source)
        val name = source.getStringExtra(EXTRA_NAME).orEmpty()
        val dosage = source.getStringExtra(EXTRA_DOSAGE).orEmpty()
        val time = source.getStringExtra(EXTRA_TIME).orEmpty()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pill)
            .setContentTitle(context.getString(R.string.notification_dose_title))
            .setContentText(context.getString(R.string.notification_dose_text, name, dosage, time))
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(
                0,
                context.getString(R.string.action_taken),
                actionIntent(context, source, ACTION_TAKEN)
            )
            .addAction(
                0,
                context.getString(R.string.action_snooze),
                actionIntent(context, source, ACTION_SNOOZE)
            )
            .build()
        NotificationManagerCompat.from(context).notify(doseId.hashCode(), notification)
    }

    private fun showEscalation(context: Context, source: Intent, doseId: String) {
        if (!canNotify(context)) return
        ensureChannel(context, source)
        val name = source.getStringExtra(EXTRA_NAME).orEmpty()
        val caregiver = source.getStringExtra(EXTRA_CAREGIVER_NAME).orEmpty()
        val phone = source.getStringExtra(EXTRA_CAREGIVER_PHONE).orEmpty()
        val email = source.getStringExtra(EXTRA_CAREGIVER_EMAIL).orEmpty()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pill)
            .setContentTitle(context.getString(R.string.notification_escalation_title))
            .setContentText(
                context.getString(R.string.notification_escalation_text, name, caregiver)
            )
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
        if (phone.isNotBlank()) {
            builder.addAction(
                0,
                context.getString(R.string.notification_action_message),
                messageIntent(context, phone, name, doseId.hashCode() + 1)
            )
        }
        if (email.isNotBlank()) {
            builder.addAction(
                0,
                context.getString(R.string.notification_action_email),
                emailIntent(context, email, name, doseId.hashCode() + 2)
            )
        }
        NotificationManagerCompat.from(context).notify(doseId.hashCode() + 31, builder.build())
    }

    private fun actionIntent(context: Context, source: Intent, action: String): PendingIntent {
        val doseId = source.getStringExtra(EXTRA_DOSE_ID).orEmpty()
        val intent = Intent(context, DoseReminderReceiver::class.java).apply {
            this.action = action
            data = Uri.parse("medicheck://dose/${Uri.encode("$doseId|$action")}")
            putExtras(source.extras ?: android.os.Bundle())
        }
        return PendingIntent.getBroadcast(
            context,
            "$doseId|$action".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun messageIntent(
        context: Context,
        phone: String,
        medication: String,
        requestCode: Int
    ): PendingIntent {
        val body = context.getString(R.string.notification_escalation_message, medication)
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phone")
            putExtra("sms_body", body)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun emailIntent(
        context: Context,
        email: String,
        medication: String,
        requestCode: Int
    ): PendingIntent {
        val body = context.getString(R.string.notification_escalation_message, medication)
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.notification_escalation_title))
            putExtra(Intent.EXTRA_TEXT, body)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel(context: Context, source: Intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(CHANNEL_ID)
        val sound = source.getStringExtra(EXTRA_SOUND).orEmpty()
        val vibrate = source.getBooleanExtra(EXTRA_VIBRATE, true)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_doses),
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.enableVibration(vibrate)
        val uri = when (sound) {
            SettingIds.SOUND_SILENT -> null
            "" -> android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
            else -> Uri.parse(sound)
        }
        channel.setSound(
            uri,
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "dose_reminders"
        const val ACTION_SHOW = "com.example.medicheckreminder.DOSE_SHOW"
        const val ACTION_TAKEN = "com.example.medicheckreminder.DOSE_TAKEN"
        const val ACTION_SNOOZE = "com.example.medicheckreminder.DOSE_SNOOZE"
        const val ACTION_ESCALATE = "com.example.medicheckreminder.DOSE_ESCALATE"
        const val EXTRA_DOSE_ID = "dose_id"
        const val EXTRA_MEDICATION_ID = "medication_id"
        const val EXTRA_NAME = "name"
        const val EXTRA_DOSAGE = "dosage"
        const val EXTRA_TIME = "time"
        const val EXTRA_DATE = "date"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
        const val EXTRA_ESCALATION = "escalation"
        const val EXTRA_CAREGIVER_NAME = "caregiver_name"
        const val EXTRA_CAREGIVER_PHONE = "caregiver_phone"
        const val EXTRA_CAREGIVER_EMAIL = "caregiver_email"
        const val EXTRA_SOUND = "sound"
        const val EXTRA_VIBRATE = "vibrate"
    }
}

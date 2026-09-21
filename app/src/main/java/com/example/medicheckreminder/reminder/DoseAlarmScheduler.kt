package com.example.medicheckreminder.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.medicheckreminder.domain.model.AppSettings
import com.example.medicheckreminder.domain.model.Dose
import com.example.medicheckreminder.domain.model.DoseLog
import com.example.medicheckreminder.domain.model.Medication
import com.example.medicheckreminder.domain.schedule.DoseSchedule
import com.example.medicheckreminder.domain.schedule.ScheduledDose

object DoseAlarmScheduler {

    private const val PREFS = "dose_alarms"
    private const val KEY_SCHEDULED = "scheduled"
    private const val KEY_SNOOZES = "snoozes"
    private const val KEY_ESCALATIONS = "escalations"
    private const val HORIZON_MS = 48L * 60L * 60L * 1000L

    fun reschedule(
        context: Context,
        medications: List<Medication>,
        logs: List<DoseLog>,
        settings: AppSettings
    ) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        prefs.getStringSet(KEY_SCHEDULED, emptySet()).orEmpty().forEach { key ->
            alarmManager.cancel(pendingIntent(appContext, key))
        }

        if (!settings.notificationsEnabled) {
            prefs.edit()
                .putStringSet(KEY_SCHEDULED, emptySet())
                .putStringSet(KEY_SNOOZES, emptySet())
                .putStringSet(KEY_ESCALATIONS, emptySet())
                .apply()
            return
        }

        val settled = logs
            .filter { it.status == Dose.Status.TAKEN || it.status == Dose.Status.SKIPPED }
            .map { it.id }
            .toSet()
        val now = System.currentTimeMillis()
        val alarms = linkedMapOf<String, Long>()
        DoseSchedule.upcoming(medications, now, HORIZON_MS)
            .filter { it.id !in settled }
            .forEach { dose -> alarms[dose.id] = dose.triggerAtMillis }

        val snoozes = retainFuture(prefs.getStringSet(KEY_SNOOZES, emptySet()).orEmpty(), settled, now)
        snoozes.forEach { (doseId, trigger) -> alarms[doseId] = trigger }
        val escalations = retainFuture(
            prefs.getStringSet(KEY_ESCALATIONS, emptySet()).orEmpty(),
            settled,
            now
        )
        escalations.forEach { (doseId, trigger) -> alarms[escalationKey(doseId)] = trigger }

        alarms.forEach { (key, trigger) ->
            val dose = doseFor(key, medications, logs)
            if (dose != null) {
                schedule(appContext, alarmManager, key, trigger, dose, settings)
            }
        }
        prefs.edit()
            .putStringSet(KEY_SCHEDULED, alarms.keys.toSet())
            .putStringSet(KEY_SNOOZES, snoozes.map { "${it.first}|${it.second}" }.toSet())
            .putStringSet(KEY_ESCALATIONS, escalations.map { "${it.first}|${it.second}" }.toSet())
            .apply()
    }

    fun rememberSnooze(context: Context, doseId: String, triggerAtMillis: Long) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val snoozes = prefs.getStringSet(KEY_SNOOZES, emptySet()).orEmpty()
            .filterNot { it.substringBeforeLast("|") == doseId }
            .toMutableSet()
        snoozes += "$doseId|$triggerAtMillis"
        val escalations = prefs.getStringSet(KEY_ESCALATIONS, emptySet()).orEmpty()
            .filterNot { it.substringBeforeLast("|") == doseId }
            .toSet()
        prefs.edit()
            .putStringSet(KEY_SNOOZES, snoozes)
            .putStringSet(KEY_ESCALATIONS, escalations)
            .apply()
    }

    fun rememberEscalation(context: Context, doseId: String, triggerAtMillis: Long) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val escalations = prefs.getStringSet(KEY_ESCALATIONS, emptySet()).orEmpty()
            .filterNot { it.substringBeforeLast("|") == doseId }
            .toMutableSet()
        escalations += "$doseId|$triggerAtMillis"
        prefs.edit().putStringSet(KEY_ESCALATIONS, escalations).apply()
    }

    fun clearFollowUps(context: Context, doseId: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet(
                KEY_SNOOZES,
                prefs.getStringSet(KEY_SNOOZES, emptySet()).orEmpty()
                    .filterNot { it.substringBeforeLast("|") == doseId }
                    .toSet()
            )
            .putStringSet(
                KEY_ESCALATIONS,
                prefs.getStringSet(KEY_ESCALATIONS, emptySet()).orEmpty()
                    .filterNot { it.substringBeforeLast("|") == doseId }
                    .toSet()
            )
            .apply()
    }

    private fun retainFuture(
        stored: Set<String>,
        settled: Set<String>,
        now: Long
    ): List<Pair<String, Long>> {
        return stored.mapNotNull { entry ->
            val doseId = entry.substringBeforeLast("|")
            val trigger = entry.substringAfterLast("|").toLongOrNull() ?: return@mapNotNull null
            if (doseId in settled || trigger <= now) null else doseId to trigger
        }
    }

    private fun doseFor(
        key: String,
        medications: List<Medication>,
        logs: List<DoseLog>
    ): ScheduledDose? {
        val doseId = key.removeSuffix("|escalate")
        val parts = doseId.split("|")
        if (parts.size < 3) return null
        val medicationId = parts[0]
        val dateKey = parts[1]
        val time = parts.drop(2).joinToString("|")
        val medication = medications.find { it.id == medicationId }
        val log = logs.find { it.id == doseId }
        val name = medication?.name ?: log?.medicationName ?: return null
        val dosage = medication?.dosage ?: log?.dosage.orEmpty()
        return ScheduledDose(
            id = doseId,
            medicationId = medicationId,
            medicationName = name,
            dosage = dosage,
            scheduledTime = time,
            dateKey = dateKey,
            triggerAtMillis = 0L
        )
    }

    private fun schedule(
        context: Context,
        alarmManager: AlarmManager,
        key: String,
        triggerAtMillis: Long,
        dose: ScheduledDose,
        settings: AppSettings
    ) {
        val pending = pendingIntent(context, key, dose, settings)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pending
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pending
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pending
            )
        }
    }

    private fun pendingIntent(
        context: Context,
        key: String,
        dose: ScheduledDose? = null,
        settings: AppSettings? = null
    ): PendingIntent {
        val escalate = key.endsWith("|escalate")
        val intent = Intent(context, DoseReminderReceiver::class.java).apply {
            action = if (escalate) DoseReminderReceiver.ACTION_ESCALATE else DoseReminderReceiver.ACTION_SHOW
            data = Uri.parse("medicheck://dose/${Uri.encode(key)}")
            if (dose != null) {
                putExtra(DoseReminderReceiver.EXTRA_DOSE_ID, dose.id)
                putExtra(DoseReminderReceiver.EXTRA_MEDICATION_ID, dose.medicationId)
                putExtra(DoseReminderReceiver.EXTRA_NAME, dose.medicationName)
                putExtra(DoseReminderReceiver.EXTRA_DOSAGE, dose.dosage)
                putExtra(DoseReminderReceiver.EXTRA_TIME, dose.scheduledTime)
                putExtra(DoseReminderReceiver.EXTRA_DATE, dose.dateKey)
            }
            if (settings != null) {
                putExtra(DoseReminderReceiver.EXTRA_SNOOZE_MINUTES, settings.snoozeMinutes)
                putExtra(DoseReminderReceiver.EXTRA_ESCALATION, settings.escalationEnabled)
                putExtra(DoseReminderReceiver.EXTRA_CAREGIVER_NAME, settings.caregiverName)
                putExtra(DoseReminderReceiver.EXTRA_CAREGIVER_PHONE, settings.caregiverPhone)
                putExtra(DoseReminderReceiver.EXTRA_CAREGIVER_EMAIL, settings.caregiverEmail)
                putExtra(DoseReminderReceiver.EXTRA_SOUND, settings.notificationSoundUri)
                putExtra(DoseReminderReceiver.EXTRA_VIBRATE, settings.vibrationEnabled)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            key.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun escalationKey(doseId: String) = "$doseId|escalate"
}

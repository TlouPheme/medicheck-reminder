package com.example.medicheckreminder.domain.schedule

import com.example.medicheckreminder.domain.model.Dose
import com.example.medicheckreminder.domain.model.DoseLog
import com.example.medicheckreminder.domain.model.Frequency
import com.example.medicheckreminder.domain.model.Medication
import java.util.Calendar
import java.util.Locale

object DoseSchedule {

    fun dosesForDay(
        medications: List<Medication>,
        recorded: List<DoseLog>,
        day: Calendar,
        todayKey: String
    ): List<Dose> {
        return logsForDays(medications, recorded, listOf(day), todayKey).map { log ->
            Dose(
                id = log.id,
                medicationName = log.medicationName,
                dosage = log.dosage,
                scheduledTime = log.scheduledTime,
                status = log.status,
                medicationId = medicationIdOf(log.id),
                dateKey = log.dateKey
            )
        }
    }

    fun logsForDays(
        medications: List<Medication>,
        recorded: List<DoseLog>,
        days: List<Calendar>,
        todayKey: String
    ): List<DoseLog> {
        val recordedById = recorded.associateBy { it.id }
        val logs = mutableListOf<DoseLog>()
        days.forEach { day ->
            val dateKey = dateKey(day)
            medications.forEach { medication ->
                if (dateKey < createdOnKey(medication)) return@forEach
                if (!occursOn(medication, day)) return@forEach
                scheduledTimes(medication).forEach { time ->
                    val id = logId(medication.id, dateKey, time)
                    val existing = recordedById[id]
                    val status = when {
                        existing != null -> existing.status
                        dateKey < todayKey -> Dose.Status.MISSED
                        else -> Dose.Status.PENDING
                    }
                    logs += DoseLog(
                        id = id,
                        medicationName = medication.name,
                        dosage = medication.dosage,
                        scheduledTime = time,
                        dateKey = dateKey,
                        status = status
                    )
                }
            }
        }
        return logs
    }

    fun logId(medicationId: String, dateKey: String, time: String): String {
        return "$medicationId|$dateKey|$time"
    }

    fun dateKey(day: Calendar): String {
        val year = day.get(Calendar.YEAR)
        val month = day.get(Calendar.MONTH) + 1
        val date = day.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, date)
    }

    private fun medicationIdOf(logId: String): String = logId.substringBefore("|")

    /** Days before the medication was added are not scheduled, so they are not missed. */
    private fun createdOnKey(medication: Medication): String {
        if (medication.createdAt <= 0L) return ""
        val created = Calendar.getInstance().apply { timeInMillis = medication.createdAt }
        return dateKey(created)
    }

    private fun occursOn(medication: Medication, day: Calendar): Boolean {
        return when (medication.frequency) {
            Frequency.DAILY, Frequency.EVERY_X_HOURS -> true
            Frequency.SPECIFIC_DAYS -> weekdayCode(day) in medication.daysOfWeek
            Frequency.AS_NEEDED -> false
        }
    }

    /** Monday = 1 … Sunday = 7, matching the add-medication day chips. */
    private fun weekdayCode(day: Calendar): Int {
        val calendarDay = day.get(Calendar.DAY_OF_WEEK)
        return if (calendarDay == Calendar.SUNDAY) 7 else calendarDay - 1
    }

    private fun scheduledTimes(medication: Medication): List<String> {
        return when (medication.frequency) {
            Frequency.AS_NEEDED -> emptyList()
            Frequency.EVERY_X_HOURS -> intervalTimes(medication)
            Frequency.DAILY, Frequency.SPECIFIC_DAYS -> medication.times.sorted()
        }
    }

    private fun intervalTimes(medication: Medication): List<String> {
        val hours = medication.intervalHours
        if (hours == null || hours <= 0) return medication.times.sorted()
        val start = medication.times.firstOrNull()?.let(::parseMinutes) ?: (8 * 60)
        val times = mutableListOf<String>()
        var minute = start
        while (minute < 24 * 60) {
            times += formatMinutes(minute)
            minute += hours * 60
        }
        return times
    }

    private fun parseMinutes(time: String): Int? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    private fun formatMinutes(total: Int): String {
        val hour = total / 60
        val minute = total % 60
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }
}

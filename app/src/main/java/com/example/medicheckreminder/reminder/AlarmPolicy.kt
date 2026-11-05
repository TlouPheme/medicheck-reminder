package com.example.medicheckreminder.reminder

import com.example.medicheckreminder.domain.model.Dose

/**
 * Escalating reminders (Part 1 FR-INN-01, built as user-defined feature 5): a dose reminder that
 * is not answered within [ALARM_DELAY_MINUTES] becomes a full-screen alarm.
 */
object AlarmPolicy {

    const val ALARM_DELAY_MINUTES = 10

    fun alarmAt(shownAtMillis: Long): Long = shownAtMillis + ALARM_DELAY_MINUTES * 60_000L

    /** The alarm only rings for a dose nobody has answered yet. Snoozing re-arms it with the next reminder. */
    fun shouldRing(status: Dose.Status?): Boolean =
        status == null || status == Dose.Status.PENDING || status == Dose.Status.MISSED
}

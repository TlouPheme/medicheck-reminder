package com.example.medicheckreminder.domain.schedule

import com.example.medicheckreminder.domain.model.Dose
import com.example.medicheckreminder.domain.model.DoseLog
import com.example.medicheckreminder.domain.model.Frequency
import com.example.medicheckreminder.domain.model.Medication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DoseScheduleTest {

    private fun day(year: Int, month: Int, date: Int): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, date)
        }

    // 3 November 2026 is a Tuesday (weekday code 2).
    private val tuesday = day(2026, 11, 3)
    private val today = "2026-11-03"

    @Test
    fun dateKey_isZeroPadded() {
        assertEquals("2026-01-05", DoseSchedule.dateKey(day(2026, 1, 5)))
    }

    @Test
    fun dailyMedication_hasOneDosePerTime_inOrder() {
        val med = Medication(id = "m1", name = "Metformin", times = listOf("20:00", "08:00"))
        val doses = DoseSchedule.dosesForDay(listOf(med), emptyList(), tuesday, today)
        assertEquals(listOf("08:00", "20:00"), doses.map { it.scheduledTime })
        assertTrue(doses.all { it.status == Dose.Status.PENDING })
    }

    @Test
    fun specificDays_onlyScheduledOnChosenWeekdays() {
        val med = Medication(
            id = "m1", name = "Vit D", frequency = Frequency.SPECIFIC_DAYS,
            times = listOf("09:00"), daysOfWeek = setOf(1, 3)
        )
        assertTrue(DoseSchedule.dosesForDay(listOf(med), emptyList(), tuesday, today).isEmpty())
    }

    @Test
    fun everyEightHours_fillsTheDayFromTheFirstTime() {
        val med = Medication(
            id = "m1", name = "Amoxicillin", frequency = Frequency.EVERY_X_HOURS,
            times = listOf("06:00"), intervalHours = 8
        )
        val doses = DoseSchedule.dosesForDay(listOf(med), emptyList(), tuesday, today)
        assertEquals(listOf("06:00", "14:00", "22:00"), doses.map { it.scheduledTime })
    }

    @Test
    fun pastDayWithoutLog_isMissed_butRecordedStatusWins() {
        val med = Medication(id = "m1", name = "Metformin", times = listOf("08:00", "20:00"))
        val monday = day(2026, 11, 2)
        val taken = DoseLog(
            id = DoseSchedule.logId("m1", "2026-11-02", "08:00"),
            medicationName = "Metformin",
            dosage = "",
            scheduledTime = "08:00",
            dateKey = "2026-11-02",
            status = Dose.Status.TAKEN
        )
        val doses = DoseSchedule.dosesForDay(listOf(med), listOf(taken), monday, today)
        assertEquals(listOf(Dose.Status.TAKEN, Dose.Status.MISSED), doses.map { it.status })
    }

    @Test
    fun daysBeforeTheMedicationWasAdded_areNotScheduled() {
        val med = Medication(id = "m1", name = "Metformin", times = listOf("08:00"), createdAt = tuesday.timeInMillis)
        assertTrue(DoseSchedule.dosesForDay(listOf(med), emptyList(), day(2026, 11, 2), today).isEmpty())
    }
}

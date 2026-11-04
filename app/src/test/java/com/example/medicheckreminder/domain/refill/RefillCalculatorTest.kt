package com.example.medicheckreminder.domain.refill

import com.example.medicheckreminder.domain.model.Dose
import com.example.medicheckreminder.domain.model.Frequency
import com.example.medicheckreminder.domain.model.Medication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RefillCalculatorTest {

    private fun med(
        stock: Int,
        frequency: Frequency = Frequency.DAILY,
        times: List<String> = listOf("08:00", "20:00"),
        days: Set<Int> = emptySet(),
        interval: Int? = null
    ) = Medication(
        id = "m1", name = "Metformin", stockCount = stock, frequency = frequency,
        times = times, daysOfWeek = days, intervalHours = interval
    )

    @Test
    fun daysRemaining_dependsOnTheSchedule() {
        assertEquals(14, RefillCalculator.daysRemaining(med(28)))
        assertEquals(9, RefillCalculator.daysRemaining(med(28, Frequency.EVERY_X_HOURS, listOf("06:00"), interval = 8)))
        // Twice a week, one dose each: 5 doses last 17.5 days, rounded down to 17.
        assertEquals(17, RefillCalculator.daysRemaining(med(5, Frequency.SPECIFIC_DAYS, listOf("09:00"), setOf(1, 4))))
        assertNull(RefillCalculator.daysRemaining(med(28, Frequency.AS_NEEDED)))
    }

    @Test
    fun refillDate_isTodayPlusDaysRemaining() {
        val today = Calendar.getInstance().apply { clear(); set(2026, Calendar.NOVEMBER, 4, 15, 30) }
        val expected = Calendar.getInstance().apply { clear(); set(2026, Calendar.NOVEMBER, 18) }
        assertEquals(expected.timeInMillis, RefillCalculator.refillDateMillis(med(28), today.timeInMillis))
    }

    @Test
    fun lowStock_isAWeekOrLess() {
        assertFalse(RefillCalculator.isLow(med(16)))
        assertTrue(RefillCalculator.isLow(med(14)))
        assertTrue(RefillCalculator.isLow(med(0)))
    }

    @Test
    fun reminderFiresOnlyWhenCrossingIntoLow() {
        assertTrue(RefillCalculator.becameLow(med(16), med(15)))
        assertFalse(RefillCalculator.becameLow(med(14), med(13)))
        assertFalse(RefillCalculator.becameLow(med(30), med(29)))
    }

    @Test
    fun stockChange_takingUsesOne_andUndoingPutsItBack() {
        assertEquals(-1, RefillCalculator.stockChange(null, Dose.Status.TAKEN))
        assertEquals(-1, RefillCalculator.stockChange(Dose.Status.SNOOZED, Dose.Status.TAKEN))
        assertEquals(0, RefillCalculator.stockChange(Dose.Status.TAKEN, Dose.Status.TAKEN))
        assertEquals(1, RefillCalculator.stockChange(Dose.Status.TAKEN, Dose.Status.SKIPPED))
        assertEquals(0, RefillCalculator.stockChange(null, Dose.Status.SKIPPED))
    }
}

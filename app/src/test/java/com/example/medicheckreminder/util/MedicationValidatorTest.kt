package com.example.medicheckreminder.util

import com.example.medicheckreminder.R
import com.example.medicheckreminder.domain.model.Frequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicationValidatorTest {

    private fun draft(
        name: String = "Metformin",
        dosage: String = "500 mg",
        frequency: Frequency = Frequency.DAILY,
        times: List<String> = listOf("08:00", "20:00"),
        daysOfWeek: Set<Int> = emptySet(),
        intervalHours: String = "",
        stockCount: String = "28"
    ) = MedicationDraft(name, dosage, frequency, times, daysOfWeek, intervalHours, stockCount, notes = "")

    @Test
    fun completeDailyMedication_isValid() {
        assertTrue(MedicationValidator.validate(draft()).isValid)
    }

    @Test
    fun missingNameAndDosage_areReported() {
        val result = MedicationValidator.validate(draft(name = " ", dosage = ""))
        assertEquals(R.string.error_medication_name, result.nameError)
        assertEquals(R.string.error_medication_dosage, result.dosageError)
    }

    @Test
    fun specificDaysWithoutDays_isInvalid() {
        val result = MedicationValidator.validate(draft(frequency = Frequency.SPECIFIC_DAYS))
        assertEquals(R.string.error_medication_days, result.daysError)
    }

    @Test
    fun intervalMustBeBetweenOneAnd24Hours() {
        fun intervalError(raw: String) =
            MedicationValidator.validate(draft(frequency = Frequency.EVERY_X_HOURS, intervalHours = raw)).intervalError
        assertEquals(R.string.error_medication_interval, intervalError(""))
        assertEquals(R.string.error_medication_interval_positive, intervalError("0"))
        assertEquals(R.string.error_medication_interval_range, intervalError("25"))
        assertNull(intervalError("8"))
    }

    @Test
    fun negativeStock_isInvalid() {
        assertFalse(MedicationValidator.validate(draft(stockCount = "-1")).isValid)
    }

    @Test
    fun daysRemaining_usesDosesPerDay() {
        assertEquals(14, MedicationValidator.daysRemaining(draft(stockCount = "28")))
        assertEquals(
            9,
            MedicationValidator.daysRemaining(
                draft(frequency = Frequency.EVERY_X_HOURS, intervalHours = "8", stockCount = "28")
            )
        )
        assertNull(MedicationValidator.daysRemaining(draft(frequency = Frequency.AS_NEEDED)))
    }
}

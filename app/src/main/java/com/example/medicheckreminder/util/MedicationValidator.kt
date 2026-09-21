package com.example.medicheckreminder.util

import com.example.medicheckreminder.R
import com.example.medicheckreminder.domain.model.Frequency
import kotlin.math.floor

data class MedicationDraft(
    val name: String,
    val dosage: String,
    val frequency: Frequency,
    val times: List<String>,
    val daysOfWeek: Set<Int>,
    val intervalHours: String,
    val stockCount: String,
    val notes: String
)

object MedicationValidator {

    data class Result(
        val nameError: Int? = null,
        val dosageError: Int? = null,
        val timesError: Int? = null,
        val daysError: Int? = null,
        val intervalError: Int? = null,
        val stockError: Int? = null
    ) {
        val isValid: Boolean
            get() = nameError == null &&
                dosageError == null &&
                timesError == null &&
                daysError == null &&
                intervalError == null &&
                stockError == null
    }

    fun validate(draft: MedicationDraft): Result {
        val nameError = if (draft.name.isBlank()) R.string.error_medication_name else null
        val dosageError = if (draft.dosage.isBlank()) R.string.error_medication_dosage else null

        val timesError = when (draft.frequency) {
            Frequency.DAILY, Frequency.SPECIFIC_DAYS ->
                if (draft.times.isEmpty()) R.string.error_medication_times else null
            Frequency.EVERY_X_HOURS, Frequency.AS_NEEDED -> null
        }

        val daysError = if (draft.frequency == Frequency.SPECIFIC_DAYS && draft.daysOfWeek.isEmpty()) {
            R.string.error_medication_days
        } else {
            null
        }

        val interval = draft.intervalHours.trim().toIntOrNull()
        val intervalError = if (draft.frequency == Frequency.EVERY_X_HOURS) {
            when {
                draft.intervalHours.isBlank() -> R.string.error_medication_interval
                interval == null || interval <= 0 -> R.string.error_medication_interval_positive
                interval > 24 -> R.string.error_medication_interval_range
                else -> null
            }
        } else {
            null
        }

        val stockError = if (draft.stockCount.isNotBlank()) {
            val stock = draft.stockCount.toIntOrNull()
            if (stock == null || stock < 0) R.string.error_medication_stock else null
        } else {
            null
        }

        return Result(
            nameError = nameError,
            dosageError = dosageError,
            timesError = timesError,
            daysError = daysError,
            intervalError = intervalError,
            stockError = stockError
        )
    }

    fun parseStock(raw: String): Int = raw.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0

    fun parseIntervalHours(raw: String): Int? = raw.trim().toIntOrNull()

    fun daysRemaining(draft: MedicationDraft): Int? {
        val stock = parseStock(draft.stockCount)
        val dosesPerDay = when (draft.frequency) {
            Frequency.DAILY -> draft.times.size.coerceAtLeast(0).toDouble()
            Frequency.SPECIFIC_DAYS -> {
                if (draft.daysOfWeek.isEmpty() || draft.times.isEmpty()) return null
                (draft.times.size * draft.daysOfWeek.size) / 7.0
            }
            Frequency.EVERY_X_HOURS -> {
                val hours = parseIntervalHours(draft.intervalHours) ?: return null
                if (hours <= 0) return null
                24.0 / hours
            }
            Frequency.AS_NEEDED -> return null
        }
        if (dosesPerDay <= 0.0) return null
        return floor(stock / dosesPerDay).toInt()
    }
}

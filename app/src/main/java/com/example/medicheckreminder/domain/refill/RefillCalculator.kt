package com.example.medicheckreminder.domain.refill

import com.example.medicheckreminder.domain.model.Dose
import com.example.medicheckreminder.domain.model.Frequency
import com.example.medicheckreminder.domain.model.Medication
import java.util.Calendar
import kotlin.math.floor

/**
 * Refill calculator (Part 1 innovative feature 5, built as user-defined feature 4).
 *
 * Stock goes down by one each time a dose is marked Taken. From the stock and the schedule the
 * app works out how many days are left and the date a refill is needed, and warns when that is
 * a week or less away.
 */
object RefillCalculator {

    /** A refill reminder is shown once the supply drops to this many days or fewer. */
    const val LOW_STOCK_DAYS = 7

    fun dosesPerDay(medication: Medication): Double? = when (medication.frequency) {
        Frequency.DAILY -> medication.times.size.toDouble()
        Frequency.SPECIFIC_DAYS ->
            if (medication.daysOfWeek.isEmpty()) null
            else medication.times.size * medication.daysOfWeek.size / 7.0
        Frequency.EVERY_X_HOURS -> {
            val hours = medication.intervalHours
            if (hours == null || hours <= 0) null else 24.0 / hours
        }
        Frequency.AS_NEEDED -> null
    }?.takeIf { it > 0.0 }

    /** Whole days the current stock lasts, or null when the schedule gives no daily use. */
    fun daysRemaining(medication: Medication): Int? {
        val perDay = dosesPerDay(medication) ?: return null
        return floor(medication.stockCount.coerceAtLeast(0) / perDay).toInt()
    }

    /** Midnight of the day the stock runs out, counted from [todayMillis]. */
    fun refillDateMillis(medication: Medication, todayMillis: Long): Long? {
        val days = daysRemaining(medication) ?: return null
        return Calendar.getInstance().apply {
            timeInMillis = todayMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, days)
        }.timeInMillis
    }

    fun isLow(medication: Medication): Boolean {
        val days = daysRemaining(medication) ?: return false
        return days <= LOW_STOCK_DAYS
    }

    /** True only on the change that takes the supply into the low range, so the reminder shows once. */
    fun becameLow(before: Medication, after: Medication): Boolean = !isLow(before) && isLow(after)

    /**
     * How a status change moves the stock: taking a dose uses one, and undoing a Taken
     * (for example changing it to Skipped) puts it back. Marking Taken twice uses only one.
     */
    fun stockChange(previous: Dose.Status?, next: Dose.Status): Int = when {
        next == Dose.Status.TAKEN && previous != Dose.Status.TAKEN -> -1
        previous == Dose.Status.TAKEN && next != Dose.Status.TAKEN -> 1
        else -> 0
    }
}

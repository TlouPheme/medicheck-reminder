package com.example.medicheckreminder.ui.history

import com.example.medicheckreminder.domain.model.Dose
import com.example.medicheckreminder.domain.model.DoseLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class HistoryFilter {
    LAST_7,
    LAST_30,
    CUSTOM
}

enum class DayAdherence {
    NONE,
    GOOD,
    MISSED
}

data class CalendarDay(
    val cellIndex: Int,
    val dateKey: String?,
    val dayOfMonth: Int?,
    val isSelected: Boolean,
    val isToday: Boolean,
    val isInFilterRange: Boolean,
    val adherence: DayAdherence
) {
    val isPlaceholder: Boolean get() = dateKey == null
}

data class HistoryUiState(
    val monthTitle: String = "",
    val selectedDateLabel: String = "",
    val selectedDateKey: String = "",
    val filter: HistoryFilter = HistoryFilter.LAST_7,
    val customRangeLabel: String? = null,
    val calendarDays: List<CalendarDay> = emptyList(),
    val selectedLogs: List<DoseLog> = emptyList(),
    val rangeStartMillis: Long = 0L,
    val rangeEndMillis: Long = 0L
)

object HistoryDates {
    private fun dateKeyFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    fun today(): Calendar = startOfDay(Calendar.getInstance())

    fun startOfDay(source: Calendar): Calendar {
        return (source.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun dateKey(calendar: Calendar): String = dateKeyFormat().format(calendar.time)

    fun parseDateKey(key: String): Calendar {
        val parsed = dateKeyFormat().parse(key) ?: Date()
        val calendar = Calendar.getInstance()
        calendar.time = parsed
        return startOfDay(calendar)
    }

    fun addDays(calendar: Calendar, days: Int): Calendar {
        return (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, days) }
    }

    fun startOfMonth(calendar: Calendar): Calendar {
        return startOfDay(calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    }

    fun monthTitle(calendar: Calendar): String {
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    }

    fun selectedDateLabel(calendar: Calendar): String {
        return SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(calendar.time)
    }

    fun rangeLabel(start: Calendar, end: Calendar): String {
        val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
        return "${fmt.format(start.time)} – ${fmt.format(end.time)}"
    }

    fun utcMillisToLocalDay(utcMillis: Long): Calendar {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.timeInMillis = utcMillis
        val local = Calendar.getInstance()
        local.clear()
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
        return startOfDay(local)
    }

    fun localDayToUtcMillis(calendar: Calendar): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.clear()
        utc.set(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        return utc.timeInMillis
    }

    fun isSameDay(left: Calendar, right: Calendar): Boolean {
        return left.get(Calendar.YEAR) == right.get(Calendar.YEAR) &&
            left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR)
    }

    fun isInRange(day: Calendar, start: Calendar, end: Calendar): Boolean {
        val time = startOfDay(day).timeInMillis
        return time in startOfDay(start).timeInMillis..startOfDay(end).timeInMillis
    }
}

internal fun adherenceFor(logs: List<DoseLog>): DayAdherence {
    if (logs.isEmpty()) return DayAdherence.NONE
    if (logs.any { it.status == Dose.Status.SKIPPED || it.status == Dose.Status.MISSED }) {
        return DayAdherence.MISSED
    }
    return if (logs.all { it.status == Dose.Status.TAKEN }) {
        DayAdherence.GOOD
    } else {
        DayAdherence.NONE
    }
}

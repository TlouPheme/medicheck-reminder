package com.example.medicheckreminder.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicheckreminder.MediCheckApp
import com.example.medicheckreminder.domain.model.DoseLog
import com.example.medicheckreminder.domain.model.Medication
import com.example.medicheckreminder.domain.schedule.DoseSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MediCheckApp
    private val medicationRepository = app.container.medicationRepository
    private val doseLogRepository = app.container.doseLogRepository

    private var medications: List<Medication> = emptyList()
    private var recorded: List<DoseLog> = emptyList()

    private var visibleMonth: Calendar = HistoryDates.startOfMonth(HistoryDates.today())
    private var selectedDay: Calendar = HistoryDates.today()
    private var filter: HistoryFilter = HistoryFilter.LAST_7
    private var customStart: Calendar = HistoryDates.addDays(HistoryDates.today(), -6)
    private var customEnd: Calendar = HistoryDates.today()

    private val _uiState = MutableStateFlow(buildState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                medicationRepository.observeAll(),
                doseLogRepository.observeAll()
            ) { meds, logs -> meds to logs }
                .collect { (meds, logs) ->
                    medications = meds
                    recorded = logs
                    publish()
                }
        }
    }

    fun selectDate(dateKey: String) {
        selectedDay = HistoryDates.parseDateKey(dateKey)
        publish()
    }

    fun shiftMonth(delta: Int) {
        visibleMonth = (visibleMonth.clone() as Calendar).apply { add(Calendar.MONTH, delta) }
        publish()
    }

    fun setFilter(newFilter: HistoryFilter) {
        filter = newFilter
        clampSelectionToRange()
        publish()
    }

    fun setCustomRange(startUtcMillis: Long, endUtcMillis: Long) {
        customStart = HistoryDates.utcMillisToLocalDay(startUtcMillis)
        customEnd = HistoryDates.utcMillisToLocalDay(endUtcMillis)
        if (customEnd.before(customStart)) {
            val swap = customStart
            customStart = customEnd
            customEnd = swap
        }
        filter = HistoryFilter.CUSTOM
        visibleMonth = HistoryDates.startOfMonth(customEnd)
        clampSelectionToRange()
        publish()
    }

    fun logsForExport(): List<DoseLog> = periodLogs()

    private fun clampSelectionToRange() {
        val (start, end) = currentRange()
        if (!HistoryDates.isInRange(selectedDay, start, end)) {
            selectedDay = end
            visibleMonth = HistoryDates.startOfMonth(selectedDay)
        }
    }

    private fun currentRange(): Pair<Calendar, Calendar> {
        val today = HistoryDates.today()
        return when (filter) {
            HistoryFilter.LAST_7 -> HistoryDates.addDays(today, -6) to today
            HistoryFilter.LAST_30 -> HistoryDates.addDays(today, -29) to today
            HistoryFilter.CUSTOM -> HistoryDates.startOfDay(customStart) to HistoryDates.startOfDay(customEnd)
        }
    }

    private fun periodLogs(): List<DoseLog> {
        val (start, end) = currentRange()
        val todayKey = HistoryDates.dateKey(HistoryDates.today())
        return DoseSchedule.logsForDays(
            medications = medications,
            recorded = recorded,
            days = daysBetween(start, end),
            todayKey = todayKey
        ).sortedWith(compareByDescending<DoseLog> { it.dateKey }.thenBy { it.scheduledTime })
    }

    private fun daysBetween(start: Calendar, end: Calendar): List<Calendar> {
        val days = mutableListOf<Calendar>()
        var cursor = HistoryDates.startOfDay(start)
        val last = HistoryDates.startOfDay(end)
        while (!cursor.after(last)) {
            days += cursor.clone() as Calendar
            cursor = HistoryDates.addDays(cursor, 1)
        }
        return days
    }

    private fun publish() {
        _uiState.value = buildState()
    }

    private fun buildState(): HistoryUiState {
        val (start, end) = currentRange()
        val selectedKey = HistoryDates.dateKey(selectedDay)
        val period = periodLogs()
        val logsByDay = period.groupBy { it.dateKey }
        return HistoryUiState(
            monthTitle = HistoryDates.monthTitle(visibleMonth),
            selectedDateLabel = HistoryDates.selectedDateLabel(selectedDay),
            selectedDateKey = selectedKey,
            filter = filter,
            customRangeLabel = if (filter == HistoryFilter.CUSTOM) {
                HistoryDates.rangeLabel(start, end)
            } else {
                null
            },
            calendarDays = buildCalendarDays(logsByDay, start, end, selectedKey),
            selectedLogs = period,
            rangeStartMillis = HistoryDates.localDayToUtcMillis(start),
            rangeEndMillis = HistoryDates.localDayToUtcMillis(end)
        )
    }

    private fun buildCalendarDays(
        logsByDay: Map<String, List<DoseLog>>,
        rangeStart: Calendar,
        rangeEnd: Calendar,
        selectedKey: String
    ): List<CalendarDay> {
        val firstOfMonth = HistoryDates.startOfMonth(visibleMonth)
        val firstDayOfWeek = Calendar.SUNDAY
        val leadingEmpty = (firstOfMonth.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
        val daysInMonth = firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val todayKey = HistoryDates.dateKey(HistoryDates.today())

        val cells = mutableListOf<CalendarDay>()
        repeat(leadingEmpty) { cells.add(placeholderDay(cells.size)) }

        for (day in 1..daysInMonth) {
            val cell = (firstOfMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
            val key = HistoryDates.dateKey(cell)
            val inRange = HistoryDates.isInRange(cell, rangeStart, rangeEnd)
            cells.add(
                CalendarDay(
                    cellIndex = cells.size,
                    dateKey = key,
                    dayOfMonth = day,
                    isSelected = key == selectedKey,
                    isToday = key == todayKey,
                    isInFilterRange = inRange,
                    adherence = if (inRange) {
                        adherenceFor(logsByDay[key].orEmpty())
                    } else {
                        DayAdherence.NONE
                    }
                )
            )
        }

        while (cells.size % 7 != 0) {
            cells.add(placeholderDay(cells.size))
        }
        while (cells.size < 42) {
            cells.add(placeholderDay(cells.size))
        }
        return cells
    }

    private fun placeholderDay(index: Int): CalendarDay = CalendarDay(
        cellIndex = index,
        dateKey = null,
        dayOfMonth = null,
        isSelected = false,
        isToday = false,
        isInFilterRange = false,
        adherence = DayAdherence.NONE
    )
}

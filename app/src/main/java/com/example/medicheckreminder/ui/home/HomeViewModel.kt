package com.example.medicheckreminder.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicheckreminder.MediCheckApp
import com.example.medicheckreminder.domain.model.Dose
import com.example.medicheckreminder.domain.schedule.DoseSchedule
import com.example.medicheckreminder.ui.history.HistoryDates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val todayAdherence: Int = 0,
    val doses: List<DoseListItem> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MediCheckApp
    private val medicationRepository = app.container.medicationRepository
    private val doseLogRepository = app.container.doseLogRepository

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                medicationRepository.observeAll(),
                doseLogRepository.observeAll()
            ) { medications, logs ->
                val today = HistoryDates.today()
                val doses = DoseSchedule.dosesForDay(
                    medications = medications,
                    recorded = logs,
                    day = today,
                    todayKey = HistoryDates.dateKey(today)
                )
                HomeUiState(
                    todayAdherence = calculateAdherence(doses),
                    doses = groupByTime(doses),
                    isLoading = false,
                    isOffline = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun sync() {
        _uiState.value = _uiState.value.copy(isLoading = false, isOffline = false)
    }

    fun markAsTaken(dose: Dose) = record(dose, Dose.Status.TAKEN)

    fun markAsSkipped(dose: Dose) = record(dose, Dose.Status.SKIPPED)

    fun snooze(dose: Dose) = record(dose, Dose.Status.SNOOZED)

    private fun record(dose: Dose, status: Dose.Status) {
        viewModelScope.launch {
            doseLogRepository.record(dose.copy(status = status, dateKey = todayKey()))
        }
    }

    private fun todayKey(): String = HistoryDates.dateKey(Calendar.getInstance())

    private fun calculateAdherence(todayDoses: List<Dose>): Int {
        if (todayDoses.isEmpty()) return 0
        val taken = todayDoses.count { it.status == Dose.Status.TAKEN }
        return (taken * 100) / todayDoses.size
    }

    private fun groupByTime(todayDoses: List<Dose>): List<DoseListItem> {
        return todayDoses
            .sortedBy { it.scheduledTime }
            .groupBy { it.scheduledTime }
            .flatMap { (time, grouped) ->
                listOf(DoseListItem.Header(time)) + grouped.map { DoseListItem.DoseItem(it) }
            }
    }
}

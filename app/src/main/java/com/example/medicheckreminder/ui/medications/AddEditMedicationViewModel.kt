package com.example.medicheckreminder.ui.medications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicheckreminder.MediCheckApp
import com.example.medicheckreminder.domain.model.Frequency
import com.example.medicheckreminder.domain.model.Medication
import com.example.medicheckreminder.util.MedicationDraft
import com.example.medicheckreminder.util.MedicationValidator
import com.example.medicheckreminder.worker.SyncScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AddEditUiState(
    val isEditMode: Boolean = false,
    val isLoaded: Boolean = false,
    val name: String = "",
    val dosage: String = "",
    val frequency: Frequency = Frequency.DAILY,
    val times: List<String> = emptyList(),
    val daysOfWeek: Set<Int> = emptySet(),
    val intervalHours: String = "",
    val stockCount: String = "",
    val notes: String = "",
    val errors: MedicationValidator.Result = MedicationValidator.Result(),
    val isSaving: Boolean = false
)

sealed class AddEditEvent {
    data object Saved : AddEditEvent()
    data object Deleted : AddEditEvent()
}

class AddEditMedicationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MediCheckApp).container.medicationRepository

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddEditEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AddEditEvent> = _events.asSharedFlow()

    private var medicationId: String? = null
    private var createdAt: Long = 0L
    private var initialized = false

    fun load(medicationId: String?) {
        if (initialized) return
        initialized = true
        this.medicationId = medicationId

        if (medicationId.isNullOrBlank()) {
            _uiState.value = AddEditUiState(isEditMode = false, isLoaded = true)
            return
        }

        viewModelScope.launch {
            val existing = repository.getById(medicationId)
            if (existing == null) {
                this@AddEditMedicationViewModel.medicationId = null
                _uiState.value = AddEditUiState(isEditMode = false, isLoaded = true)
                return@launch
            }
            createdAt = existing.createdAt
            _uiState.value = AddEditUiState(
                isEditMode = true,
                isLoaded = true,
                name = existing.name,
                dosage = existing.dosage,
                frequency = existing.frequency,
                times = existing.times,
                daysOfWeek = existing.daysOfWeek,
                intervalHours = existing.intervalHours?.toString().orEmpty(),
                stockCount = existing.stockCount.takeIf { it > 0 }?.toString().orEmpty(),
                notes = existing.notes
            )
        }
    }

    fun setFrequency(frequency: Frequency) {
        _uiState.update { it.copy(frequency = frequency, errors = it.errors.copy(
            timesError = null,
            daysError = null,
            intervalError = null
        )) }
    }

    fun addTime(time: String) {
        _uiState.update { state ->
            if (time in state.times) state
            else state.copy(
                times = (state.times + time).sorted(),
                errors = state.errors.copy(timesError = null)
            )
        }
    }

    fun removeTime(time: String) {
        _uiState.update { it.copy(times = it.times.filterNot { existing -> existing == time }) }
    }

    fun setDay(day: Int, selected: Boolean) {
        _uiState.update { state ->
            val days = if (selected) state.daysOfWeek + day else state.daysOfWeek - day
            state.copy(daysOfWeek = days, errors = state.errors.copy(daysError = null))
        }
    }

    fun save(draft: MedicationDraft) {
        if (_uiState.value.isSaving) return
        val merged = draft.copy(
            times = _uiState.value.times,
            daysOfWeek = _uiState.value.daysOfWeek,
            frequency = _uiState.value.frequency
        )
        val errors = MedicationValidator.validate(merged)
        if (!errors.isValid) {
            _uiState.update { it.copy(errors = errors) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errors = MedicationValidator.Result()) }
            val id = medicationId ?: UUID.randomUUID().toString()
            repository.upsert(
                Medication(
                    id = id,
                    name = merged.name.trim(),
                    dosage = merged.dosage.trim(),
                    stockCount = MedicationValidator.parseStock(merged.stockCount),
                    frequency = merged.frequency,
                    times = merged.times,
                    daysOfWeek = merged.daysOfWeek,
                    intervalHours = if (merged.frequency == Frequency.EVERY_X_HOURS) {
                        MedicationValidator.parseIntervalHours(merged.intervalHours)
                    } else {
                        null
                    },
                    notes = merged.notes.trim(),
                    createdAt = createdAt
                )
            )
            SyncScheduler.enqueue(getApplication())
            medicationId = id
            _uiState.update { it.copy(isSaving = false, isEditMode = true) }
            _events.emit(AddEditEvent.Saved)
        }
    }

    fun delete() {
        val id = medicationId ?: return
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.delete(id)
            SyncScheduler.enqueue(getApplication())
            _events.emit(AddEditEvent.Deleted)
        }
    }
}

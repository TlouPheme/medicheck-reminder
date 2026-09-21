package com.example.medicheckreminder.ui.medications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicheckreminder.MediCheckApp
import com.example.medicheckreminder.domain.model.Medication
import com.example.medicheckreminder.worker.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MedicationsUiState(
    val medications: List<Medication> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class MedicationsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MediCheckApp).container.medicationRepository
    private val query = MutableStateFlow("")
    private val loadError = MutableStateFlow<String?>(null)
    private val retryTick = MutableStateFlow(0)

    val uiState: StateFlow<MedicationsUiState> = retryTick.flatMapLatest {
        combine(
            repository.observeAll()
                .onEach { loadError.value = null }
                .catch { error ->
                    loadError.value = error.message.orEmpty()
                    emit(emptyList())
                },
            query,
            loadError
        ) { medications, needle, error ->
            val filtered = if (needle.isBlank()) {
                medications
            } else {
                medications.filter { it.name.contains(needle, ignoreCase = true) }
            }
            MedicationsUiState(
                medications = filtered,
                query = needle,
                isLoading = false,
                errorMessage = error
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MedicationsUiState(isLoading = true)
    )

    fun setQuery(value: String) {
        query.value = value
    }

    fun retry() {
        loadError.value = null
        retryTick.value = retryTick.value + 1
    }

    fun delete(medication: Medication) {
        viewModelScope.launch {
            repository.delete(medication.id)
            SyncScheduler.enqueue(getApplication())
        }
    }
}

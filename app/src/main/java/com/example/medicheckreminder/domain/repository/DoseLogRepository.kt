package com.example.medicheckreminder.domain.repository

import com.example.medicheckreminder.domain.model.Dose
import com.example.medicheckreminder.domain.model.DoseLog
import kotlinx.coroutines.flow.Flow

interface DoseLogRepository {
    fun observeAll(): Flow<List<DoseLog>>
    suspend fun record(dose: Dose)
}

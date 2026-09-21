package com.example.medicheckreminder.domain.repository

import com.example.medicheckreminder.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    fun snapshot(): AppSettings
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

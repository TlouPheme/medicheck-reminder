package com.example.medicheckreminder

import android.app.Application
import com.example.medicheckreminder.di.AppContainer
import com.example.medicheckreminder.reminder.DoseAlarmScheduler
import com.example.medicheckreminder.util.SettingsApplier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MediCheckApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        val settings = container.settingsRepository.snapshot()
        SettingsApplier.applyTheme(settings.theme)
        SettingsApplier.applyLanguage(settings.language)
        appScope.launch {
            combine(
                container.medicationRepository.observeAll(),
                container.doseLogRepository.observeAll(),
                container.settingsRepository.observe()
            ) { medications, logs, currentSettings ->
                Triple(medications, logs, currentSettings)
            }.collect { (medications, logs, currentSettings) ->
                DoseAlarmScheduler.reschedule(
                    this@MediCheckApp,
                    medications,
                    logs,
                    currentSettings
                )
            }
        }
    }
}

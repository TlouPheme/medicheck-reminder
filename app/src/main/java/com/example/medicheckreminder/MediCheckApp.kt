package com.example.medicheckreminder

import android.app.Application
import com.example.medicheckreminder.di.AppContainer
import com.example.medicheckreminder.util.SettingsApplier

class MediCheckApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        val settings = container.settingsRepository.snapshot()
        SettingsApplier.applyTheme(settings.theme)
        SettingsApplier.applyLanguage(settings.language)
    }
}

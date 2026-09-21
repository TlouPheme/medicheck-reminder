package com.example.medicheckreminder.ui.settings

import android.app.Application
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.text.InputType
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicheckreminder.MediCheckApp
import com.example.medicheckreminder.R
import com.example.medicheckreminder.domain.model.AppLanguage
import com.example.medicheckreminder.domain.model.AppSettings
import com.example.medicheckreminder.domain.model.AppTheme
import com.example.medicheckreminder.util.PasswordValidator
import com.example.medicheckreminder.util.SettingsApplier
import com.example.medicheckreminder.util.SouthAfricanPhoneValidator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SettingsEvent {
    data object LoggedOut : SettingsEvent()
    data class OpenUrl(val url: String) : SettingsEvent()
    data class PickSound(val currentUri: String) : SettingsEvent()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MediCheckApp).container.settingsRepository

    private val _rows = MutableStateFlow<List<SettingRow>>(emptyList())
    val rows: StateFlow<List<SettingRow>> = _rows.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.observe().collect { settings ->
                _rows.value = buildRows(settings)
            }
        }
    }

    fun onLanguageSelected(index: Int) {
        val language = AppLanguage.entries.getOrNull(index) ?: return
        viewModelScope.launch {
            repository.update { it.copy(language = language) }
            SettingsApplier.applyLanguage(language)
        }
    }

    fun onThemeSelected(index: Int) {
        val theme = AppTheme.entries.getOrNull(index) ?: return
        viewModelScope.launch {
            repository.update { it.copy(theme = theme) }
            SettingsApplier.applyTheme(theme)
        }
    }

    fun onNotificationsToggled(enabled: Boolean) {
        update { it.copy(notificationsEnabled = enabled) }
    }

    fun onVibrationToggled(enabled: Boolean) {
        update { it.copy(vibrationEnabled = enabled) }
    }

    fun onEscalationToggled(enabled: Boolean) {
        update { it.copy(escalationEnabled = enabled) }
    }

    fun onSnoozeSelected(index: Int) {
        val minutes = SNOOZE_OPTIONS.getOrNull(index) ?: return
        update { it.copy(snoozeMinutes = minutes) }
    }

    fun onCaregiverNameChanged(value: String) {
        update { it.copy(caregiverName = value) }
    }

    fun onCaregiverPhoneChanged(value: String) {
        update { it.copy(caregiverPhone = value) }
    }

    fun onCaregiverEmailChanged(value: String) {
        update { it.copy(caregiverEmail = value) }
    }

    fun setSoundUri(uri: String?) {
        val defaultUri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.toString()
        val stored = when {
            uri == null -> SettingIds.SOUND_SILENT
            uri == defaultUri -> ""
            else -> uri
        }
        update { it.copy(notificationSoundUri = stored) }
    }

    fun onRowClicked(id: String) {
        when (id) {
            SettingIds.SOUND -> {
                val current = repository.snapshot().notificationSoundUri
                _events.tryEmit(SettingsEvent.PickSound(current))
            }
            SettingIds.PRIVACY -> _events.tryEmit(SettingsEvent.OpenUrl(SettingIds.PRIVACY_URL))
        }
    }

    fun logout() {
        (getApplication<Application>() as MediCheckApp).container.accountStore.signOut()
        _events.tryEmit(SettingsEvent.LoggedOut)
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repository.update(transform) }
    }

    private fun buildRows(settings: AppSettings): List<SettingRow> {
        val app = getApplication<Application>()
        val notificationsOn = settings.notificationsEnabled
        val languageLabels = listOf(
            app.getString(R.string.settings_language_english),
            app.getString(R.string.settings_language_isizulu),
            app.getString(R.string.settings_language_afrikaans)
        )
        val themeLabels = listOf(
            app.getString(R.string.settings_theme_light),
            app.getString(R.string.settings_theme_dark),
            app.getString(R.string.settings_theme_system)
        )
        val snoozeLabels = SNOOZE_OPTIONS.map { minutes ->
            app.getString(R.string.settings_snooze_minutes, minutes)
        }
        val snoozeIndex = SNOOZE_OPTIONS.indexOf(settings.snoozeMinutes).coerceAtLeast(0)
        val languageIndex = AppLanguage.entries.indexOf(settings.language).coerceAtLeast(0)
        val themeIndex = AppTheme.entries.indexOf(settings.theme).coerceAtLeast(0)
        val phoneError = if (SouthAfricanPhoneValidator.isValidOrBlank(settings.caregiverPhone)) {
            null
        } else {
            app.getString(R.string.error_caregiver_phone)
        }
        val emailError = when {
            settings.caregiverEmail.isBlank() -> null
            PasswordValidator.validateEmail(settings.caregiverEmail) -> null
            else -> app.getString(R.string.error_invalid_email)
        }

        return listOf(
            SettingRow.Dropdown(
                id = SettingIds.LANGUAGE,
                title = app.getString(R.string.settings_language),
                valueLabel = languageLabels[languageIndex],
                options = languageLabels,
                selectedIndex = languageIndex
            ),
            SettingRow.Dropdown(
                id = SettingIds.THEME,
                title = app.getString(R.string.settings_theme),
                valueLabel = themeLabels[themeIndex],
                options = themeLabels,
                selectedIndex = themeIndex
            ),
            SettingRow.Header(
                id = SettingIds.SECTION_NOTIFICATIONS,
                title = app.getString(R.string.settings_section_notifications)
            ),
            SettingRow.Toggle(
                id = SettingIds.NOTIFICATIONS,
                title = app.getString(R.string.settings_notifications),
                subtitle = app.getString(R.string.settings_notifications_subtitle),
                checked = settings.notificationsEnabled
            ),
            SettingRow.Navigation(
                id = SettingIds.SOUND,
                title = app.getString(R.string.settings_sound),
                valueLabel = soundLabel(settings.notificationSoundUri),
                enabled = notificationsOn
            ),
            SettingRow.Toggle(
                id = SettingIds.VIBRATION,
                title = app.getString(R.string.settings_vibration),
                checked = settings.vibrationEnabled,
                enabled = notificationsOn
            ),
            SettingRow.Dropdown(
                id = SettingIds.SNOOZE,
                title = app.getString(R.string.settings_snooze),
                valueLabel = snoozeLabels[snoozeIndex],
                options = snoozeLabels,
                selectedIndex = snoozeIndex,
                enabled = notificationsOn
            ),
            SettingRow.Toggle(
                id = SettingIds.ESCALATION,
                title = app.getString(R.string.settings_escalation),
                subtitle = app.getString(R.string.settings_escalation_subtitle),
                checked = settings.escalationEnabled,
                enabled = notificationsOn
            ),
            SettingRow.Header(
                id = SettingIds.SECTION_CAREGIVER,
                title = app.getString(R.string.settings_section_caregiver)
            ),
            SettingRow.TextField(
                id = SettingIds.CAREGIVER_NAME,
                title = app.getString(R.string.settings_caregiver_name),
                value = settings.caregiverName,
                hint = app.getString(R.string.settings_caregiver_name),
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PERSON_NAME or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS
            ),
            SettingRow.TextField(
                id = SettingIds.CAREGIVER_PHONE,
                title = app.getString(R.string.settings_caregiver_phone),
                value = settings.caregiverPhone,
                hint = app.getString(R.string.settings_caregiver_phone_hint),
                inputType = InputType.TYPE_CLASS_PHONE,
                error = phoneError
            ),
            SettingRow.TextField(
                id = SettingIds.CAREGIVER_EMAIL,
                title = app.getString(R.string.settings_caregiver_email),
                value = settings.caregiverEmail,
                hint = app.getString(R.string.settings_caregiver_email),
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                error = emailError
            ),
            SettingRow.Header(
                id = SettingIds.SECTION_ABOUT,
                title = app.getString(R.string.settings_section_about)
            ),
            SettingRow.Info(
                id = SettingIds.VERSION,
                title = app.getString(R.string.settings_version),
                value = appVersion()
            ),
            SettingRow.Info(
                id = SettingIds.ICONS,
                title = app.getString(R.string.settings_icons_credit),
                value = ""
            ),
            SettingRow.Navigation(
                id = SettingIds.PRIVACY,
                title = app.getString(R.string.settings_privacy_policy),
                valueLabel = ""
            ),
            SettingRow.DangerButton(
                id = SettingIds.LOGOUT,
                title = app.getString(R.string.settings_logout)
            )
        )
    }

    private fun soundLabel(uriString: String): String {
        val app = getApplication<Application>()
        return when {
            uriString.isBlank() -> app.getString(R.string.settings_sound_default)
            uriString == SettingIds.SOUND_SILENT -> app.getString(R.string.settings_sound_silent)
            else -> runCatching {
                RingtoneManager.getRingtone(app, uriString.toUri())?.getTitle(app)
            }.getOrNull()?.takeIf { it.isNotBlank() }
                ?: app.getString(R.string.settings_sound_custom)
        }
    }

    private fun appVersion(): String {
        val app = getApplication<Application>()
        return runCatching {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                app.packageManager.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                app.packageManager.getPackageInfo(app.packageName, 0)
            }
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            app.getString(R.string.settings_version_value, info.versionName.orEmpty(), code)
        }.getOrDefault(app.getString(R.string.settings_version_unknown))
    }

    companion object {
        val SNOOZE_OPTIONS = listOf(5, 10, 15, 30, 60)
    }
}

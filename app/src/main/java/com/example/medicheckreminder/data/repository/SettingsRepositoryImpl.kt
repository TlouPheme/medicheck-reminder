package com.example.medicheckreminder.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.medicheckreminder.domain.model.AppLanguage
import com.example.medicheckreminder.domain.model.AppSettings
import com.example.medicheckreminder.domain.model.AppTheme
import com.example.medicheckreminder.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SettingsRepositoryImpl(
    context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences = createPrefs(context.applicationContext)
    private val writeLock = Mutex()

    override fun observe(): Flow<AppSettings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(snapshot())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(snapshot())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    override fun snapshot(): AppSettings = AppSettings(
        language = AppLanguage.entries.find { it.code == prefs.getString(KEY_LANGUAGE, "en") }
            ?: AppLanguage.ENGLISH,
        theme = runCatching {
            AppTheme.valueOf(prefs.getString(KEY_THEME, AppTheme.SYSTEM.name)!!)
        }.getOrDefault(AppTheme.SYSTEM),
        notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true),
        notificationSoundUri = prefs.getString(KEY_SOUND, "").orEmpty(),
        vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true),
        snoozeMinutes = prefs.getInt(KEY_SNOOZE, 10),
        escalationEnabled = prefs.getBoolean(KEY_ESCALATION, false),
        caregiverName = prefs.getString(KEY_CAREGIVER_NAME, "").orEmpty(),
        caregiverPhone = prefs.getString(KEY_CAREGIVER_PHONE, "").orEmpty(),
        caregiverEmail = prefs.getString(KEY_CAREGIVER_EMAIL, "").orEmpty()
    )

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        withContext(Dispatchers.IO) {
            writeLock.withLock {
                write(transform(snapshot()))
            }
        }
    }

    private fun write(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_LANGUAGE, settings.language.code)
            .putString(KEY_THEME, settings.theme.name)
            .putBoolean(KEY_NOTIFICATIONS, settings.notificationsEnabled)
            .putString(KEY_SOUND, settings.notificationSoundUri)
            .putBoolean(KEY_VIBRATION, settings.vibrationEnabled)
            .putInt(KEY_SNOOZE, settings.snoozeMinutes)
            .putBoolean(KEY_ESCALATION, settings.escalationEnabled)
            .putString(KEY_CAREGIVER_NAME, settings.caregiverName)
            .putString(KEY_CAREGIVER_PHONE, settings.caregiverPhone)
            .putString(KEY_CAREGIVER_EMAIL, settings.caregiverEmail)
            .apply()
    }

    private fun createPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    private companion object {
        const val FILE_NAME = "medicheck_settings"
        const val KEY_LANGUAGE = "language"
        const val KEY_THEME = "theme"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_SOUND = "notification_sound"
        const val KEY_VIBRATION = "vibration_enabled"
        const val KEY_SNOOZE = "snooze_minutes"
        const val KEY_ESCALATION = "escalation_enabled"
        const val KEY_CAREGIVER_NAME = "caregiver_name"
        const val KEY_CAREGIVER_PHONE = "caregiver_phone"
        const val KEY_CAREGIVER_EMAIL = "caregiver_email"
    }
}

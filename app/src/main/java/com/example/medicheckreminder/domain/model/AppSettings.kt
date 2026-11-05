package com.example.medicheckreminder.domain.model

enum class AppLanguage(val code: String) {
    ENGLISH("en"),
    ISIZULU("zu"),
    AFRIKAANS("af")
}

enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM
}

data class AppSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val theme: AppTheme = AppTheme.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val notificationSoundUri: String = "",
    val vibrationEnabled: Boolean = true,
    val snoozeMinutes: Int = 10,
    val escalationEnabled: Boolean = false,
    /** Firebase Cloud Messaging pushes (updates and alerts sent to this phone). */
    val pushEnabled: Boolean = true,
    /** Unanswered dose reminders become a full-screen alarm after 10 minutes. */
    val fullScreenAlarmEnabled: Boolean = true,
    val caregiverName: String = "",
    val caregiverPhone: String = "",
    val caregiverEmail: String = ""
)

package com.example.medicheckreminder.ui.settings

sealed class SettingRow {
    abstract val id: String

    data class Header(
        override val id: String,
        val title: String
    ) : SettingRow()

    data class Dropdown(
        override val id: String,
        val title: String,
        val valueLabel: String,
        val options: List<String>,
        val selectedIndex: Int,
        val enabled: Boolean = true
    ) : SettingRow()

    data class Toggle(
        override val id: String,
        val title: String,
        val subtitle: String? = null,
        val checked: Boolean,
        val enabled: Boolean = true
    ) : SettingRow()

    data class Navigation(
        override val id: String,
        val title: String,
        val valueLabel: String,
        val enabled: Boolean = true
    ) : SettingRow()

    data class TextField(
        override val id: String,
        val title: String,
        val value: String,
        val hint: String,
        val inputType: Int,
        val error: String? = null
    ) : SettingRow()

    data class Info(
        override val id: String,
        val title: String,
        val value: String
    ) : SettingRow()

    data class DangerButton(
        override val id: String,
        val title: String
    ) : SettingRow()
}

object SettingIds {
    const val LANGUAGE = "language"
    const val THEME = "theme"
    const val SECTION_NOTIFICATIONS = "section_notifications"
    const val NOTIFICATIONS = "notifications"
    const val SOUND = "sound"
    const val VIBRATION = "vibration"
    const val SNOOZE = "snooze"
    const val ESCALATION = "escalation"
    const val SECTION_CAREGIVER = "section_caregiver"
    const val CAREGIVER_NAME = "caregiver_name"
    const val CAREGIVER_PHONE = "caregiver_phone"
    const val CAREGIVER_EMAIL = "caregiver_email"
    const val SECTION_ABOUT = "section_about"
    const val ICONS = "icons"
    const val VERSION = "version"
    const val PRIVACY = "privacy"
    const val LOGOUT = "logout"

    const val SOUND_SILENT = "silent"
    const val PRIVACY_URL = "https://www.medicheck.app/privacy"
}

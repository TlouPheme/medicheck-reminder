package com.example.medicheckreminder.push

import com.example.medicheckreminder.R

/** What a push message says, read from its notification part or, failing that, its data part. */
data class PushMessage(val title: String, val body: String, val screen: String?)

/**
 * Maps the `screen` key of a push message (or of a notification the app posts itself) to the
 * screen that opens when the notification is tapped, e.g. `screen = medications`.
 */
object PushRoutes {

    const val EXTRA_SCREEN = "screen"
    const val HOME = "home"
    const val MEDICATIONS = "medications"
    const val HISTORY = "history"
    const val HEALTH = "health"
    const val SETTINGS = "settings"

    fun destinationFor(screen: String?): Int? = when (screen?.trim()?.lowercase()) {
        HOME -> R.id.homeFragment
        MEDICATIONS -> R.id.medicationsListFragment
        HISTORY -> R.id.historyFragment
        HEALTH -> R.id.healthMeasurementsFragment
        SETTINGS -> R.id.settingsFragment
        else -> null
    }

    fun parse(notificationTitle: String?, notificationBody: String?, data: Map<String, String>): PushMessage? {
        val title = notificationTitle?.takeIf { it.isNotBlank() } ?: data["title"].orEmpty()
        val body = notificationBody?.takeIf { it.isNotBlank() } ?: data["body"].orEmpty()
        if (title.isBlank() && body.isBlank()) return null
        return PushMessage(title, body, data[EXTRA_SCREEN]?.takeIf { it.isNotBlank() })
    }
}

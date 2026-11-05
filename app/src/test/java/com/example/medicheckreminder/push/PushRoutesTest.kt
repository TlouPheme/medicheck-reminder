package com.example.medicheckreminder.push

import com.example.medicheckreminder.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushRoutesTest {

    @Test
    fun screenKey_opensTheMatchingTab() {
        assertEquals(R.id.homeFragment, PushRoutes.destinationFor("home"))
        assertEquals(R.id.medicationsListFragment, PushRoutes.destinationFor("medications"))
        assertEquals(R.id.historyFragment, PushRoutes.destinationFor(" History "))
        assertEquals(R.id.healthMeasurementsFragment, PushRoutes.destinationFor("HEALTH"))
        assertEquals(R.id.settingsFragment, PushRoutes.destinationFor("settings"))
    }

    @Test
    fun unknownOrMissingScreen_opensNothingSpecial() {
        assertNull(PushRoutes.destinationFor(null))
        assertNull(PushRoutes.destinationFor("admin"))
    }

    @Test
    fun notificationPart_winsOverData() {
        val push = PushRoutes.parse("Refill soon", "3 days left", mapOf("title" to "x", "screen" to "medications"))
        assertEquals(PushMessage("Refill soon", "3 days left", "medications"), push)
    }

    @Test
    fun dataOnlyMessage_isStillShown() {
        val push = PushRoutes.parse(null, null, mapOf("title" to "Dose missed", "body" to "Metformin 08:00"))
        assertEquals(PushMessage("Dose missed", "Metformin 08:00", null), push)
    }

    @Test
    fun emptyMessage_isIgnored() {
        assertNull(PushRoutes.parse(" ", null, mapOf("screen" to "home")))
    }
}

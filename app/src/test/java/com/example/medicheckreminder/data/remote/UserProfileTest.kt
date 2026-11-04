package com.example.medicheckreminder.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UserProfileTest {

    private fun profile(
        email: String = "Thandi@Example.com ",
        displayName: String = "Thandi Mokoena",
        providers: List<String> = listOf("firebase", "password"),
        createdAtMillis: Long? = 1_700_000_000_000L
    ) = UserProfile("uid-1", email, displayName, providers, createdAtMillis)

    @Test
    fun email_isNormalised_soBothSignInMethodsMatch() {
        assertEquals("thandi@example.com", profile().toFirestoreMap()[UserProfile.FIELD_EMAIL])
    }

    @Test
    fun providers_dropFirebaseAndDuplicates_andAreSorted() {
        val map = profile(providers = listOf("password", "firebase", "google.com", "password")).toFirestoreMap()
        assertEquals(listOf("google.com", "password"), map[UserProfile.FIELD_PROVIDERS])
    }

    @Test
    fun blankName_isNotWritten_soMergeKeepsTheExistingName() {
        val map = profile(displayName = "  ").toFirestoreMap()
        assertFalse(map.containsKey(UserProfile.FIELD_DISPLAY_NAME))
    }

    @Test
    fun missingCreationTime_isNotWritten() {
        assertFalse(profile(createdAtMillis = null).toFirestoreMap().containsKey(UserProfile.FIELD_CREATED_AT))
        assertFalse(profile(createdAtMillis = 0L).toFirestoreMap().containsKey(UserProfile.FIELD_CREATED_AT))
    }

    @Test
    fun uid_isStoredInTheDocument() {
        assertEquals("uid-1", profile().toFirestoreMap()[UserProfile.FIELD_UID])
    }
}

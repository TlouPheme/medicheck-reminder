package com.example.medicheckreminder.data.remote

/**
 * The shared user record kept in Firestore at `users/{uid}`.
 *
 * Email/password sign-ups and Google sign-ins both end up as one Firebase Auth account per
 * email address, so both write to the same document. [providers] records which sign-in
 * methods are linked to that account (for example `password` and `google.com`).
 */
data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val providers: List<String>,
    val createdAtMillis: Long?
) {

    /** Fields written with a merge, so a later sign-in never wipes earlier data. */
    fun toFirestoreMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            FIELD_UID to uid,
            FIELD_EMAIL to email.trim().lowercase(),
            FIELD_PROVIDERS to providers.filter { it.isNotBlank() && it != FIREBASE_PROVIDER }.distinct().sorted()
        )
        if (displayName.isNotBlank()) map[FIELD_DISPLAY_NAME] = displayName.trim()
        if (createdAtMillis != null && createdAtMillis > 0L) map[FIELD_CREATED_AT] = createdAtMillis
        return map
    }

    companion object {
        const val COLLECTION = "users"
        const val FIELD_UID = "uid"
        const val FIELD_EMAIL = "email"
        const val FIELD_DISPLAY_NAME = "displayName"
        const val FIELD_PROVIDERS = "providers"
        const val FIELD_CREATED_AT = "createdAtMillis"
        const val FIELD_LAST_SIGN_IN = "lastSignInAt"

        /** Firebase lists itself as a provider on every user; it says nothing about sign-in method. */
        private const val FIREBASE_PROVIDER = "firebase"
    }
}

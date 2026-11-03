package com.example.medicheckreminder.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Thin wrapper around Firebase Authentication and the Firestore `users` collection.
 *
 * Firebase is optional at build time: without `app/google-services.json` no FirebaseApp exists,
 * [isAvailable] is false, and every call is skipped so the local account keeps working.
 *
 * Adapted from the Firebase docs:
 * - Email/password: https://firebase.google.com/docs/auth/android/password-auth
 * - Google credential: https://firebase.google.com/docs/auth/android/google-signin
 * - Merge writes: https://firebase.google.com/docs/firestore/manage-data/add-data#set_a_document
 */
class FirebaseAuthGateway(context: Context) {

    private val appContext = context.applicationContext

    val isAvailable: Boolean
        get() = FirebaseApp.getApps(appContext).isNotEmpty()

    private val auth: FirebaseAuth? get() = if (isAvailable) FirebaseAuth.getInstance() else null
    private val firestore: FirebaseFirestore? get() = if (isAvailable) FirebaseFirestore.getInstance() else null

    val currentUser: FirebaseUser? get() = auth?.currentUser

    /** Creates the Firebase account for a new email sign-up, or signs in if the email already has one. */
    suspend fun registerWithEmail(email: String, password: String, name: String): FirebaseUser? {
        val auth = auth ?: return null
        return try {
            val user = withTimeout(TIMEOUT_MS) {
                auth.createUserWithEmailAndPassword(email, password).await().user
            }
            Log.d(TAG, "Created Firebase account uid=${user?.uid}")
            user?.let { saveProfile(it, name) }
            user
        } catch (_: FirebaseAuthUserCollisionException) {
            // The email already has a Firebase account (for example from Google sign-in on
            // another phone). Signing in links this device to that same account and record.
            Log.d(TAG, "Email already registered in Firebase, signing in instead")
            signInWithEmail(email, password, name)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase sign-up failed; the local account is still created", e)
            null
        }
    }

    suspend fun signInWithEmail(email: String, password: String, name: String = ""): FirebaseUser? {
        val auth = auth ?: return null
        return try {
            val user = withTimeout(TIMEOUT_MS) {
                auth.signInWithEmailAndPassword(email, password).await().user
            }
            Log.d(TAG, "Firebase email sign-in uid=${user?.uid}")
            user?.let { saveProfile(it, name) }
            user
        } catch (e: Exception) {
            Log.e(TAG, "Firebase email sign-in failed", e)
            null
        }
    }

    /**
     * Exchanges a Google ID token for a Firebase session. With Firebase's default
     * "one account per email address" setting, a Google account whose email already signed up
     * with a password resolves to the same uid, so both share one `users/{uid}` record.
     *
     * Throws so the caller can tell the person why sign-in failed.
     */
    suspend fun signInWithGoogle(idToken: String): FirebaseUser {
        val auth = auth ?: error("Firebase is not configured")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = withTimeout(TIMEOUT_MS) {
            auth.signInWithCredential(credential).await().user
        } ?: error("Firebase returned no user")
        Log.d(TAG, "Firebase Google sign-in uid=${user.uid}")
        saveProfile(user, user.displayName.orEmpty())
        return user
    }

    fun signOut() {
        auth?.signOut()
        Log.d(TAG, "Signed out of Firebase")
    }

    /**
     * Writes the user record. Not awaited: Firestore queues the write while offline and sends it
     * when the phone reconnects, so sign-in never waits on the network for this.
     */
    private fun saveProfile(user: FirebaseUser, name: String) {
        val firestore = firestore ?: return
        val profile = UserProfile(
            uid = user.uid,
            email = user.email.orEmpty(),
            displayName = name.ifBlank { user.displayName.orEmpty() },
            providers = user.providerData.map { it.providerId },
            createdAtMillis = user.metadata?.creationTimestamp
        )
        val data = profile.toFirestoreMap() + (UserProfile.FIELD_LAST_SIGN_IN to FieldValue.serverTimestamp())
        firestore.collection(UserProfile.COLLECTION)
            .document(user.uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener { Log.d(TAG, "Saved user record users/${user.uid}") }
            .addOnFailureListener { e -> Log.e(TAG, "Could not save user record", e) }
    }

    private companion object {
        const val TAG = "FirebaseAuthGateway"
        const val TIMEOUT_MS = 15_000L
    }
}

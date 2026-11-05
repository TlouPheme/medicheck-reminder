package com.example.medicheckreminder.push

import android.content.Context
import android.util.Log
import com.example.medicheckreminder.MediCheckApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Keeps this phone's FCM token on the signed-in user's record (`users/{uid}.fcmTokens`), so a push
 * can be addressed to every phone the person uses. Turning push off in Settings removes the token.
 *
 * https://firebase.google.com/docs/cloud-messaging/manage-tokens
 */
object PushTokenRegistrar {

    private const val TAG = "PushTokens"
    private const val USERS = "users"
    const val FIELD_TOKENS = "fcmTokens"
    private const val FIELD_TOKEN_UPDATED = "fcmTokenUpdatedAt"

    private fun firebaseReady(context: Context): Boolean =
        (context.applicationContext as MediCheckApp).container.firebaseAuthGateway.isAvailable

    private fun pushEnabled(context: Context): Boolean =
        (context.applicationContext as MediCheckApp).container.settingsRepository.snapshot().pushEnabled

    /** Fetches the current token and stores it. Called after sign-in and when push is switched on. */
    suspend fun register(context: Context) {
        if (!firebaseReady(context) || !pushEnabled(context)) return
        try {
            FirebaseMessaging.getInstance().isAutoInitEnabled = true
            val token = FirebaseMessaging.getInstance().token.await()
            save(context, token)
        } catch (e: Exception) {
            Log.e(TAG, "Could not get the FCM token", e)
        }
    }

    /** Called from [MediCheckMessagingService.onNewToken] when FCM rotates the token. */
    fun save(context: Context, token: String) {
        if (!firebaseReady(context) || !pushEnabled(context)) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.d(TAG, "Token received before sign-in; it is saved after the next sign-in")
            return
        }
        FirebaseFirestore.getInstance().collection(USERS).document(uid)
            .set(
                mapOf(
                    FIELD_TOKENS to FieldValue.arrayUnion(token),
                    FIELD_TOKEN_UPDATED to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener { Log.d(TAG, "Saved FCM token on users/$uid: $token") }
            .addOnFailureListener { e -> Log.e(TAG, "Could not save the FCM token", e) }
    }

    /**
     * Removes this phone's token from the record and from FCM, so no more pushes arrive. Time-limited,
     * so signing out offline is never held up: Firestore queues the removal until the phone reconnects.
     */
    suspend fun unregister(context: Context) {
        if (!firebaseReady(context)) return
        val messaging = FirebaseMessaging.getInstance()
        messaging.isAutoInitEnabled = false
        try {
            withTimeoutOrNull(UNREGISTER_TIMEOUT_MS) {
                val token = messaging.token.await()
                FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                    FirebaseFirestore.getInstance().collection(USERS).document(uid)
                        .update(FIELD_TOKENS, FieldValue.arrayRemove(token))
                        .addOnFailureListener { e -> Log.e(TAG, "Could not remove the token from Firestore", e) }
                }
                messaging.deleteToken().await()
                Log.d(TAG, "Removed this phone's FCM token")
            } ?: Log.d(TAG, "Token removal timed out; it finishes when the phone is online")
        } catch (e: Exception) {
            Log.e(TAG, "Could not remove the FCM token", e)
        }
    }

    private const val UNREGISTER_TIMEOUT_MS = 5_000L
}

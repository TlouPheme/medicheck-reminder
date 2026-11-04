package com.example.medicheckreminder.ui.auth

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.medicheckreminder.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException

/**
 * Google sign-in through Credential Manager, which replaces the deprecated GoogleSignInClient.
 *
 * Adapted from:
 * - https://developer.android.com/identity/sign-in/credential-manager-siwg
 * - https://firebase.google.com/docs/auth/android/google-signin
 */
class GoogleSignInHelper(context: Context) {

    private val appContext = context.applicationContext
    private val credentialManager = CredentialManager.create(appContext)

    /**
     * The OAuth web client ID that the google-services plugin generates from
     * `google-services.json`. Looked up by name so the app still builds without that file.
     */
    val webClientId: String?
        get() {
            @Suppress("DiscouragedApi")
            val id = appContext.resources.getIdentifier(
                "default_web_client_id", "string", appContext.packageName
            )
            return if (id == 0) null else appContext.getString(id).takeIf { it.isNotBlank() }
        }

    fun isOnline(): Boolean {
        val manager = appContext.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Shows the Google account sheet and returns the ID token for the chosen account. */
    suspend fun requestIdToken(activity: Activity): String {
        val clientId = webClientId ?: error("default_web_client_id is missing")
        val option = GetSignInWithGoogleOption.Builder(clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        val credential = credentialManager.getCredential(activity, request).credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            Log.d(TAG, "Received Google ID token for ${google.id}")
            return google.idToken
        }
        error("Unexpected credential type ${credential.type}")
    }

    /** Forgets the chosen Google account, so the next sign-in shows the account picker again. */
    suspend fun clearCredentialState() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e(TAG, "Could not clear credential state", e)
        }
    }

    private companion object {
        const val TAG = "GoogleSignInHelper"
    }
}

/** Turns a Google sign-in failure into a message for the person, or null when they cancelled. */
object GoogleSignInErrors {

    fun messageFor(error: Throwable): Int? = when (error) {
        is GetCredentialCancellationException -> null
        is NoCredentialException -> R.string.error_google_no_account
        is FirebaseNetworkException, is IOException, is TimeoutCancellationException ->
            R.string.error_no_network
        is GetCredentialException -> R.string.error_google_sign_in
        else -> R.string.error_google_sign_in
    }
}

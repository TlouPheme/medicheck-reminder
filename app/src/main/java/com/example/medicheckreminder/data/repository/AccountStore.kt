package com.example.medicheckreminder.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.medicheckreminder.R
import com.example.medicheckreminder.data.remote.FirebaseAuthGateway
import com.example.medicheckreminder.util.PasswordValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

/**
 * The signed-in account.
 *
 * Email/password accounts are checked against a salted hash in encrypted preferences, so login
 * works offline. When Firebase is configured, the same email and password also create or sign in
 * to a Firebase Auth account, and Google sign-in uses Firebase Auth directly. Both kinds of user
 * then share one Firestore record at `users/{uid}` (see [FirebaseAuthGateway]).
 */
class AccountStore(context: Context, private val remote: FirebaseAuthGateway) {

    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    fun hasSession(): Boolean = prefs.getBoolean(KEY_SIGNED_IN, false)

    fun displayName(): String {
        val name = prefs.getString(KEY_NAME, "").orEmpty().trim()
        return name.substringBefore(' ').trim()
    }

    fun profilePhotoUrl(): String? = prefs.getString(KEY_PHOTO_URL, null)?.takeIf { it.isNotBlank() }

    /** Changes whenever the photo does, so image caches don't serve a stale avatar. */
    fun profilePhotoVersion(): Long = prefs.getLong(KEY_PHOTO_VERSION, 0L)

    fun setProfilePhoto(url: String) {
        prefs.edit()
            .putString(KEY_PHOTO_URL, url)
            .putLong(KEY_PHOTO_VERSION, System.currentTimeMillis())
            .apply()
    }

    suspend fun register(email: String, password: String, name: String): AccountResult {
        return withContext(Dispatchers.IO) {
            val normalized = email.trim().lowercase()
            if (!PasswordValidator.validateEmail(normalized)) {
                return@withContext AccountResult.Failure(R.string.error_invalid_email)
            }
            if (!PasswordValidator.isComplexEnough(password)) {
                return@withContext AccountResult.Failure(R.string.error_weak_password)
            }
            val existing = prefs.getString(KEY_EMAIL, null)
            if (existing != null && existing.equals(normalized, ignoreCase = true)) {
                return@withContext AccountResult.Failure(R.string.error_account_exists)
            }
            val salt = UUID.randomUUID().toString()
            prefs.edit()
                .putString(KEY_EMAIL, normalized)
                .putString(KEY_NAME, name.trim())
                .putString(KEY_SALT, salt)
                .putString(KEY_HASH, digest(password, salt))
                .putString(KEY_PROVIDER, PROVIDER_PASSWORD)
                .putBoolean(KEY_SIGNED_IN, true)
                .apply()
            Log.d(TAG, "Registered local account")
            remote.registerWithEmail(normalized, password, name)
            AccountResult.Success
        }
    }

    suspend fun login(email: String, password: String): AccountResult {
        return withContext(Dispatchers.IO) {
            if (!PasswordValidator.validateEmail(email.trim())) {
                return@withContext AccountResult.Failure(R.string.error_invalid_email)
            }
            if (password.isEmpty()) {
                return@withContext AccountResult.Failure(R.string.error_password_required)
            }
            val normalized = email.trim().lowercase()
            if (matches(normalized, password)) {
                prefs.edit().putBoolean(KEY_SIGNED_IN, true).apply()
                Log.d(TAG, "Local login succeeded")
                remote.signInWithEmail(normalized, password)
                return@withContext AccountResult.Success
            }
            // Not on this phone yet: an account made on another phone can still sign in through
            // Firebase, and is then saved locally so the next login works offline.
            val user = remote.signInWithEmail(normalized, password)
                ?: return@withContext AccountResult.Failure(R.string.error_login_failed)
            saveLocal(normalized, user.displayName.orEmpty(), PROVIDER_PASSWORD, password)
            Log.d(TAG, "Restored account from Firebase")
            AccountResult.Success
        }
    }

    suspend fun resetPassword(email: String, password: String): AccountResult {
        return withContext(Dispatchers.IO) {
            if (!PasswordValidator.validateEmail(email.trim())) {
                return@withContext AccountResult.Failure(R.string.error_invalid_email)
            }
            if (!PasswordValidator.isComplexEnough(password)) {
                return@withContext AccountResult.Failure(R.string.error_weak_password)
            }
            val stored = prefs.getString(KEY_EMAIL, null)
            if (stored == null || !stored.equals(email.trim(), ignoreCase = true)) {
                return@withContext AccountResult.Failure(R.string.error_login_failed)
            }
            val salt = UUID.randomUUID().toString()
            prefs.edit()
                .putString(KEY_SALT, salt)
                .putString(KEY_HASH, digest(password, salt))
                .putBoolean(KEY_SIGNED_IN, false)
                .apply()
            AccountResult.Success
        }
    }

    /**
     * Completes Google sign-in with the ID token from Credential Manager. The Google account
     * replaces any other account on this phone; a password set earlier for the same email is kept.
     */
    suspend fun signInWithGoogle(idToken: String): AccountResult {
        return withContext(Dispatchers.IO) {
            val user = remote.signInWithGoogle(idToken)
            val email = user.email.orEmpty().trim().lowercase()
            if (email.isEmpty()) {
                return@withContext AccountResult.Failure(R.string.error_google_sign_in)
            }
            saveLocal(email, user.displayName.orEmpty(), PROVIDER_GOOGLE, password = null)
            Log.d(TAG, "Google sign-in saved locally")
            AccountResult.Success
        }
    }

    val isGoogleSignInAvailable: Boolean get() = remote.isAvailable

    fun signOut() {
        prefs.edit().putBoolean(KEY_SIGNED_IN, false).apply()
        remote.signOut()
    }

    private fun saveLocal(email: String, name: String, provider: String, password: String?) {
        val sameEmail = prefs.getString(KEY_EMAIL, null)?.equals(email, ignoreCase = true) == true
        val editor = prefs.edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_PROVIDER, provider)
            .putBoolean(KEY_SIGNED_IN, true)
        if (name.isNotBlank()) editor.putString(KEY_NAME, name.trim())
        if (password != null) {
            val salt = UUID.randomUUID().toString()
            editor.putString(KEY_SALT, salt).putString(KEY_HASH, digest(password, salt))
        } else if (!sameEmail) {
            // A different person's password must not unlock this Google account.
            editor.remove(KEY_SALT).remove(KEY_HASH)
        }
        if (!sameEmail) {
            // Nor should their profile photo show for the new account.
            editor.remove(KEY_PHOTO_URL).remove(KEY_PHOTO_VERSION)
        }
        editor.apply()
    }

    private fun matches(email: String, password: String): Boolean {
        val storedEmail = prefs.getString(KEY_EMAIL, null) ?: return false
        val salt = prefs.getString(KEY_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_HASH, null) ?: return false
        return storedEmail.equals(email.trim(), ignoreCase = true) && storedHash == digest(password, salt)
    }

    private fun digest(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest("$salt:$password".toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
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
        const val FILE_NAME = "medicheck_account"
        const val KEY_EMAIL = "email"
        const val KEY_NAME = "name"
        const val KEY_SALT = "salt"
        const val KEY_HASH = "hash"
        const val KEY_SIGNED_IN = "signed_in"
        const val KEY_PHOTO_URL = "photo_url"
        const val KEY_PHOTO_VERSION = "photo_version"
        const val KEY_PROVIDER = "provider"
        const val PROVIDER_PASSWORD = "password"
        const val PROVIDER_GOOGLE = "google.com"
        const val TAG = "AccountStore"
    }
}

sealed class AccountResult {
    data object Success : AccountResult()
    data class Failure(val messageRes: Int) : AccountResult()
}

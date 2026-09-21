package com.example.medicheckreminder.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.medicheckreminder.R
import com.example.medicheckreminder.util.PasswordValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

class AccountStore(context: Context) {

    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    fun hasSession(): Boolean = prefs.getBoolean(KEY_SIGNED_IN, false)

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
                .putBoolean(KEY_SIGNED_IN, true)
                .apply()
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
            if (!matches(email, password)) {
                return@withContext AccountResult.Failure(R.string.error_login_failed)
            }
            prefs.edit().putBoolean(KEY_SIGNED_IN, true).apply()
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

    fun signOut() {
        prefs.edit().putBoolean(KEY_SIGNED_IN, false).apply()
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
    }
}

sealed class AccountResult {
    data object Success : AccountResult()
    data class Failure(val messageRes: Int) : AccountResult()
}

package com.example.medicheckreminder.util

import android.util.Patterns

object PasswordValidator {

    enum class Strength(val score: Int) {
        WEAK(1), MEDIUM(2), STRONG(3)
    }

    fun validateEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun calculateStrength(password: String): Strength? {
        if (password.isBlank()) return null
        
        var score = 0
        if (password.length >= 8) score++
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() }) score++
        
        return when {
            score >= 3 -> Strength.STRONG
            score >= 2 -> Strength.MEDIUM
            else -> Strength.WEAK
        }
    }

    fun isComplexEnough(password: String): Boolean {
        val pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$".toRegex()
        return pattern.matches(password)
    }
}

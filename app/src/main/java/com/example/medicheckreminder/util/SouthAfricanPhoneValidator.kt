package com.example.medicheckreminder.util

object SouthAfricanPhoneValidator {

    private val pattern = Regex("""^(\+27|0)[1-8]\d{8}$""")

    fun isValid(raw: String): Boolean {
        val normalized = raw.filter { it.isDigit() || it == '+' }
        return pattern.matches(normalized)
    }

    fun isValidOrBlank(raw: String): Boolean = raw.isBlank() || isValid(raw)
}

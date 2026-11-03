package com.example.medicheckreminder.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SouthAfricanPhoneValidatorTest {

    @Test
    fun localAndInternationalNumbers_areValid() {
        assertTrue(SouthAfricanPhoneValidator.isValid("0821234567"))
        assertTrue(SouthAfricanPhoneValidator.isValid("+27 82 123 4567"))
    }

    @Test
    fun wrongLengthOrPrefix_isInvalid() {
        assertFalse(SouthAfricanPhoneValidator.isValid("082123456"))
        assertFalse(SouthAfricanPhoneValidator.isValid("0921234567"))
        assertFalse(SouthAfricanPhoneValidator.isValid("+44 7700 900123"))
    }

    @Test
    fun blankCaregiverNumber_isAllowed() {
        assertTrue(SouthAfricanPhoneValidator.isValidOrBlank(""))
        assertFalse(SouthAfricanPhoneValidator.isValidOrBlank("123"))
    }
}

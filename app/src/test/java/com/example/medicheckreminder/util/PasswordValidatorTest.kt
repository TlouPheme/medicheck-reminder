package com.example.medicheckreminder.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordValidatorTest {

    @Test
    fun complexPassword_isAccepted() {
        assertTrue(PasswordValidator.isComplexEnough("Medi@1234"))
    }

    @Test
    fun passwordMissingARule_isRejected() {
        assertFalse(PasswordValidator.isComplexEnough("medi@1234")) // no upper case
        assertFalse(PasswordValidator.isComplexEnough("MEDI@1234")) // no lower case
        assertFalse(PasswordValidator.isComplexEnough("Medi@abcd")) // no digit
        assertFalse(PasswordValidator.isComplexEnough("Medi12345")) // no symbol
        assertFalse(PasswordValidator.isComplexEnough("Me@1")) // too short
    }

    @Test
    fun strength_growsWithEachRuleMet() {
        assertNull(PasswordValidator.calculateStrength(""))
        assertEquals(PasswordValidator.Strength.WEAK, PasswordValidator.calculateStrength("abc"))
        assertEquals(PasswordValidator.Strength.MEDIUM, PasswordValidator.calculateStrength("Abcdefgh"))
        assertEquals(PasswordValidator.Strength.STRONG, PasswordValidator.calculateStrength("Abcdefg1!"))
    }
}

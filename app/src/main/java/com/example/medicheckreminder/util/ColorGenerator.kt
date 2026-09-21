package com.example.medicheckreminder.util

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils

/**
 * Stable palette lookup so a medication keeps the same pill color
 * across sessions (derived from the name, not a random pick).
 */
object ColorGenerator {

    data class Swatch(
        @ColorInt val background: Int,
        @ColorInt val onBackground: Int
    )

    private val palette = intArrayOf(
        0xFF2563EB.toInt(),
        0xFF7C3AED.toInt(),
        0xFFDB2777.toInt(),
        0xFFEA580C.toInt(),
        0xFFCA8A04.toInt(),
        0xFF16A34A.toInt(),
        0xFF0D9488.toInt(),
        0xFF0284C7.toInt(),
        0xFF4F46E5.toInt(),
        0xFFDC2626.toInt()
    )

    fun fromName(name: String): Swatch {
        val index = stableIndex(name, palette.size)
        val background = palette[index]
        return Swatch(
            background = background,
            onBackground = onColor(background)
        )
    }

    @ColorInt
    fun colorFor(name: String): Int = fromName(name).background

    private fun stableIndex(name: String, size: Int): Int {
        var hash = 0
        for (char in name.trim().lowercase()) {
            hash = 31 * hash + char.code
        }
        return (hash and Int.MAX_VALUE) % size
    }

    @ColorInt
    private fun onColor(@ColorInt background: Int): Int {
        return if (ColorUtils.calculateLuminance(background) > 0.183) {
            0xFF1E293B.toInt()
        } else {
            Color.WHITE
        }
    }
}

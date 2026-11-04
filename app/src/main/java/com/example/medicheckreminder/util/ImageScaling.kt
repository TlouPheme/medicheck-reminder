package com.example.medicheckreminder.util

import kotlin.math.roundToInt

object ImageScaling {

    /** Scales (width, height) down so the longest edge is at most [maxEdge]; never scales up. */
    fun fitWithin(width: Int, height: Int, maxEdge: Int): Pair<Int, Int> {
        require(width > 0 && height > 0 && maxEdge > 0) { "Dimensions must be positive" }
        val longest = maxOf(width, height)
        if (longest <= maxEdge) return width to height
        val scale = maxEdge.toFloat() / longest
        return (width * scale).roundToInt().coerceAtLeast(1) to
                (height * scale).roundToInt().coerceAtLeast(1)
    }

    fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        require(width > 0 && height > 0 && maxEdge > 0) { "Dimensions must be positive" }
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= maxEdge) sample *= 2
        return sample
    }
}

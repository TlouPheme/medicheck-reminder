package com.example.medicheckreminder

import com.example.medicheckreminder.util.ImageScaling
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImageScalingTest {
    @Test fun fitWithin_landscape() = assertEquals(512 to 384, ImageScaling.fitWithin(4000, 3000, 512))
    @Test fun fitWithin_portrait() = assertEquals(384 to 512, ImageScaling.fitWithin(3000, 4000, 512))
    @Test fun fitWithin_smallNotScaledUp() = assertEquals(400 to 300, ImageScaling.fitWithin(400, 300, 512))
    @Test fun fitWithin_extremeRatioKeepsOnePixel() = assertEquals(512 to 1, ImageScaling.fitWithin(100_000, 10, 512))
    @Test fun fitWithin_invalidThrows() {
        assertThrows(IllegalArgumentException::class.java) { ImageScaling.fitWithin(0, 100, 512) }
    }
    @Test fun sampleSize_large() = assertEquals(4, ImageScaling.sampleSize(4000, 3000, 512))
    @Test fun sampleSize_small() = assertEquals(1, ImageScaling.sampleSize(600, 400, 512))
}
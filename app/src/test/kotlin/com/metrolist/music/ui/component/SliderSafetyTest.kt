package com.metrolist.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SliderSafetyTest {
    @Test
    fun `invalid range returns safe zero fraction`() {
        assertEquals(0f, safeSliderFraction(0.5f, 1f..1f))
        assertEquals(0f, safeSliderFraction(Float.NaN, 0f..1f))
        assertEquals(0f, safeSliderFraction(0.5f, 1f..0f))
    }

    @Test
    fun `valid values are clamped to the drawing range`() {
        assertEquals(0f, safeSliderFraction(-1f, 0f..1f))
        assertEquals(0.5f, safeSliderFraction(0.5f, 0f..1f))
        assertEquals(1f, safeSliderFraction(2f, 0f..1f))
        assertTrue(safeSliderFraction(Float.POSITIVE_INFINITY, 0f..1f).isFinite())
    }
}

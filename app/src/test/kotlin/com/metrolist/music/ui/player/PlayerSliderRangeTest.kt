package com.metrolist.music.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSliderRangeTest {
    @Test
    fun `unknown duration uses a finite disabled placeholder range`() {
        val input = playerSliderInput(durationMillis = 0L, positionMillis = 50L)

        assertEquals(0f, input.valueRange.start, 0f)
        assertEquals(1f, input.valueRange.endInclusive, 0f)
        assertEquals(0f, input.value, 0f)
        assertFalse(input.isSeekable)
    }

    @Test
    fun `valid duration clamps position into the slider range`() {
        val input = playerSliderInput(durationMillis = 10_000L, positionMillis = 12_500L)

        assertEquals(0f, input.valueRange.start, 0f)
        assertEquals(10_000f, input.valueRange.endInclusive, 0f)
        assertEquals(10_000f, input.value, 0f)
        assertTrue(input.isSeekable)
    }

    @Test
    fun `large duration remains finite`() {
        val input = playerSliderInput(durationMillis = Long.MAX_VALUE, positionMillis = Long.MAX_VALUE)

        assertTrue(input.valueRange.endInclusive.isFinite())
        assertTrue(input.value.isFinite())
        assertTrue(input.isSeekable)
    }
}

package com.metrolist.music.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSliderRangeTest {
    @Test
    fun `unknown and invalid durations use a finite disabled placeholder range`() {
        listOf(0L, -1L, Long.MIN_VALUE).forEach { duration ->
            val input = playerSliderInput(duration, positionMillis = 50L)

            assertEquals(0f, input.valueRange.start, 0f)
            assertEquals(1f, input.valueRange.endInclusive, 0f)
            assertEquals(0f, input.value, 0f)
            assertFalse(input.isSeekable)
        }
    }

    @Test
    fun `valid duration clamps player position into the slider range`() {
        val input = playerSliderInput(durationMillis = 10_000L, positionMillis = 12_500L)

        assertEquals(0f, input.valueRange.start, 0f)
        assertEquals(10_000f, input.valueRange.endInclusive, 0f)
        assertEquals(10_000f, input.value, 0f)
        assertTrue(input.isSeekable)
    }

    @Test
    fun `negative position is clamped to the start`() {
        val input = playerSliderInput(durationMillis = 10_000L, positionMillis = -5L)

        assertEquals(0f, input.value, 0f)
        assertTrue(input.isSeekable)
    }

    @Test
    fun `large positive durations still produce a finite range`() {
        val input = playerSliderInput(durationMillis = Long.MAX_VALUE, positionMillis = Long.MAX_VALUE)

        assertTrue(input.valueRange.endInclusive.isFinite())
        assertTrue(input.value.isFinite())
        assertTrue(input.isSeekable)
    }
}

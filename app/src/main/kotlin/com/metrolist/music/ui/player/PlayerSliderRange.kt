package com.metrolist.music.ui.player

internal data class PlayerSliderInput(
    val valueRange: ClosedFloatingPointRange<Float>,
    val value: Float,
    val isSeekable: Boolean,
)

/**
 * Compose sliders require a usable range even before the current item reports its duration.
 * Use a disabled 0..1 placeholder until a positive duration is available, and clamp positions
 * from the player/cast state before they are converted to Float.
 */
internal fun playerSliderInput(durationMillis: Long, positionMillis: Long): PlayerSliderInput {
    val maxValue =
        durationMillis
            .takeIf { it > 0L }
            ?.toFloat()
            ?.takeIf { it.isFinite() && it > 0f }

    if (maxValue == null) {
        return PlayerSliderInput(valueRange = 0f..1f, value = 0f, isSeekable = false)
    }

    val position = positionMillis.toFloat().takeIf(Float::isFinite) ?: 0f
    return PlayerSliderInput(
        valueRange = 0f..maxValue,
        value = position.coerceIn(0f, maxValue),
        isSeekable = true,
    )
}

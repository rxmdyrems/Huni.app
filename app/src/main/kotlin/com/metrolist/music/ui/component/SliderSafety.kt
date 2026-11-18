package com.metrolist.music.ui.component

/**
 * Converts a slider value to a safe 0..1 fraction for drawing.
 *
 * Settings previews can be composed before a persisted value has a usable range.
 * Keeping this guard at the drawing boundary prevents NaN/Infinity from reaching
 * Compose progress and canvas APIs.
 */
internal fun safeSliderFraction(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
): Float {
    val start = valueRange.start
    val end = valueRange.endInclusive
    if (!value.isFinite() || !start.isFinite() || !end.isFinite() || end <= start) return 0f
    return ((value - start) / (end - start)).coerceIn(0f, 1f)
}

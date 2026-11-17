package com.metrolist.music.ui.component

/** Pure geometry used by the floating navigation dock and its unit tests. */
data class NavigationDockMetrics(
    val heightDp: Float,
    val horizontalPaddingDp: Float,
    val labelAlpha: Float,
    val menuSizeDp: Float,
)

fun navigationDockMetrics(
    collapseFraction: Float,
    compact: Boolean,
    keepExpanded: Boolean,
): NavigationDockMetrics {
    val progress = if (keepExpanded || !collapseFraction.isFinite()) {
        if (collapseFraction == Float.POSITIVE_INFINITY) 1f else 0f
    } else {
        collapseFraction.coerceIn(0f, 1f)
    }
    val expandedHeight = if (compact) 58f else 66f
    val collapsedHeight = if (compact) 50f else 56f
    return NavigationDockMetrics(
        heightDp = expandedHeight + (collapsedHeight - expandedHeight) * progress,
        horizontalPaddingDp = 16f + 12f * progress,
        labelAlpha = 1f - progress,
        menuSizeDp = 64f - 10f * progress,
    )
}

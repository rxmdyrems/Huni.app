package com.metrolist.music.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/** A display-only preview; style selection is handled by the surrounding card. */
@Composable
fun SliderPreview(
    value: Float,
    colors: SliderColors = SliderDefaults.colors(),
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val fraction = safeSliderFraction(value, 0f..1f)
    Canvas(modifier.fillMaxWidth().height(if (compact) 14.dp else 32.dp)) {
        val y = size.height / 2f
        val width = size.width
        val stroke = if (compact) 6.dp.toPx() else 10.dp.toPx()
        val thumbRadius = if (compact) 0f else 8.dp.toPx()
        drawLine(
            color = colors.inactiveTrackColor,
            start = androidx.compose.ui.geometry.Offset(0f, y),
            end = androidx.compose.ui.geometry.Offset(width, y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = colors.activeTrackColor,
            start = androidx.compose.ui.geometry.Offset(0f, y),
            end = androidx.compose.ui.geometry.Offset(width * fraction, y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        if (thumbRadius > 0f) {
            drawCircle(
                color = colors.thumbColor,
                radius = thumbRadius,
                center = androidx.compose.ui.geometry.Offset(width * fraction, y),
            )
        }
    }
}

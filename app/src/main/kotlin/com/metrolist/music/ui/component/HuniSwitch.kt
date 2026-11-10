package com.metrolist.music.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R

@Composable
fun HuniSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    thumbContent: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val offOutline = lerp(colors.outline, colors.onSurface, 0.2f)
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.drawWithContent {
            drawContent()
            if (!checked) {
                val trackWidth = 52.dp.toPx()
                val trackHeight = 30.dp.toPx()
                val stroke = 2.dp.toPx()
                val left = (size.width - trackWidth) / 2
                val top = (size.height - trackHeight) / 2
                val alpha = if (enabled) 1f else 0.38f
                drawRoundRect(
                    color = colors.surfaceContainerHigh.copy(alpha = alpha),
                    topLeft = Offset(left, top),
                    size = Size(trackWidth, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2),
                )
                drawRoundRect(
                    color = offOutline.copy(alpha = alpha),
                    topLeft = Offset(left + stroke / 2, top + stroke / 2),
                    size = Size(trackWidth - stroke, trackHeight - stroke),
                    cornerRadius = CornerRadius((trackHeight - stroke) / 2),
                    style = Stroke(width = stroke),
                )
                drawCircle(
                    color = Color(0xFFA99FB8).copy(alpha = alpha),
                    radius = 8.dp.toPx(),
                    center = Offset(left + 12.dp.toPx(), top + trackHeight / 2),
                )
            }
        },
        thumbContent = if (checked) thumbContent ?: {
            Icon(
                painter = painterResource(R.drawable.check),
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
                tint = colors.primary,
            )
        } else null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onPrimary,
            checkedTrackColor = colors.primary,
            checkedBorderColor = colors.primary,
            checkedIconColor = colors.primary,
            uncheckedThumbColor = Color.Transparent,
            uncheckedTrackColor = Color.Transparent,
            uncheckedBorderColor = Color.Transparent,
            uncheckedIconColor = colors.onSurfaceVariant,
        ),
    )
}

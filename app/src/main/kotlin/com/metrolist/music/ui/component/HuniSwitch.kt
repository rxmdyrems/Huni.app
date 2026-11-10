package com.metrolist.music.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Stroke
import androidx.compose.ui.graphics.lerp
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
                val stroke = 2.dp.toPx()
                drawRoundRect(
                    color = offOutline,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius((size.height - stroke) / 2),
                    style = Stroke(width = stroke),
                )
            }
        },
        thumbContent = thumbContent ?: {
            Icon(
                painter = painterResource(if (checked) R.drawable.check else R.drawable.close),
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
                tint = if (checked) colors.primary else colors.onSurfaceVariant,
            )
        },
        thumbContent = thumbContent,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onPrimary,
            checkedTrackColor = colors.primary,
            checkedBorderColor = colors.primary,
            checkedIconColor = colors.primary,
            uncheckedThumbColor = colors.onSurfaceVariant,
            uncheckedTrackColor = Color.Transparent,
            uncheckedBorderColor = offOutline,
            uncheckedIconColor = colors.onSurfaceVariant,
        ),
    )
}

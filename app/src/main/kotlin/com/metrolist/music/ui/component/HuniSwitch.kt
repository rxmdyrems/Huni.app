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
        modifier = modifier,
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
            uncheckedThumbColor = Color(0xFFA99FB8),
            uncheckedTrackColor = colors.surfaceContainerHigh,
            uncheckedBorderColor = offOutline,
            uncheckedIconColor = colors.onSurfaceVariant,
        ),
    )
}

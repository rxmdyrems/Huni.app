package com.metrolist.music.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

@Composable
fun HuniSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    maxLines: Int = 2,
    minSize: Int = 14,
) {
    var fontSize by remember(text) { mutableStateOf(style.fontSize.value.coerceAtMost(18f)) }
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(fontSize = fontSize.sp),
        color = MaterialTheme.colorScheme.primary,
        maxLines = maxLines,
        softWrap = true,
        overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
        onTextLayout = { result ->
            if ((result.didOverflowHeight || result.lineCount > maxLines) && fontSize > minSize) {
                fontSize -= 1f
            }
        },
    )
}

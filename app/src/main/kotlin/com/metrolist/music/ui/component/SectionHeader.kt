package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HuniSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    maxLines: Int = 2,
    minSize: Int = 14,
) {
    var fontSize by remember(text, style) {
        mutableFloatStateOf(style.fontSize.value.coerceAtMost(18f))
    }
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(fontSize = fontSize.sp),
        color = MaterialTheme.colorScheme.primary,
        maxLines = maxLines,
        softWrap = true,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { result ->
            if ((result.didOverflowHeight || result.lineCount > maxLines) && fontSize > minSize) {
                fontSize = (fontSize - 1f).coerceAtLeast(minSize.toFloat())
            }
        },
    )
}

@Composable
fun HuniScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
) {
    HuniAutoFitText(
        text = text,
        modifier = modifier.padding(end = 16.dp),
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        minSize = 16,
        maxSize = 22,
        maxLines = 1,
        softWrap = false,
        textAlign = TextAlign.Start,
    )
}

@Composable
fun HuniAutoFitText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color,
    minSize: Int,
    maxSize: Int,
    maxLines: Int = 1,
    softWrap: Boolean = false,
    textAlign: TextAlign = TextAlign.Center,
) {
    var fontSize by remember(text, style, minSize, maxSize) {
        mutableFloatStateOf(style.fontSize.value.coerceIn(minSize.toFloat(), maxSize.toFloat()))
    }
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(fontSize = fontSize.sp, textAlign = textAlign),
        color = color,
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { result ->
            if ((result.didOverflowWidth || result.didOverflowHeight || result.lineCount > maxLines) && fontSize > minSize) {
                fontSize = (fontSize - 1f).coerceAtLeast(minSize.toFloat())
            }
        },
    )
}

package com.metrolist.music.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.constants.ThumbnailCornerRadius

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RandomizeGridItem(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dotOffsetMultiplier by animateFloatAsState(
        targetValue = if (isLoading) 0f else 1f,
        animationSpec = tween(durationMillis = 600),
        label = "dotOffset",
    )
    val loadingAlpha by animateFloatAsState(
        targetValue = if (isLoading) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "loadingAlpha",
    )

    Column(modifier = modifier.fillMaxSize().clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(ThumbnailCornerRadius)).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            val dotColor = MaterialTheme.colorScheme.onSecondaryContainer
            val dotSize = 14.dp
            val padding = 24.dp
            Box(Modifier.align(Alignment.Center).offset { IntOffset((-padding * dotOffsetMultiplier).roundToPx(), (-padding * dotOffsetMultiplier).roundToPx()) }.size(dotSize).clip(CircleShape).background(dotColor))
            Box(Modifier.align(Alignment.Center).offset { IntOffset((padding * dotOffsetMultiplier).roundToPx(), (-padding * dotOffsetMultiplier).roundToPx()) }.size(dotSize).clip(CircleShape).background(dotColor))
            Box(Modifier.align(Alignment.Center).size(dotSize).clip(CircleShape).background(dotColor))
            Box(Modifier.align(Alignment.Center).offset { IntOffset((-padding * dotOffsetMultiplier).roundToPx(), (padding * dotOffsetMultiplier).roundToPx()) }.size(dotSize).clip(CircleShape).background(dotColor))
            Box(Modifier.align(Alignment.Center).offset { IntOffset((padding * dotOffsetMultiplier).roundToPx(), (padding * dotOffsetMultiplier).roundToPx()) }.size(dotSize).clip(CircleShape).background(dotColor))
            Box(modifier = Modifier.alpha(loadingAlpha)) {
                LoadingIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Text(
            text = "More",
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

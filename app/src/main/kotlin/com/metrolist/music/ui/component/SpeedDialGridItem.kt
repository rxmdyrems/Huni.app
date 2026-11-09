package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.R
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.ui.utils.resize

@Composable
fun SpeedDialGridItem(
    item: YTItem,
    isPinned: Boolean,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)),
        ) {
            ItemThumbnail(
                thumbnailUrl = item.thumbnail?.resize(200, 200),
                isActive = isActive,
                isPlaying = isPlaying,
                shape = if (item is ArtistItem) CircleShape else RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxSize(),
            )
            if (isPinned) {
                Icon(painter = painterResource(R.drawable.ic_push_pin), contentDescription = "Pinned", tint = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp))
            }
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        )
    }
}

package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.ui.theme.bbhBartle

@Composable
fun HuniScreenTitle(text: String, modifier: Modifier = Modifier) {
    var fontSize by remember(text) { mutableIntStateOf(22) }
    Text(
        text = text,
        modifier = modifier.padding(end = 16.dp),
        style = MaterialTheme.typography.titleLarge.copy(fontSize = fontSize.sp),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize > 16) fontSize -= 1
        },
    )
}

@Composable
fun HuniWordmark(greeting: String, name: String) {
    Column {
        Text(
            text = "HUNI",
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = bbhBartle, fontSize = 28.sp),
            maxLines = 1,
            softWrap = false,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "$greeting, ",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

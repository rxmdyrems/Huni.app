package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.BuildConfig
import com.metrolist.music.R
import com.metrolist.music.ui.component.HuniScreenTitle
import com.metrolist.music.ui.utils.ReleaseNoteEntry
import com.metrolist.music.ui.utils.ReleaseNoteSection
import com.metrolist.music.ui.utils.ReleaseNotesDocument
import com.metrolist.music.ui.utils.ReleaseNotesParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val notes = remember {
        runCatching {
            context.assets.open("CHANGELOG.md").bufferedReader().use { ReleaseNotesParser.parse(it.readText()) }
        }.getOrDefault(ReleaseNotesDocument(emptyList(), emptyList(), null))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 92.dp),
            ) {
                item {
                    HuniScreenTitle("Changelog", modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                }
                if (notes.preambleSections.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        ) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ReleaseNoteSections(notes.preambleSections)
                            }
                        }
                    }
                }
                items(notes.entries) { entry ->
                    ChangelogEntryCard(entry)
                }
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                notes.footer?.let { footer ->
                    item {
                        Text(
                            text = footer,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            ExtendedFloatingActionButton(
                onClick = { uriHandler.openUri("https://github.com/rxmdyrems/Huni.app/releases") },
                icon = { androidx.compose.material3.Icon(painterResource(R.drawable.github), null, Modifier.size(22.dp)) },
                text = { Text("View all releases on GitHub") },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }
}

@Composable
private fun ChangelogEntryCard(entry: ReleaseNoteEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("v${entry.version}", fontFamily = FontFamily.Default, fontSize = 16.sp)
                if (entry.version == BuildConfig.BASE_VERSION_NAME) AssistChip(onClick = {}, label = { Text("Installed") })
            }
            if (entry.date.isNotBlank()) {
                Text(entry.date, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ReleaseNoteSections(entry.sections)
        }
    }
}

@Composable
private fun ReleaseNoteSections(sections: List<ReleaseNoteSection>) {
    sections.forEach { section ->
        section.heading?.let { heading ->
            Text(
                text = heading,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                ),
            )
        }
        section.bullets.forEach { bullet ->
            Text("• $bullet", fontSize = 16.sp)
        }
    }
}

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.BuildConfig
import com.metrolist.music.R

data class ChangelogEntry(val version: String, val date: String, val groups: List<Pair<String?, List<String>>>)

private fun parseChangelog(markdown: String): List<ChangelogEntry> {
    val entries = mutableListOf<ChangelogEntry>()
    var current: ChangelogEntry? = null
    var group: String? = null
    val groups = mutableListOf<Pair<String?, MutableList<String>>>()
    fun finish() { current?.let { entries += it.copy(groups = groups.map { (name, items) -> name to items.toList() }) } }
    markdown.lineSequence().forEach { line ->
        val heading = Regex("^##\\s+v?([^\\s(]+)(?:\\s*\\(([^)]+)\\))?").find(line)
        if (heading != null) {
            finish(); groups.clear(); group = null
            current = ChangelogEntry(heading.groupValues[1], heading.groupValues.getOrNull(2).orEmpty(), emptyList())
        } else if (current != null && line.trim().startsWith("### ")) {
            group = line.trim().removePrefix("### ").trim()
            groups.add(group to mutableListOf<String>())
        } else if (current != null && line.trim().startsWith("- ")) {
            if (groups.isEmpty()) groups += null to mutableListOf()
            groups.last().second += line.trim().removePrefix("- ").trim()
        }
    }
    finish()
    return entries
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val entries = remember {
        runCatching { context.assets.open("CHANGELOG.md").bufferedReader().use { parseChangelog(it.readText()) } }.getOrDefault(emptyList())
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
                item { Text("Changelog", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 4.dp)) }
                items(entries) { entry ->
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
                            if (entry.date.isNotBlank()) Text(entry.date, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            entry.groups.forEach { (name, bullets) ->
                                if (!name.isNullOrBlank()) Text(name, style = MaterialTheme.typography.titleSmall)
                                bullets.forEach { Text("• $it", fontSize = 16.sp) }
                            }
                        }
                    }
                }
                item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
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

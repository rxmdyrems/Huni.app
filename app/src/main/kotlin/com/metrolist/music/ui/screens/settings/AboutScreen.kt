package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.metrolist.music.BuildConfig
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.dataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class AboutLink(val title: String, val subtitle: String, val icon: Int, val url: String)
private val aboutLinks = listOf(
    AboutLink("Source code", "github.com/rxmdyrems/Huni.app", R.drawable.github, "https://github.com/rxmdyrems/Huni.app"),
    AboutLink("Changelog", "Release history and changes", R.drawable.history, "https://github.com/rxmdyrems/Huni.app/blob/main/CHANGELOG.md"),
    AboutLink("License: GPL v3", "GNU General Public License v3.0", R.drawable.balance_scale, "https://github.com/rxmdyrems/Huni.app/blob/main/LICENSE"),
    AboutLink("Report a bug", "Open an issue on GitHub", R.drawable.bug_report, "https://github.com/rxmdyrems/Huni.app/issues/new/choose"),
)

@Composable
fun AboutScreen(navController: NavController, scrollBehavior: TopAppBarScrollBehavior) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState()).nestedScroll(scrollBehavior.nestedScrollConnection).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        Spacer(Modifier.height(20.dp))
        Surface(Modifier.size(100.dp), CircleShape, Color.Transparent, border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)) {
            Image(painter = painterResource(R.drawable.avatar_remy), contentDescription = "Remy Camiguel avatar", contentScale = ContentScale.Crop, modifier = Modifier.padding(2.dp).clip(CircleShape))
        }
        Spacer(Modifier.height(14.dp))
        var identitySize by remember { mutableIntStateOf(22) }
        Text(
            "Remy Camiguel",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = identitySize.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            onTextLayout = { if (it.didOverflowWidth && identitySize > 16) identitySize-- },
        )
        Text("Developer of Huni", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            var chipSize by remember { mutableIntStateOf(13) }
            Text(
                "${BuildConfig.VERSION_NAME} · Universal",
                Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = chipSize.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (it.didOverflowWidth && chipSize > 10) chipSize-- },
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "A music player with no sign-in. Everything stays on your device.",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Local profile", style = MaterialTheme.typography.titleMedium)
                Text("Your name and avatar stay on this device and are excluded from Android backup.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(24.dp)); SectionLabel("Links"); Spacer(Modifier.height(10.dp))
        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(vertical = 4.dp)) {
                aboutLinks.forEachIndexed { index, link ->
                    Row(Modifier.fillMaxWidth().clickable { uriHandler.openUri(link.url) }.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(Modifier.size(40.dp), RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.outline) {
                            Icon(painterResource(link.icon), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(9.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(link.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(link.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(painterResource(R.drawable.arrow_forward), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    if (index < aboutLinks.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        Spacer(Modifier.height(24.dp)); SectionLabel("Built on"); Spacer(Modifier.height(10.dp))
        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UpstreamRow("Metrolist / InnerTune", "GPL v3")
                UpstreamRow("AndroidX Media3", "Apache 2.0")
                UpstreamRow("Ktor, Coil, and Material 3", "Apache 2.0")
            }
        }
        Spacer(Modifier.height(24.dp))
        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.errorContainer) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Delete all data", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                Text("Remove library, settings, account data, caches, downloads, and your local profile. This cannot be undone.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                OutlinedButton(onClick = { deleteConfirmation = ""; showDeleteDialog = true }) { Text("Delete all data") }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete all data?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("This permanently removes all local Huni data. Type DELETE to confirm.")
                    OutlinedTextField(value = deleteConfirmation, onValueChange = { deleteConfirmation = it }, singleLine = true, label = { Text("Type DELETE") })
                }
            },
            confirmButton = {
                Button(enabled = deleteConfirmation == "DELETE", onClick = {
                    scope.launch(Dispatchers.IO) {
                        context.dataStore.edit { it.clear() }
                        context.deleteDatabase("song.db")
                        context.cacheDir.deleteRecursively()
                        context.filesDir.resolve("profile").deleteRecursively()
                        context.filesDir.resolve("download").deleteRecursively()
                        context.noBackupFilesDir.deleteRecursively()
                        withContext(Dispatchers.Main) {
                            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                context.startActivity(this)
                            }
                            (context as? android.app.Activity)?.finishAffinity()
                        }
                    }
                }) { Text("Delete permanently") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
        )
    }
    TopAppBar(title = { com.metrolist.music.ui.component.HuniScreenTitle(stringResource(R.string.about)) }, navigationIcon = {
        IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) { Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.cd_back)) }
    }, scrollBehavior = scrollBehavior)
}

@Composable private fun SectionLabel(text: String) {
    com.metrolist.music.ui.component.HuniSectionHeader(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}
@Composable private fun UpstreamRow(name: String, license: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(license, modifier = Modifier.width(84.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, maxLines = 1, softWrap = false)
    }
}

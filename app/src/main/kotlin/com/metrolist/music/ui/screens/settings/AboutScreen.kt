package com.metrolist.music.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.music.BuildConfig
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.UserAvatarKey
import com.metrolist.music.constants.UserNameKey
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.AppDataWiper
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.safeDataStoreEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

private val avatarPresets = listOf(
    R.drawable.avatar_amber, R.drawable.avatar_violet, R.drawable.avatar_green, R.drawable.avatar_maroon,
    R.drawable.avatar_ocean, R.drawable.avatar_teal, R.drawable.avatar_rose, R.drawable.avatar_orange,
    R.drawable.avatar_indigo, R.drawable.avatar_slate, R.drawable.avatar_mint, R.drawable.avatar_coral,
)
private val avatarIds = listOf("avatar_amber", "avatar_violet", "avatar_green", "avatar_maroon", "avatar_ocean", "avatar_teal", "avatar_rose", "avatar_orange", "avatar_indigo", "avatar_slate", "avatar_mint", "avatar_coral")
private data class AboutLink(val title: String, val subtitle: String, val icon: Int, val url: String)
private val aboutLinks = listOf(
    AboutLink("Source code", "github.com/rxmdyrems/Huni.app", R.drawable.github, "https://github.com/rxmdyrems/Huni.app"),
    AboutLink("Changelog", "Release history and changes", R.drawable.delete_history, "https://github.com/rxmdyrems/Huni.app/blob/main/CHANGELOG.md"),
    AboutLink("License GPL v3", "GNU General Public License v3.0", R.drawable.balance_scale, "https://github.com/rxmdyrems/Huni.app/blob/main/LICENSE"),
    AboutLink("Report a bug", "Open an issue on GitHub", R.drawable.bug_report, "https://github.com/rxmdyrems/Huni.app/issues/new/choose"),
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AboutScreen(navController: NavController, scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val name by rememberPreference(UserNameKey, "Music lover")
    val avatar by rememberPreference(UserAvatarKey, "avatar_amber")
    var editName by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf(name) }
    var photoSheet by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var deleteText by remember { mutableStateOf("") }
    var wiping by remember { mutableStateOf(false) }
    var wipeError by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                File(context.filesDir, "profile").mkdirs()
                File(context.filesDir, "profile/avatar.webp").outputStream().use { output -> input.copyTo(output) }
            }
            context.safeDataStoreEdit { it[UserAvatarKey] = "file" }
        }
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        Spacer(Modifier.height(18.dp))
        ProfileAvatar(context, avatar, 84)
        Spacer(Modifier.height(10.dp))
        Text(name.ifBlank { "Music lover" }, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        Text("Your music, your way.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(22.dp))
        SectionLabel("You")
        CardColumn {
            ActionRow(R.drawable.edit, "Edit name", "Change the name Huni uses for you") { nameDraft = name; editName = true }
            DividerRow()
            ActionRow(R.drawable.hide_image, "Change photo", "Choose a preset or use your own photo") { photoSheet = true }
            DividerRow()
            ActionRow(R.drawable.delete, "Delete app data", "Erases everything saved on this phone", MaterialTheme.colorScheme.error) { deleteText = ""; deleteDialog = true }
        }
        Spacer(Modifier.height(24.dp)); SectionLabel("Developer"); Spacer(Modifier.height(10.dp))
        CardColumn {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.avatar_remy), "Remy Camiguel", Modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Column(Modifier.weight(1f)) { Text("Remy Camiguel", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis); Text("Developer of Huni", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHighest) { Text(text = "${BuildConfig.VERSION_NAME} · Universal", modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) }
            }
            aboutLinks.forEachIndexed { i, link ->
                DividerRow(); ActionRow(link.icon, link.title, link.subtitle) { uriHandler.openUri(link.url) }
            }
        }
        Spacer(Modifier.height(24.dp)); SectionLabel("Built on"); Spacer(Modifier.height(10.dp))
        CardColumn { UpstreamRow("Metrolist / InnerTune", "GPL v3"); UpstreamRow("AndroidX Media3", "Apache 2.0"); UpstreamRow("Ktor, Coil, and Material 3", "Apache 2.0") }
        Spacer(Modifier.height(28.dp))
    }

    if (editName) AlertDialog(onDismissRequest = { editName = false }, title = { Text("Edit name") }, text = { OutlinedTextField(value = nameDraft, onValueChange = { nameDraft = it.take(20) }, label = { Text("Name") }, singleLine = true) }, confirmButton = { Button(onClick = { scope.launch { context.safeDataStoreEdit { it[UserNameKey] = nameDraft.trim().ifBlank { "Music lover" } } }; editName = false }) { Text("Save") } }, dismissButton = { TextButton(onClick = { editName = false }) { Text("Cancel") } })
    if (photoSheet) ModalBottomSheet(onDismissRequest = { photoSheet = false }) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Change photo", style = MaterialTheme.typography.titleLarge); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { avatarPresets.take(6).forEachIndexed { i, res -> Image(painterResource(res), avatarIds[i], Modifier.size(48.dp).clip(CircleShape).clickable { scope.launch { context.safeDataStoreEdit { it[UserAvatarKey] = avatarIds[i] } }; photoSheet = false }) } }; TextButton({ picker.launch("image/*"); photoSheet = false }) { Text("Upload your own photo") }; TextButton({ scope.launch { context.safeDataStoreEdit { it[UserAvatarKey] = "avatar_amber" }; File(context.filesDir, "profile/avatar.webp").delete() }; photoSheet = false }) { Text("Remove photo") } } }
    if (deleteDialog) AlertDialog(modifier = Modifier.padding(horizontal = 16.dp), properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = !wiping, dismissOnBackPress = !wiping), onDismissRequest = { if (!wiping) deleteDialog = false }, title = { Text("Delete all app data?") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("This erases your settings, library, history, sign-ins, name and photo. Your own music files are not touched. Huni will restart and show the welcome pages again."); OutlinedTextField(value = deleteText, onValueChange = { deleteText = it }, label = { Text("Type DELETE to confirm") }, singleLine = true, modifier = Modifier.fillMaxWidth()); if (wiping) Text("Wiping your Huni data…", color = MaterialTheme.colorScheme.primary); if (wipeError) Text("Couldn't delete everything. Please try again.", color = MaterialTheme.colorScheme.error) } }, confirmButton = { Button(enabled = deleteText.trim().equals("DELETE", true) && !wiping, onClick = { wiping = true; scope.launch { val ok = AppDataWiper.wipe(context); if (ok) { context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK); context.startActivity(this) }; (context as? android.app.Activity)?.finishAffinity() } else { wiping = false; wipeError = true } } }) { Text("Delete everything") } }, dismissButton = { TextButton(enabled = !wiping, onClick = { deleteDialog = false }) { Text("Cancel") } })
    TopAppBar(title = { com.metrolist.music.ui.component.HuniScreenTitle(stringResource(R.string.about)) }, navigationIcon = { IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) { Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.cd_back)) } }, scrollBehavior = scrollBehavior)
}

@Composable private fun ProfileAvatar(context: Context, id: String, size: Int) { Surface(Modifier.size(size.dp), CircleShape, MaterialTheme.colorScheme.surface, border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)) { if (id == "file") AsyncImage(File(context.filesDir, "profile/avatar.webp").toURI().toString(), "Your photo", Modifier.padding(2.dp).clip(CircleShape), contentScale = ContentScale.Crop) else Image(painterResource(avatarPresets.getOrElse(avatarIds.indexOf(id)) { 0 }), "Profile avatar", Modifier.padding(2.dp).clip(CircleShape), contentScale = ContentScale.Crop) } }
@Composable private fun CardColumn(content: @Composable ColumnScope.() -> Unit) { Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, content = { Column(content = content) }) }
@Composable private fun SectionLabel(text: String) { Text(text, Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium) }
@Composable private fun DividerRow() { HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant) }
@Composable private fun ActionRow(icon: Int, title: String, subtitle: String, tint: androidx.compose.ui.graphics.Color? = null, onClick: () -> Unit) { val rowTint = tint ?: MaterialTheme.colorScheme.onSurface; Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { Icon(painter = painterResource(icon), contentDescription = null, tint = rowTint, modifier = Modifier.size(22.dp)); Column(Modifier.weight(1f)) { Text(title, color = rowTint, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis) } } }
@Composable private fun UpstreamRow(name: String, license: String) { Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(name, Modifier.weight(1f)); Text(license, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } }

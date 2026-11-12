package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import com.metrolist.music.ui.component.HuniScreenTitle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.metrolist.music.BuildConfig
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.ReleaseNotesParser
import com.metrolist.music.utils.ReleaseInfo
import com.metrolist.music.utils.Updater
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.constants.CheckForUpdatesKey
import com.metrolist.music.constants.UpdateNotificationsEnabledKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdaterScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (checkForUpdates, onCheckForUpdatesChange) = rememberPreference(CheckForUpdatesKey, true)
    val (notifications, onNotificationsChange) = rememberPreference(UpdateNotificationsEnabledKey, true)
    var checking by remember { mutableStateOf(false) }
    var release by remember { mutableStateOf<ReleaseInfo?>(null) }
    var available by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var installError by remember { mutableStateOf<String?>(null) }
    var lastChecked by remember { mutableStateOf<Long?>(null) }
    val releaseNotes = remember(release?.description) {
        release?.description?.let { ReleaseNotesParser.parse(it) }
    }

    fun check(force: Boolean) {
        if (checking) return
        scope.launch {
            checking = true; error = false; installError = null
            val result = withContext(Dispatchers.IO) { Updater.checkForUpdate(force) }
            result.onSuccess { (info, update) ->
                release = info; available = update; lastChecked = Updater.lastCheckTime
            }.onFailure { error = true }
            checking = false
        }
    }
    LaunchedEffect(checkForUpdates) { if (checkForUpdates) check(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        Spacer(Modifier.height(4.dp))
        Material3SettingsGroup(
            title = "Current version",
            items = listOf(Material3SettingsItem(title = { Text("${BuildConfig.VERSION_NAME} · Universal") }, description = { Text("Package: ${BuildConfig.APPLICATION_ID}") })),
        )
        Spacer(Modifier.height(16.dp))
        Material3SettingsGroup(
            title = "Update settings",
            items = listOf(
                Material3SettingsItem(title = { Text("Check for updates") }, icon = painterResource(R.drawable.update), trailingContent = { com.metrolist.music.ui.component.HuniSwitch(checked = checkForUpdates, onCheckedChange = onCheckForUpdatesChange) }, onClick = { onCheckForUpdatesChange(!checkForUpdates) }),
                Material3SettingsItem(title = { Text("Update notifications") }, icon = painterResource(R.drawable.notification), trailingContent = { com.metrolist.music.ui.component.HuniSwitch(checked = notifications, onCheckedChange = onNotificationsChange) }, onClick = { onNotificationsChange(!notifications) }),
            ),
        )
        Spacer(Modifier.height(16.dp))
        Material3SettingsGroup(
            title = "Updates",
            items = listOf(Material3SettingsItem(
                icon = painterResource(R.drawable.refresh),
                title = { Text(if (checking) "Checking for updates…" else if (available && release != null) "Update available: v${release!!.versionName}" else "Check for updates") },
                description = { Text(if (error) "Couldn't check for updates. Try again." else if (!available && release != null) "You're on the latest version (${BuildConfig.BASE_VERSION_NAME})" else lastChecked?.let { "Last checked: ${DateFormat.getDateTimeInstance().format(Date(it))}" } ?: "GitHub releases/latest") },
                trailingContent = { if (checking) CircularProgressIndicator(Modifier.padding(end = 12.dp), strokeWidth = 2.dp) },
                onClick = { check(true) },
            )),
        )
        if (available && release != null) {
            Spacer(Modifier.height(16.dp))
            val sections = releaseNotes?.preambleSections.orEmpty() + releaseNotes?.entries.orEmpty().flatMap { it.sections }
            if (sections.isEmpty()) {
                Text(
                    ReleaseNotesParser.cleanDisplayText(release!!.description).ifBlank { "No release notes provided." },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                ) {
                    sections.forEach { section ->
                        section.heading?.let { heading ->
                            Text(
                                heading,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                        section.bullets.forEach { bullet ->
                            Text(
                                "• $bullet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            releaseNotes?.footer?.let { footer ->
                Text(
                    footer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                scope.launch {
                    installError = withContext(Dispatchers.IO) { Updater.downloadAndInstall(context, release!!).exceptionOrNull()?.message }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Download and install") }
        }
        installError?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(if (message.contains("signature", true)) "This update uses a new signing key. Uninstall Huni once and install it again. Your on-device data will be cleared." else "Couldn't check for updates. Try again.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(32.dp))
    }
    TopAppBar(title = { HuniScreenTitle(stringResource(R.string.updater)) }, navigationIcon = {
        IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) { Icon(painterResource(R.drawable.arrow_back), null) }
    })
}

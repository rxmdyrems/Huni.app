package com.metrolist.music.reader.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.request.ImageRequest
import com.metrolist.music.data.model.ExtensionMeta
import com.metrolist.music.data.model.ExtensionSourceMeta
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.launch

@Composable
fun ReaderExtensionsScreen(
    navController: NavController,
    extensionId: String? = null,
    viewModel: ReaderExtensionsViewModel = hiltViewModel(),
    readerViewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var repoDialog by remember { mutableStateOf(false) }
    var editingRepoUrl by remember { mutableStateOf<String?>(null) }
    var repoUrlDraft by rememberSaveable { mutableStateOf("") }
    var repoLabelDraft by rememberSaveable { mutableStateOf("") }
    var uninstallTarget by remember { mutableStateOf<ExtensionMeta?>(null) }
    var openingChapter by remember { mutableStateOf(false) }

    LaunchedEffect(extensionId, state.installed) {
        if (extensionId != null && state.selectedPackage != extensionId && state.installed.any { it.pkg == extensionId }) {
            viewModel.selectInstalled(extensionId)
        }
    }

    val topTitle = when {
        state.selectedSourceId != null -> state.selectedSourceName ?: "Source browser"
        state.selectedPackage != null -> state.installed.firstOrNull { it.pkg == state.selectedPackage }?.name ?: "Extension sources"
        else -> "Extensions"
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(topTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                TextButton(onClick = {
                    when {
                        state.selectedManga != null -> viewModel.backToResults()
                        state.selectedSourceId != null -> viewModel.backToSources()
                        state.selectedPackage != null -> {
                            if (extensionId != null) navController.navigateUp() else viewModel.backToSources()
                        }
                        else -> navController.navigateUp()
                    }
                }) { Text("Back") }
            },
        )
    }) { padding ->
        when {
            state.selectedSourceId != null -> SourceBrowser(
                state = state,
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize().padding(padding),
                openingChapter = openingChapter,
                onChapter = { chapter ->
                    val details = state.details ?: state.selectedManga
                    val sourceName = state.selectedSourceName ?: "Mihon source"
                    val referer = state.sourceReferer
                    openingChapter = true
                    scope.launch {
                        try {
                            val pageUrls = viewModel.pages(chapter)
                            val bookId = readerViewModel.importRemoteChapter(
                                title = "${details?.title ?: "Manga"} — ${chapter.name}",
                                sourceName = sourceName,
                                pageUrls = pageUrls,
                                referer = referer,
                            )
                            navController.navigate("reader/$bookId")
                        } catch (error: Exception) {
                            viewModel.showError(error.message ?: "Source may be broken. Try updating.")
                        } finally {
                            openingChapter = false
                        }
                    }
                },
            )
            state.selectedPackage != null -> SourceChooser(
                state.selectedPackage!!,
                state.sources,
                Modifier.fillMaxSize().padding(padding),
                onSource = viewModel::selectSource,
            )
            else -> Column(Modifier.fillMaxSize().padding(padding)) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Installed") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Browse") })
                }
                if (selectedTab == 0) {
                    InstalledExtensions(
                        state = state,
                        onOpen = { meta -> navController.navigate("reader/extensions/${meta.pkg}") },
                        onUninstall = { uninstallTarget = it },
                    )
                } else {
                    BrowseExtensions(
                        state = state,
                        viewModel = viewModel,
                        onEditRepo = { entry ->
                            editingRepoUrl = entry?.url
                            repoUrlDraft = entry?.url.orEmpty()
                            repoLabelDraft = entry?.label.orEmpty()
                            repoDialog = true
                        },
                    )
                }
            }
        }
    }

    if (repoDialog) {
        AlertDialog(
            onDismissRequest = { repoDialog = false },
            title = { Text(if (editingRepoUrl == null) "Add repository" else "Edit repository") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(repoLabelDraft, { repoLabelDraft = it }, label = { Text("Label") }, singleLine = true)
                    OutlinedTextField(repoUrlDraft, { repoUrlDraft = it }, label = { Text("index.json URL") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editingRepoUrl == null) viewModel.addRepo(repoUrlDraft.trim(), repoLabelDraft.trim())
                    else viewModel.updateRepo(editingRepoUrl!!, repoUrlDraft.trim(), repoLabelDraft.trim())
                    repoDialog = false
                }, enabled = repoUrlDraft.isNotBlank() && (editingRepoUrl != null || repoLabelDraft.isNotBlank())) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { repoDialog = false }) { Text("Cancel") } },
        )
    }

    uninstallTarget?.let { meta ->
        AlertDialog(
            onDismissRequest = { uninstallTarget = null },
            title = { Text("Uninstall ${meta.name}?") },
            text = { Text("The extension APK and its installed entry will be removed from this device.") },
            confirmButton = { TextButton(onClick = { viewModel.remove(meta.pkg); uninstallTarget = null }) { Text("Uninstall") } },
            dismissButton = { TextButton(onClick = { uninstallTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun InstalledExtensions(
    state: ReaderExtensionState,
    onOpen: (ExtensionMeta) -> Unit,
    onUninstall: (ExtensionMeta) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        ExtensionError(state.error)
        if (state.installed.isEmpty()) {
            Text("No extensions installed yet. Browse the catalog to install one.", Modifier.padding(top = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(state.installed, key = { it.pkg }) { meta ->
                    Card(Modifier.fillMaxWidth().combinedClickable(onClick = { onOpen(meta) }, onLongClick = { onUninstall(meta) })) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(meta.name, style = MaterialTheme.typography.titleMedium)
                                Text("${meta.lang} · v${meta.version} · hold to uninstall", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { onOpen(meta) }) { Text("Browse") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseExtensions(
    state: ReaderExtensionState,
    viewModel: ReaderExtensionsViewModel,
    onEditRepo: (com.metrolist.music.data.model.RepoEntry?) -> Unit,
) {
    val filtered = remember(state.catalog, state.catalogQuery) {
        val query = state.catalogQuery.trim().lowercase()
        if (query.isBlank()) state.catalog else state.catalog.filter {
            it.name.lowercase().contains(query) || it.pkg.lowercase().contains(query) || it.lang.lowercase().contains(query)
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.catalogQuery,
                onValueChange = viewModel::setCatalogQuery,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search extensions") },
                singleLine = true,
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Catalog", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = viewModel::refreshCatalog, enabled = !state.loading) { Text("Refresh") }
            }
            ExtensionError(state.error)
            if (state.loading && state.installingPkg == null) LinearLoading()
            if (!state.loading && state.catalog.isEmpty() && state.error == null) {
                Text("No extensions found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(filtered, key = { it.pkg }) { meta ->
            val installed = state.installed.any { it.pkg == meta.pkg }
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(meta.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${meta.lang} · v${meta.version}${if (meta.nsfw) " · NSFW" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    when {
                        installed -> Text("Installed", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp))
                        state.installingPkg == meta.pkg -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        else -> TextButton(onClick = { if (state.failedInstallPkg == meta.pkg) viewModel.retryInstall(meta.pkg) else viewModel.install(meta) }) { Text(if (state.failedInstallPkg == meta.pkg) "Retry" else "Install") }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Repositories", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { onEditRepo(null) }) { Text("Add") }
            }
        }
        items(state.repos, key = { it.url }) { repo ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(repo.label, style = MaterialTheme.typography.titleSmall)
                        Text(repo.url, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { onEditRepo(repo) }) { Text("Edit") }
                    TextButton(onClick = { viewModel.removeRepo(repo.url) }) { Text("Remove") }
                }
            }
        }
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun SourceChooser(
    pkg: String,
    sources: List<ExtensionSourceMeta>,
    modifier: Modifier,
    onSource: (String, ExtensionSourceMeta) -> Unit,
) {
    Column(modifier.padding(16.dp)) {
        Text("Choose a source", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        if (sources.isEmpty()) Text("No compatible sources are listed for this extension.", color = MaterialTheme.colorScheme.error)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sources, key = { it.id }) { source ->
                Card(Modifier.fillMaxWidth().clickable { onSource(pkg, source) }) {
                    Column(Modifier.padding(14.dp)) {
                        Text(source.name, style = MaterialTheme.typography.titleMedium)
                        Text("${source.lang} · ${source.baseUrl}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceBrowser(
    state: ReaderExtensionState,
    viewModel: ReaderExtensionsViewModel,
    modifier: Modifier,
    openingChapter: Boolean,
    onChapter: (SChapter) -> Unit,
) {
    Column(modifier.padding(horizontal = 16.dp)) {
        ExtensionError(state.error)
        val manga = state.selectedManga
        if (manga == null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.browseQuery,
                    onValueChange = viewModel::setBrowseQuery,
                    modifier = Modifier.weight(1f),
                    label = { Text("Search manga") },
                    singleLine = true,
                )
                Button(onClick = { viewModel.search() }, enabled = !state.loading) { Text("Search") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = viewModel::loadPopular, enabled = !state.loading) { Text("Popular") }
                TextButton(onClick = viewModel::nextPage, enabled = state.hasNextPage && !state.loading) { Text("Next page") }
            }
            if (state.loading) LinearLoading()
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.mangas, key = { it.url }) { item ->
                    Card(Modifier.fillMaxWidth().clickable { viewModel.openManga(item) }) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            item.thumbnail_url?.let { SourceImage(it, state.sourceReferer.orEmpty(), Modifier.size(64.dp)) }
                            Column {
                                Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                item.author?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
            }
        } else {
            TextButton(onClick = viewModel::backToResults, modifier = Modifier.align(Alignment.Start)) { Text("Back to results") }
            if (state.loading) LinearLoading()
            val details = state.details ?: manga
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        details.thumbnail_url?.let { SourceImage(it, state.sourceReferer.orEmpty(), Modifier.size(92.dp)) }
                        Column {
                            Text(details.title, style = MaterialTheme.typography.titleLarge)
                            details.author?.takeIf(String::isNotBlank)?.let { Text("Author: $it", style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                    details.description?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 8, overflow = TextOverflow.Ellipsis) }
                    Text("Chapters", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(state.chapters, key = { it.url }) { chapter ->
                    Card(Modifier.fillMaxWidth().clickable(enabled = !state.loading && !openingChapter) { onChapter(chapter) }) {
                        Text(chapter.name, Modifier.fillMaxWidth().padding(14.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (openingChapter) item { LinearLoading() }
                if (state.chapters.isEmpty() && !state.loading) item { Text("No chapters were returned by this source.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun SourceImage(url: String, referer: String, modifier: Modifier) {
    val context = LocalContext.current
    val request = remember(url, referer) {
        ImageRequest.Builder(context)
            .data(url)
            .httpHeaders(NetworkHeaders.Builder().set("Referer", referer).build())
            .build()
    }
    AsyncImage(model = request, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
}

@Composable
private fun ExtensionError(message: String?) {
    if (message != null) Text(message, Modifier.fillMaxWidth().padding(vertical = 8.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun LinearLoading() {
    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
    }
}

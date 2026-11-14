package com.metrolist.music.reader.ui

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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.music.data.model.ExtensionManga
import com.metrolist.music.data.model.ExtensionResult
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun ReaderExtensionsScreen(
    navController: NavController,
    extensionId: String? = null,
    viewModel: ReaderExtensionsViewModel = hiltViewModel(),
    readerViewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config = extensionId?.let(viewModel::config)
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable(extensionId) { mutableStateOf("") }
    var addedForRoute by remember(extensionId) { mutableStateOf(false) }
    var openingChapter by remember(extensionId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(extensionId, state.installed) {
        if (extensionId != null && config != null && !addedForRoute) {
            addedForRoute = true
            viewModel.loadPopular(extensionId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(config?.name ?: "Extensions") },
                navigationIcon = {
                    TextButton(onClick = { if (extensionId == null) navController.navigateUp() else navController.popBackStack() }) {
                        Text("Back")
                    }
                },
            )
        },
    ) { padding ->
        if (extensionId == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(12.dp))
                Text("JSON extensions use static HTML and CSS selectors only. Add sources you trust; sites and content rights vary.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { input = ""; showAddDialog = true }) { Text("Add extension") }
                Spacer(Modifier.height(8.dp))
                ExtensionError(state.error, viewModel::clearError)
                Spacer(Modifier.height(4.dp))
                if (state.installed.isEmpty()) {
                    Text("No extensions installed. Paste a JSON config or an HTTP(S) URL to a config/index.", modifier = Modifier.padding(top = 18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.installed, key = { it.id }) { extension ->
                            Card(Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth().clickable { navController.navigate("reader/extensions/${extension.id}") }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(extension.name, style = MaterialTheme.typography.titleMedium)
                                        Text("${extension.lang} · ${extension.baseUrl}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    TextButton(onClick = { viewModel.remove(extension.id) }) { Text("Remove") }
                                }
                            }
                        }
                    }
                }
            }
        } else if (config == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (state.installed.isEmpty()) "Loading extensions…" else "This extension is not installed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                ExtensionError(state.error, viewModel::clearError)
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                if (state.selectedManga == null) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Search manga") },
                            singleLine = true,
                        )
                        Button(enabled = !state.loading, onClick = { viewModel.search(extensionId, searchQuery) }) { Text("Search") }
                    }
                    TextButton(enabled = !state.loading, onClick = { searchQuery = ""; viewModel.loadPopular(extensionId) }) { Text("Popular") }
                    ExtensionError(state.error, viewModel::clearError)
                    if (state.loading) LinearLoading()
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.mangas, key = { it.link }) { manga -> MangaRow(manga) { viewModel.openManga(extensionId, manga) } }
                    }
                } else {
                    ExtensionError(state.error, viewModel::clearError)
                    val selected = requireNotNull(state.selectedManga)
                    val details = state.details
                    TextButton(onClick = viewModel::backToResults, modifier = Modifier.align(Alignment.Start)) { Text("Back to results") }
                    if (state.loading && details == null) LinearLoading()
                    if (state.loading && details != null) LinearLoading()
                    if (details != null) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            details.coverUrl?.let { AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(92.dp), contentScale = ContentScale.Crop) }
                            Column {
                                Text(details.title.ifBlank { selected.title }, style = MaterialTheme.typography.titleLarge)
                                Text("Author: ${details.author}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Text("Chapters", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (openingChapter) item(key = "opening-chapter") { LinearLoading() }
                            items(state.chapters, key = { it.link }) { chapter ->
                                Card(Modifier.fillMaxWidth().clickable(enabled = !state.loading && !openingChapter) {
                                    openingChapter = true
                                    scope.launch {
                                        try {
                                            when (val result = viewModel.pages(extensionId, chapter.link)) {
                                                is ExtensionResult.Error -> Unit
                                                is ExtensionResult.Success -> {
                                                    if (result.data.isEmpty()) viewModel.showError("No readable page images were found.")
                                                    else runCatching {
                                                        readerViewModel.importRemoteChapter(
                                                            title = "${details.title.ifBlank { selected.title }} — ${chapter.title}",
                                                            sourceName = config.name,
                                                            pageUrls = result.data,
                                                        )
                                                    }.onSuccess { bookId -> navController.navigate("reader/$bookId") }
                                                        .onFailure { viewModel.showError("Could not open chapter: ${it.message}") }
                                                }
                                            }
                                        } finally {
                                            openingChapter = false
                                        }
                                    }
                                }) { Text(chapter.title, Modifier.fillMaxWidth().padding(14.dp), maxLines = 2, overflow = TextOverflow.Ellipsis) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add JSON extension") },
            text = {
                Column {
                    Text("Paste one extension config, an index JSON, or an HTTP(S) URL to either.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("JSON or URL") }, minLines = 4, maxLines = 9, modifier = Modifier.fillMaxWidth())
                    ExtensionError(state.error, viewModel::clearError)
                    if (state.loading) LinearLoading()
                }
            },
            confirmButton = {
                TextButton(enabled = !state.loading && input.isNotBlank(), onClick = {
                    viewModel.add(input) { result ->
                        if (result.isSuccess) {
                            input = ""
                            showAddDialog = false
                        }
                    }
                }) { Text("Install") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MangaRow(manga: ExtensionManga, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            manga.coverUrl?.let { AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(64.dp), contentScale = ContentScale.Crop) }
            Text(manga.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ExtensionError(message: String?, onDismiss: () -> Unit) {
    if (message != null) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
private fun LinearLoading() {
    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp) }
}

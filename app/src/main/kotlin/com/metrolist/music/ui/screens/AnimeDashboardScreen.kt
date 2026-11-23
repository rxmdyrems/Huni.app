package com.metrolist.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import coil3.compose.AsyncImage
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.constants.AnimeContinueWatchingKey
import com.metrolist.music.constants.AnimeFavoritesKey
import com.metrolist.music.data.model.AnimeMediaItem
import com.metrolist.music.data.model.AnimeProviderMeta
import com.metrolist.music.ui.component.HuniScreenTitle
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.AnimeDashboardViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Composable
fun AnimeDashboardScreen(viewModel: AnimeDashboardViewModel = hiltViewModel()) {
    val installed by viewModel.installed.collectAsStateWithLifecycle()
    val repositories by viewModel.repositories.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val episodes by viewModel.episodes.collectAsStateWithLifecycle()
    val videoSource by viewModel.videoSource.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val selectedProvider by viewModel.selectedProviderId.collectAsStateWithLifecycle()
    val playerConnection = LocalPlayerConnection.current
    val (favoritesJson, setFavoritesJson) = rememberPreference(AnimeFavoritesKey, "[]")
    val (continueJson, setContinueJson) = rememberPreference(AnimeContinueWatchingKey, "[]")
    val favorites = remember(favoritesJson) {
        runCatching { Json.decodeFromString<List<AnimeMediaItem>>(favoritesJson) }.getOrDefault(emptyList())
    }
    val continueWatching = remember(continueJson) {
        runCatching { Json.decodeFromString<List<AnimeMediaItem>>(continueJson) }.getOrDefault(emptyList())
    }
    var query by rememberSaveable { mutableStateOf("") }
    var repoUrl by rememberSaveable { mutableStateOf("") }
    var selectedItemJson by rememberSaveable { mutableStateOf("") }
    val selectedItem = remember(selectedItemJson) {
        selectedItemJson.takeIf(String::isNotBlank)?.let { runCatching { Json.decodeFromString<AnimeMediaItem>(it) }.getOrNull() }
    }

    val playResolvedStream: () -> Unit = {
        val source = videoSource
        val item = selectedItem
        if (source != null && item != null && playerConnection != null) {
            runCatching {
                val mediaItem = MediaItem.Builder()
                    .setUri(source.url)
                    .setMimeType(if (source.container == "hls") MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(item.title)
                            .setArtist("Anime · Huni")
                            .build(),
                    )
                    .build()
                val httpFactory = DefaultHttpDataSource.Factory().setDefaultRequestProperties(source.headers)
                val mediaSource = DefaultMediaSourceFactory(httpFactory).createMediaSource(mediaItem)
                playerConnection.player.setMediaSource(mediaSource)
                playerConnection.player.prepare()
                playerConnection.player.play()
            }.onFailure { viewModel.reportError(it.message ?: "Could not start anime playback.") }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
    ) {
        HuniScreenTitle("Anime")
        Spacer(Modifier.height(8.dp))
        Text(stringResource(com.metrolist.music.R.string.anime_dashboard_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(20.dp))
        if (installed.any { it.enabled }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                installed.filter { it.enabled }.forEach { provider ->
                    Surface(
                        onClick = { viewModel.selectProvider(provider.meta.id) },
                        shape = RoundedCornerShape(50),
                        color = if (provider.meta.id == selectedProvider) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Text(provider.meta.name, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(com.metrolist.music.R.string.anime_search)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search(query) }),
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.search(query) }, enabled = !loading && selectedProvider != null) {
                Text(stringResource(com.metrolist.music.R.string.anime_search_button))
            }
        } else {
            EmptyStateCard(
                title = stringResource(com.metrolist.music.R.string.anime_no_providers),
                body = stringResource(com.metrolist.music.R.string.anime_no_providers_description),
            )
        }

        if (loading) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Loading provider…", style = MaterialTheme.typography.bodyMedium)
            }
        }
        error?.let { message ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { viewModel.retryLast() }) { Text("Retry") }
                }
            }
        }

        if (results.isNotEmpty()) {
            SectionHeading("Discover")
            results.forEach { item ->
                AnimeItemCard(
                    item = item,
                    favorite = favorites.any { it.sourceId == item.sourceId && it.id == item.id },
                    onClick = {
                        selectedItemJson = Json.encodeToString(item)
                        viewModel.loadEpisodes(item)
                        setContinueJson(Json.encodeToString((listOf(item) + continueWatching.filterNot { it.sourceId == item.sourceId && it.id == item.id }).take(12)))
                    },
                    onFavorite = { toggleFavorite(item, favorites, setFavoritesJson) },
                )
            }
        }

        selectedItem?.let { item ->
            SectionHeading(item.title)
            if (episodes.isEmpty() && !loading) {
                Text("No episodes are available yet. Retry or select another provider.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            episodes.forEach { episode ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.resolveVideo(episode) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(episode.number?.let { "${it.toInt()}. " }.orEmpty(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.width(8.dp))
                        Text(episode.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("Resolve", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (videoSource != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${videoSource?.quality?.ifBlank { videoSource?.container.orEmpty() }} stream ready", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = playResolvedStream) { Text("Play") }
                }
            }
        }

        if (continueWatching.isNotEmpty()) {
            SectionHeading("Continue watching")
            continueWatching.forEach { item ->
                AnimeItemCard(item, favorite = favorites.any { it.id == item.id && it.sourceId == item.sourceId }, onClick = {
                    selectedItemJson = Json.encodeToString(item)
                    viewModel.selectProvider(item.sourceId)
                    viewModel.loadEpisodes(item)
                }, onFavorite = { toggleFavorite(item, favorites, setFavoritesJson) })
            }
        }
        if (favorites.isNotEmpty()) {
            SectionHeading("Favorites")
            favorites.forEach { item ->
                AnimeItemCard(item, favorite = true, onClick = {
                    selectedItemJson = Json.encodeToString(item)
                    viewModel.selectProvider(item.sourceId)
                    viewModel.loadEpisodes(item)
                }, onFavorite = { toggleFavorite(item, favorites, setFavoritesJson) })
            }
        }

        SectionHeading("Provider repositories")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = repoUrl,
                onValueChange = { repoUrl = it },
                modifier = Modifier.weight(1f),
                label = { Text("HTTPS repository manifest URL") },
                singleLine = true,
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { viewModel.refreshRepository(repoUrl); repoUrl = "" }, enabled = !loading && repoUrl.startsWith("https://")) { Text("Add") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { viewModel.refreshAllRepositories() }, enabled = !loading && repositories.isNotEmpty()) { Text("Refresh") }
        }
        repositories.forEach { repo ->
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(repo.name, style = MaterialTheme.typography.titleSmall)
                            Text(repo.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(onClick = { viewModel.removeRepository(repo.url) }) { Text("Remove repo") }
                    }
                    repo.providers.forEach { provider ->
                        ProviderRow(
                            meta = provider,
                            installed = installed.any { it.meta.repositoryUrl == repo.url && it.meta.id == provider.id },
                            enabled = installed.firstOrNull { it.meta.repositoryUrl == repo.url && it.meta.id == provider.id }?.enabled ?: false,
                            onInstall = { viewModel.install(provider) },
                            onEnabledChange = { viewModel.setEnabled(provider, it) },
                            onRemove = { viewModel.remove(provider) },
                            busy = loading,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun EmptyStateCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(text, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp), style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun AnimeItemCard(
    item: AnimeMediaItem,
    favorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (item.cover.isNotBlank()) {
                AsyncImage(
                    model = item.cover,
                    contentDescription = item.title,
                    modifier = Modifier.size(width = 78.dp, height = 104.dp),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.description.isNotBlank()) Text(item.description, modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onFavorite) { Text(if (favorite) "Saved" else "Save") }
        }
    }
}

@Composable
private fun ProviderRow(
    meta: AnimeProviderMeta,
    installed: Boolean,
    enabled: Boolean,
    onInstall: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onRemove: () -> Unit,
    busy: Boolean,
) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("${meta.name} · ${meta.lang}", style = MaterialTheme.typography.bodyMedium)
            Text("v${meta.version}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when {
            !installed -> TextButton(onClick = onInstall, enabled = !busy) { Text("Install") }
            else -> {
                TextButton(onClick = { onEnabledChange(!enabled) }, enabled = !busy) { Text(if (enabled) "Disable" else "Enable") }
                TextButton(onClick = onRemove, enabled = !busy) { Text("Remove") }
            }
        }
    }
}

private fun toggleFavorite(
    item: AnimeMediaItem,
    favorites: List<AnimeMediaItem>,
    setRaw: (String) -> Unit,
) {
    val isSaved = favorites.any { it.sourceId == item.sourceId && it.id == item.id }
    val next = if (isSaved) favorites.filterNot { it.sourceId == item.sourceId && it.id == item.id }
    else (listOf(item) + favorites).take(100)
    setRaw(Json.encodeToString(next))
}

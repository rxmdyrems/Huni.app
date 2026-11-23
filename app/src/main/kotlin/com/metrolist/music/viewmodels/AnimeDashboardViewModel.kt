package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.data.engine.AnimeJavaScriptRuntime
import com.metrolist.music.data.model.AnimeEpisode
import com.metrolist.music.data.model.AnimeMediaItem
import com.metrolist.music.data.model.AnimeProviderMeta
import com.metrolist.music.data.model.AnimeProviderRepoCatalog
import com.metrolist.music.data.model.AnimeVideoSource
import com.metrolist.music.data.model.InstalledAnimeProvider
import com.metrolist.music.data.repository.AnimeProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class AnimeDashboardViewModel @Inject constructor(
    @ApplicationContext context: Context,
) : ViewModel() {
    private val repository = AnimeProviderRepository(context.filesDir)
    private val runtime = AnimeJavaScriptRuntime()

    private val _repositories = MutableStateFlow(repository.getRepositories())
    val repositories: StateFlow<List<AnimeProviderRepoCatalog>> = _repositories.asStateFlow()

    private val _installed = MutableStateFlow(repository.getInstalled())
    val installed: StateFlow<List<InstalledAnimeProvider>> = _installed.asStateFlow()

    private val _results = MutableStateFlow<List<AnimeMediaItem>>(emptyList())
    val results: StateFlow<List<AnimeMediaItem>> = _results.asStateFlow()

    private val _episodes = MutableStateFlow<List<AnimeEpisode>>(emptyList())
    val episodes: StateFlow<List<AnimeEpisode>> = _episodes.asStateFlow()

    private val _videoSource = MutableStateFlow<AnimeVideoSource?>(null)
    val videoSource: StateFlow<AnimeVideoSource?> = _videoSource.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedProviderId = MutableStateFlow(repository.getInstalled().firstOrNull { it.enabled }?.meta?.id)
    val selectedProviderId: StateFlow<String?> = _selectedProviderId.asStateFlow()
    private var retryAction: (() -> Unit)? = null

    fun selectProvider(id: String) {
        if (_installed.value.any { it.enabled && it.meta.id == id }) {
            _selectedProviderId.value = id
            _results.value = emptyList()
            _episodes.value = emptyList()
            _videoSource.value = null
        }
    }

    fun refreshRepository(url: String) {
        if (_loading.value) return
        retryAction = { refreshRepository(url) }
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val refreshed = repository.refreshRepository(url).getOrThrow()
                _repositories.value = repository.getRepositories()
                if (refreshed.providers.isEmpty()) _error.value = "This repository has no compatible anime providers."
            } catch (error: Exception) {
                _error.value = error.message ?: "Could not refresh provider repository."
            } finally {
                _loading.value = false
            }
        }
    }

    fun refreshAllRepositories() {
        val urls = _repositories.value.map { it.url }
        if (urls.isEmpty()) return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                urls.forEach { url ->
                    runCatching { repository.refreshRepository(url).getOrThrow() }
                        .onFailure { _error.value = it.message ?: "Repository refresh failed." }
                }
                _repositories.value = repository.getRepositories()
            } finally {
                _loading.value = false
            }
        }
    }

    fun install(meta: AnimeProviderMeta) {
        if (_loading.value) return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val collision = _installed.value.firstOrNull { it.meta.id == meta.id && it.meta.repositoryUrl != meta.repositoryUrl }
                require(collision == null) { "Another installed repository already uses provider ID '${meta.id}'." }
                repository.install(meta)
                refreshInstalled()
                if (_selectedProviderId.value == null) _selectedProviderId.value = meta.id
            } catch (error: Exception) {
                _error.value = error.message ?: "Could not install provider."
            } finally {
                _loading.value = false
            }
        }
    }

    fun setEnabled(meta: AnimeProviderMeta, enabled: Boolean) {
        runCatching { repository.setEnabled(meta.repositoryUrl, meta.id, enabled) }
            .onFailure { _error.value = it.message ?: "Could not change provider state." }
        refreshInstalled()
        if (!enabled && _selectedProviderId.value == meta.id) {
            _selectedProviderId.value = _installed.value.firstOrNull { it.enabled }?.meta?.id
            _results.value = emptyList()
        }
    }

    fun remove(meta: AnimeProviderMeta) {
        runCatching { repository.removeProvider(meta.repositoryUrl, meta.id) }
            .onFailure { _error.value = it.message ?: "Could not remove provider." }
        refreshInstalled()
        if (_selectedProviderId.value == meta.id) _selectedProviderId.value = _installed.value.firstOrNull { it.enabled }?.meta?.id
        _results.value = emptyList()
    }

    fun removeRepository(url: String) {
        runCatching { repository.removeRepository(url) }
            .onFailure { _error.value = it.message ?: "Could not remove repository." }
        _repositories.value = repository.getRepositories()
    }

    fun search(query: String) {
        val provider = selectedProvider() ?: run {
            _error.value = "Install and select an anime provider to search."
            return
        }
        if (_loading.value) return
        retryAction = { search(query) }
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _results.value = withContext(Dispatchers.IO) {
                    synchronized(runtime) {
                        runtime.load(provider.meta, repository.providerFile(provider.meta.id).readText())
                        runtime.search(query.trim(), 1).map { it.copy(sourceId = provider.meta.id) }
                    }
                }
                if (_results.value.isEmpty()) _error.value = "No anime found. Try another search or provider."
            } catch (error: Exception) {
                _error.value = "${provider.meta.name}: ${error.message ?: "Provider request failed."}"
                _results.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadEpisodes(item: AnimeMediaItem) {
        val provider = _installed.value.firstOrNull { it.enabled && it.meta.id == item.sourceId }
            ?: return setError("The selected provider is no longer enabled.")
        if (_loading.value) return
        retryAction = { loadEpisodes(item) }
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _episodes.value = withContext(Dispatchers.IO) {
                    synchronized(runtime) {
                        runtime.load(provider.meta, repository.providerFile(provider.meta.id).readText())
                        runtime.episodes(item.url)
                    }
                }
                if (_episodes.value.isEmpty()) _error.value = "This provider did not return any episodes."
            } catch (error: Exception) {
                _error.value = "${provider.meta.name}: ${error.message ?: "Could not load episodes."}"
                _episodes.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun resolveVideo(episode: AnimeEpisode) {
        val providerId = episode.sourceId.takeIf(String::isNotBlank) ?: _selectedProviderId.value
        val provider = _installed.value.firstOrNull { it.enabled && it.meta.id == providerId }
            ?: return setError("The episode's provider is no longer enabled.")
        if (_loading.value) return
        retryAction = { resolveVideo(episode) }
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _videoSource.value = null
            try {
                _videoSource.value = withContext(Dispatchers.IO) {
                    synchronized(runtime) {
                        runtime.load(provider.meta, repository.providerFile(provider.meta.id).readText())
                        runtime.videoSources(episode.url).firstOrNull()
                    }
                }
                if (_videoSource.value == null) _error.value = "The provider did not resolve a playable stream."
            } catch (error: Exception) {
                _error.value = "${provider.meta.name}: ${error.message ?: "Stream resolution failed."}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() { _error.value = null }

    fun reportError(message: String) { _error.value = message }

    fun retryLast() {
        val retry = retryAction
        if (retry == null) refreshAllRepositories() else retry()
    }

    private fun selectedProvider(): InstalledAnimeProvider? =
        _installed.value.firstOrNull { it.enabled && it.meta.id == _selectedProviderId.value }

    private fun refreshInstalled() {
        _installed.value = repository.getInstalled()
        if (_installed.value.none { it.enabled && it.meta.id == _selectedProviderId.value }) {
            _selectedProviderId.value = _installed.value.firstOrNull { it.enabled }?.meta?.id
        }
    }

    private fun setError(message: String) { _error.value = message }

    override fun onCleared() {
        runtime.close()
        repository.close()
    }
}

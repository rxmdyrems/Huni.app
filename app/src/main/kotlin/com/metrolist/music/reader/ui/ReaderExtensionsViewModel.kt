package com.metrolist.music.reader.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.data.engine.ExtensionEngine
import com.metrolist.music.data.model.ExtensionChapter
import com.metrolist.music.data.model.ExtensionConfig
import com.metrolist.music.data.model.ExtensionManga
import com.metrolist.music.data.model.ExtensionMangaDetails
import com.metrolist.music.data.model.ExtensionResult
import com.metrolist.music.data.repository.ExtensionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


data class ReaderExtensionState(
    val installed: List<ExtensionConfig> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val mangas: List<ExtensionManga> = emptyList(),
    val selectedManga: ExtensionManga? = null,
    val details: ExtensionMangaDetails? = null,
    val chapters: List<ExtensionChapter> = emptyList(),
)

@HiltViewModel
class ReaderExtensionsViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    private val repository = ExtensionRepository(application.filesDir)
    private val engine = ExtensionEngine()
    private val mutableState = MutableStateFlow(ReaderExtensionState())
    val state = mutableState.asStateFlow()

    init { refreshInstalled() }

    fun config(id: String): ExtensionConfig? = state.value.installed.firstOrNull { it.id == id }

    fun refreshInstalled() {
        viewModelScope.launch {
            runCatching { repository.installed() }
                .onSuccess { mutableState.value = mutableState.value.copy(installed = it, error = null) }
                .onFailure { showError("Could not load installed extensions: ${it.message}") }
        }
    }

    fun add(input: String, onComplete: (Result<List<ExtensionConfig>>) -> Unit) {
        mutableState.value = mutableState.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val result = runCatching { repository.addFromInput(input) }
            mutableState.value = mutableState.value.copy(loading = false, error = result.exceptionOrNull()?.message)
            result.onSuccess { refreshInstalled() }
            result.onFailure { showError(it.message ?: "Could not add extension.") }
            onComplete(result)
        }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            runCatching { repository.remove(id) }
                .onSuccess { refreshInstalled() }
                .onFailure { showError("Could not remove extension: ${it.message}") }
        }
    }

    fun loadPopular(extensionId: String) = runList(extensionId) { engine.popular(it) }

    fun search(extensionId: String, query: String) = runList(extensionId) { engine.search(it, query) }

    fun openManga(extensionId: String, manga: ExtensionManga) {
        val config = config(extensionId) ?: return showError("This extension is no longer installed.")
        mutableState.value = mutableState.value.copy(loading = true, error = null, selectedManga = manga, details = null, chapters = emptyList())
        viewModelScope.launch {
            when (val detailResult = engine.details(config, manga.link)) {
                is ExtensionResult.Error -> showError(detailResult.message)
                is ExtensionResult.Success -> {
                    when (val chapterResult = engine.chapters(config, manga.link)) {
                        is ExtensionResult.Error -> mutableState.value = mutableState.value.copy(loading = false, details = detailResult.data, error = chapterResult.message)
                        is ExtensionResult.Success -> mutableState.value = mutableState.value.copy(loading = false, details = detailResult.data, chapters = chapterResult.data, error = null)
                    }
                }
            }
        }
    }

    suspend fun pages(extensionId: String, chapterUrl: String): ExtensionResult<List<String>> {
        val source = config(extensionId) ?: return ExtensionResult.Error("This extension is no longer installed.")
        mutableState.value = mutableState.value.copy(loading = true, error = null)
        val result = engine.pages(source, chapterUrl)
        mutableState.value = when (result) {
            is ExtensionResult.Success -> mutableState.value.copy(loading = false)
            is ExtensionResult.Error -> mutableState.value.copy(loading = false, error = result.message)
        }
        return result
    }

    fun backToResults() {
        mutableState.value = mutableState.value.copy(selectedManga = null, details = null, chapters = emptyList(), error = null)
    }

    fun showError(message: String) {
        mutableState.value = mutableState.value.copy(loading = false, error = message.ifBlank { "Request failed. Please try again." })
    }

    fun clearError() { mutableState.value = mutableState.value.copy(error = null) }

    private fun runList(extensionId: String, request: suspend (ExtensionConfig) -> ExtensionResult<List<ExtensionManga>>) {
        val config = config(extensionId) ?: return showError("This extension is no longer installed.")
        mutableState.value = mutableState.value.copy(loading = true, error = null, selectedManga = null, details = null, chapters = emptyList())
        viewModelScope.launch {
            when (val result = request(config)) {
                is ExtensionResult.Success -> mutableState.value = mutableState.value.copy(loading = false, mangas = result.data, error = null)
                is ExtensionResult.Error -> mutableState.value = mutableState.value.copy(loading = false, mangas = emptyList(), error = result.message)
            }
        }
    }

    override fun onCleared() {
        engine.close()
        repository.close()
        super.onCleared()
    }
}

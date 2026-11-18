package com.metrolist.music.reader.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.data.engine.ExtensionRuntime
import com.metrolist.music.data.model.ExtensionMeta
import com.metrolist.music.data.model.ExtensionSourceMeta
import com.metrolist.music.data.model.RepoEntry
import com.metrolist.music.data.repository.ExtensionRepository
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


data class ReaderExtensionState(
    val installed: List<ExtensionMeta> = emptyList(),
    val catalog: List<ExtensionMeta> = emptyList(),
    val repos: List<RepoEntry> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val catalogQuery: String = "",
    val installingPkg: String? = null,
    val failedInstallPkg: String? = null,
    val selectedPackage: String? = null,
    val sources: List<ExtensionSourceMeta> = emptyList(),
    val selectedSourceId: Long? = null,
    val selectedSourceName: String? = null,
    val sourceReferer: String? = null,
    val mangas: List<SManga> = emptyList(),
    val selectedManga: SManga? = null,
    val details: SManga? = null,
    val chapters: List<SChapter> = emptyList(),
    val page: Int = 1,
    val hasNextPage: Boolean = false,
    val browseQuery: String = "",
)

@HiltViewModel
class ReaderExtensionsViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    private val repository = ExtensionRepository(application.filesDir)
    private val runtime = ExtensionRuntime(application.applicationContext)
    private val mutableState = MutableStateFlow(ReaderExtensionState())
    val state = mutableState.asStateFlow()

    init {
        refreshInstalledAndRepos()
        refreshCatalog()
    }

    fun refreshInstalledAndRepos() {
        mutableState.value = mutableState.value.copy(
            installed = repository.getInstalled(),
            repos = repository.getRepos(),
        )
    }

    fun refreshCatalog() {
        mutableState.value = mutableState.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val repos = withContext(Dispatchers.IO) { repository.getRepos() }
            val items = mutableListOf<ExtensionMeta>()
            var failed = repos.isEmpty()
            for (repo in repos) {
                val result = repository.fetchCatalog(repo.url)
                result.fold(
                    onSuccess = { items += it },
                    onFailure = { failed = true },
                )
            }
            mutableState.value = mutableState.value.copy(
                loading = false,
                repos = repos,
                catalog = items.distinctBy { it.pkg }.sortedBy { it.name.lowercase() },
                error = if (failed && items.isEmpty()) "Failed to load extensions." else null,
            )
        }
    }

    fun setCatalogQuery(query: String) {
        mutableState.value = mutableState.value.copy(catalogQuery = query)
    }

    fun addRepo(url: String, label: String) {
        runRepoChange { repository.addRepo(url, label) }
    }

    fun removeRepo(url: String) {
        runRepoChange { repository.removeRepo(url) }
    }

    fun updateRepo(url: String, newUrl: String, label: String) {
        runRepoChange { repository.updateRepo(url, newUrl, label) }
    }

    private fun runRepoChange(change: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching(change).onFailure { showError("Could not update repositories.") }
            refreshInstalledAndRepos()
            refreshCatalog()
        }
    }

    fun install(meta: ExtensionMeta) {
        if (mutableState.value.installingPkg == meta.pkg) return
        mutableState.value = mutableState.value.copy(loading = true, installingPkg = meta.pkg, failedInstallPkg = null, error = null)
        viewModelScope.launch {
            val apk = try {
                repository.downloadExtension(meta)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(loading = false, installingPkg = null, failedInstallPkg = meta.pkg, error = "Download failed.")
                return@launch
            }
            try {
                withContext(Dispatchers.IO) { runtime.loadSources(apk, meta.pkg) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: LinkageError) {
                handleInstallLoadFailure(meta, error)
                return@launch
            } catch (error: Exception) {
                handleInstallLoadFailure(meta, error)
                return@launch
            }

            try {
                withContext(Dispatchers.IO) { repository.markInstalled(meta) }
                mutableState.value = mutableState.value.copy(
                    loading = false,
                    installingPkg = null,
                    failedInstallPkg = null,
                    installed = withContext(Dispatchers.IO) { repository.getInstalled() },
                    error = null,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                runtime.remove(meta.pkg)
                Timber.e(error, "Could not save installed reader extension %s", meta.pkg)
                showError("Extension loaded, but Huni couldn't save it. The downloaded APK was kept; retry the install.")
                mutableState.value = mutableState.value.copy(installingPkg = null, failedInstallPkg = meta.pkg)
            }
        }
    }

    private fun handleInstallLoadFailure(meta: ExtensionMeta, error: Throwable) {
        runtime.remove(meta.pkg)
        Timber.e(error, "Could not load reader extension %s", meta.pkg)
        showError(installFailureMessage(error))
        mutableState.value = mutableState.value.copy(installingPkg = null, failedInstallPkg = meta.pkg)
    }

    private fun installFailureMessage(error: Throwable): String {
        val chain = generateSequence(error) { it.cause }.toList()
        val message = chain.joinToString(" ") { it.message.orEmpty() }
        val missingClass = chain.firstNotNullOfOrNull { cause ->
            when (cause) {
                is ClassNotFoundException -> cause.message
                is NoClassDefFoundError -> cause.message
                else -> null
            }
        }?.let { raw ->
            Regex("L([^;]+);").find(raw)?.groupValues?.get(1)?.replace('/', '.') ?: raw
        }
        return when {
            missingClass != null -> "Missing extension runtime class: ${missingClass.take(160)}"
            message.contains("package does not match", ignoreCase = true) ->
                "Downloaded APK package does not match the repository entry."
            message.contains("source", ignoreCase = true) && message.contains("declare", ignoreCase = true) ->
                "Extension does not declare a supported reader source."
            else -> "Could not load extension: ${error.message?.takeIf(String::isNotBlank) ?: "unknown error"}"
        }
    }

    fun retryInstall(pkg: String) {
        mutableState.value.catalog.firstOrNull { it.pkg == pkg }?.let(::install)
    }

    fun remove(pkg: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.removeExtension(pkg) }
                .onSuccess {
                    runtime.remove(pkg)
                    mutableState.value = mutableState.value.copy(
                        installed = repository.getInstalled(),
                        selectedPackage = mutableState.value.selectedPackage.takeUnless { it == pkg },
                        sources = if (mutableState.value.selectedPackage == pkg) emptyList() else mutableState.value.sources,
                        selectedSourceId = if (mutableState.value.selectedPackage == pkg) null else mutableState.value.selectedSourceId,
                    )
                }
                .onFailure { showError("Could not uninstall extension.") }
        }
    }

    fun selectInstalled(pkg: String) {
        val extension = mutableState.value.installed.firstOrNull { it.pkg == pkg }
        if (extension == null) return showError("Extension is not installed.")
        mutableState.value = mutableState.value.copy(
            selectedPackage = pkg,
            sources = extension.sources,
            selectedSourceId = null,
            selectedSourceName = null,
            mangas = emptyList(),
            selectedManga = null,
            details = null,
            chapters = emptyList(),
            error = null,
        )
    }

    fun selectSource(pkg: String, source: ExtensionSourceMeta) {
        val sourceId = source.id.toLongOrNull()
        if (sourceId == null) return showError("Extension source ID is invalid. Refresh the repository and retry.")
        mutableState.value = mutableState.value.copy(
            selectedPackage = pkg,
            selectedSourceId = sourceId,
            selectedSourceName = source.name,
            sourceReferer = source.baseUrl.takeIf(String::isNotBlank),
            mangas = emptyList(),
            selectedManga = null,
            details = null,
            chapters = emptyList(),
            page = 1,
            browseQuery = "",
            error = null,
        )
        loadPopular()
    }

    fun setBrowseQuery(query: String) {
        mutableState.value = mutableState.value.copy(browseQuery = query)
    }

    fun loadPopular() = loadMangaPage(1, "")

    fun search(query: String = mutableState.value.browseQuery, page: Int = 1, append: Boolean = false) {
        if (query.isBlank()) return loadPopular()
        loadMangaPage(page, query, append)
    }

    fun nextPage() {
        val state = mutableState.value
        if (!state.hasNextPage || state.loading) return
        if (state.browseQuery.isBlank()) loadMangaPage(state.page + 1, "", append = true)
        else search(state.browseQuery, state.page + 1, append = true)
    }

    private fun loadMangaPage(page: Int, query: String, append: Boolean = false) {
        val selection = selection() ?: return
        mutableState.value = mutableState.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val source = sourceFor(selection.first, selection.second)
                val result = if (query.isBlank()) {
                    runtime.fetchPopular(selection.first.apkFile, selection.first.meta.pkg, source.id, page)
                } else {
                    runtime.fetchSearch(selection.first.apkFile, selection.first.meta.pkg, source.id, query, page)
                }
                mutableState.value = mutableState.value.copy(
                    loading = false,
                    mangas = if (append) mutableState.value.mangas + result.mangas else result.mangas,
                    page = page,
                    browseQuery = query,
                    hasNextPage = result.hasNextPage,
                    sourceReferer = source.baseUrl,
                    error = null,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                showError(sourceError(error))
                mutableState.value = mutableState.value.copy(mangas = emptyList(), hasNextPage = false)
            }
        }
    }

    fun openManga(manga: SManga) {
        val selection = selection() ?: return
        val sourceId = mutableState.value.selectedSourceId ?: return showError("Extension source is unavailable.")
        mutableState.value = mutableState.value.copy(loading = true, error = null, selectedManga = manga, details = null, chapters = emptyList())
        viewModelScope.launch {
            try {
                val details = runtime.fetchDetails(selection.first.apkFile, selection.first.meta.pkg, sourceId, manga)
                val chapters = runtime.fetchChapters(selection.first.apkFile, selection.first.meta.pkg, sourceId, details)
                mutableState.value = mutableState.value.copy(loading = false, details = details, chapters = chapters, error = null)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                showError(sourceError(error))
            }
        }
    }

    suspend fun pages(chapter: SChapter): List<String> {
        val selection = selection() ?: throw IllegalStateException("Extension source is unavailable.")
        val sourceId = mutableState.value.selectedSourceId ?: throw IllegalStateException("Extension source is unavailable.")
        mutableState.value = mutableState.value.copy(loading = true, error = null)
        return try {
            val resolved = runtime.fetchPages(selection.first.apkFile, selection.first.meta.pkg, sourceId, chapter)
                .mapNotNull { it.imageUrl?.takeIf(String::isNotBlank) ?: it.url.takeIf(String::isNotBlank) }
                .filter { it.startsWith("https://") || it.startsWith("http://") }
            mutableState.value = mutableState.value.copy(loading = false)
            if (resolved.isEmpty()) throw IllegalStateException("Source unavailable.")
            resolved
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            val message = sourceError(error)
            mutableState.value = mutableState.value.copy(loading = false, error = message)
            throw IllegalStateException(message, error)
        }
    }

    fun backToSources() {
        mutableState.value = mutableState.value.copy(
            selectedSourceId = null,
            selectedSourceName = null,
            mangas = emptyList(),
            selectedManga = null,
            details = null,
            chapters = emptyList(),
            error = null,
        )
    }

    fun backToResults() {
        mutableState.value = mutableState.value.copy(selectedManga = null, details = null, chapters = emptyList(), error = null)
    }

    fun showError(message: String) {
        mutableState.value = mutableState.value.copy(loading = false, error = message.ifBlank { "Source may be broken. Try updating." })
    }

    fun clearError() {
        mutableState.value = mutableState.value.copy(error = null)
    }

    private suspend fun sourceFor(installed: InstalledExtension, requestedId: Long): HttpSource = withContext(Dispatchers.IO) {
        runtime.loadSources(installed.apkFile, installed.meta.pkg).firstOrNull { it.id == requestedId }
            ?: throw IllegalStateException("Extension source is unavailable.")
    }

    private fun selection(): Pair<InstalledExtension, Long>? {
        val state = mutableState.value
        val pkg = state.selectedPackage ?: return null.also { showError("Choose an installed extension first.") }
        val id = state.selectedSourceId ?: return null.also { showError("Choose a source first.") }
        val meta = state.installed.firstOrNull { it.pkg == pkg }
            ?: return null.also { showError("Extension is not installed.") }
        return InstalledExtension(meta, repository.apkFile(pkg)) to id
    }

    private data class InstalledExtension(val meta: ExtensionMeta, val apkFile: java.io.File)

    private val ExtensionMeta.apkFile: java.io.File get() = repository.apkFile(pkg)

    private fun sourceError(error: Throwable): String {
        val chain = generateSequence(error) { it.cause }.toList()
        val message = chain.joinToString(" ") { it.message.orEmpty() }
        val diagnostic = chain.take(3).joinToString(" <- ") { cause ->
            "${cause::class.java.simpleName}: ${cause.message.orEmpty().take(180)}"
        }
        return when {
            chain.any { it is ClassNotFoundException || it is NoClassDefFoundError } ->
                "Extension classes are missing or use an unsupported API. Update Huni or refresh the repository.\n$diagnostic"
            message.contains("HTTP", ignoreCase = true) -> "Source could not be reached. Check your connection."
            else -> "Source may be broken. Try updating.\n$diagnostic"
        }
    }

    override fun onCleared() {
        runtime.close()
        repository.close()
        super.onCleared()
    }
}

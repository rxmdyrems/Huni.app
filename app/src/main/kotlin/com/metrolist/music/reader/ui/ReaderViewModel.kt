package com.metrolist.music.reader.ui

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.metrolist.music.data.engine.ExtensionRuntime
import com.metrolist.music.data.repository.ExtensionRepository
import com.metrolist.music.reader.db.ReaderBookEntity
import com.metrolist.music.reader.db.ReaderDatabase
import com.metrolist.music.reader.db.ReaderPageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.kanade.tachiyomi.source.model.SManga
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ReaderViewModel @Inject constructor(
    application: Application,
    private val database: ReaderDatabase,
) : AndroidViewModel(application) {
    private val dao = database.readerDao()
    private val extensionRepository = ExtensionRepository(application.filesDir)
    private val extensionRuntime = ExtensionRuntime(application.applicationContext)
    val books = dao.books().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun book(bookId: String) = dao.book(bookId)

    fun pages(bookId: String) = dao.pages(bookId)

    fun importUris(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            uris.forEach { importUri(getApplication<Application>().contentResolver, it) }
        }
    }

    suspend fun importRemoteChapter(
        title: String,
        sourceName: String,
        pageUrls: List<String>,
        referer: String? = null,
        storyId: String,
        storyTitle: String,
        chapterUrl: String,
        chapterName: String,
        mangaUrl: String,
        sourcePackage: String,
        sourceId: Long,
        coverUrl: String?,
    ): String = withContext(Dispatchers.IO) {
        require(pageUrls.isNotEmpty()) { "Chapter contains no readable pages." }
        require(pageUrls.all { it.startsWith("https://") || it.startsWith("http://") }) { "Page URL is not HTTP(S)." }
        val id = UUID.randomUUID().toString()
        database.withTransaction {
            dao.insertBook(
                ReaderBookEntity(
                    id = id,
                    title = title,
                    sourceName = sourceName,
                    referer = referer,
                    storyId = storyId,
                    storyTitle = storyTitle,
                    chapterUrl = chapterUrl,
                    chapterName = chapterName,
                    mangaUrl = mangaUrl,
                    sourcePackage = sourcePackage,
                    sourceId = sourceId,
                    coverUrl = coverUrl ?: pageUrls.firstOrNull(),
                ),
            )
            dao.insertPages(pageUrls.mapIndexed { index, url -> ReaderPageEntity(id, index, url) })
        }
        id
    }

    /** Fetch and import the next chapter in this installed source's chapter ordering. */
    suspend fun importNextChapter(bookId: String): String? = withContext(Dispatchers.IO) {
        val current = dao.bookOnce(bookId) ?: return@withContext null
        val storyId = current.storyId ?: return@withContext null
        val chapterUrl = current.chapterUrl ?: return@withContext null
        val mangaUrl = current.mangaUrl ?: return@withContext null
        val packageName = current.sourcePackage ?: return@withContext null
        val sourceId = current.sourceId ?: return@withContext null
        val extension = extensionRepository.getInstalled().firstOrNull { it.pkg == packageName }
            ?: throw IllegalStateException("The source extension is not installed. Reinstall it to continue this story.")
        val apkFile = extensionRepository.apkFile(packageName)
        val manga = SManga.create().apply {
            url = mangaUrl
            title = current.storyTitle ?: current.title.substringBefore(" — ")
            thumbnail_url = current.coverUrl.orEmpty()
        }
        val chapters = extensionRuntime.fetchChapters(apkFile, packageName, sourceId, manga)
        val currentIndex = chapters.indexOfFirst { it.url == chapterUrl }
        if (currentIndex < 0) return@withContext null
        val nextChapter = chapters.getOrNull(currentIndex + 1) ?: return@withContext null
        dao.chapterForStory(storyId, nextChapter.url)?.let { return@withContext it.id }

        val pageUrls = extensionRuntime.fetchPages(apkFile, packageName, sourceId, nextChapter)
            .mapNotNull { it.imageUrl?.takeIf { url -> url.startsWith("https://") || url.startsWith("http://") } }
        require(pageUrls.isNotEmpty()) { "The next chapter contains no readable pages." }
        val nextId = UUID.randomUUID().toString()
        val storyTitle = current.storyTitle ?: current.title.substringBefore(" — ")
        database.withTransaction {
            dao.insertBook(
                ReaderBookEntity(
                    id = nextId,
                    title = "$storyTitle — ${nextChapter.name}",
                    sourceName = current.sourceName,
                    referer = current.referer,
                    storyId = storyId,
                    storyTitle = storyTitle,
                    chapterUrl = nextChapter.url,
                    chapterName = nextChapter.name,
                    mangaUrl = mangaUrl,
                    sourcePackage = packageName,
                    sourceId = sourceId,
                    coverUrl = current.coverUrl ?: pageUrls.firstOrNull(),
                ),
            )
            dao.insertPages(pageUrls.mapIndexed { index, url -> ReaderPageEntity(nextId, index, url) })
        }
        nextId
    }

    /** Download all remote pages atomically per page, preserving partial progress for retry. */
    suspend fun downloadChapter(bookId: String) = withContext(Dispatchers.IO) {
        val book = dao.bookOnce(bookId) ?: throw IllegalArgumentException("Chapter is no longer in the library.")
        val pages = dao.pagesOnce(bookId)
        require(pages.isNotEmpty()) { "Chapter has no pages to download." }
        val directory = File(getApplication<Application>().filesDir, "reader/$bookId").apply {
            if (!exists() && !mkdirs()) throw IllegalStateException("Could not create offline chapter storage.")
        }
        var totalBytes = 0L
        for (page in pages) {
            val existing = File(page.filePath)
            if (!page.filePath.startsWith("http://") && !page.filePath.startsWith("https://")) {
                if (!existing.isFile) throw IllegalStateException("A local page is missing: ${page.pageIndex + 1}.")
                continue
            }
            val destination = File(directory, "offline-${page.pageIndex.toString().padStart(5, '0')}.img")
            if (destination.isFile && destination.length() > 0L) {
                totalBytes += destination.length()
                dao.updatePagePath(bookId, page.pageIndex, destination.absolutePath)
                continue
            }
            val temporary = File(directory, "${destination.name}.tmp")
            val connection = URL(page.filePath).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.setRequestProperty("User-Agent", "Huni-Reader/0.9.22")
                book.referer?.takeIf(String::isNotBlank)?.let { connection.setRequestProperty("Referer", it) }
                val status = connection.responseCode
                if (status !in 200..299) throw IllegalStateException("Page ${page.pageIndex + 1} download failed (HTTP $status).")
                var pageBytes = 0L
                connection.inputStream.use { input ->
                    temporary.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            pageBytes += count
                            require(pageBytes <= MAX_PAGE_BYTES) { "A page is larger than the offline download limit." }
                            require(totalBytes + pageBytes <= MAX_CHAPTER_BYTES) { "This chapter exceeds the offline storage limit." }
                            output.write(buffer, 0, count)
                        }
                    }
                }
                require(pageBytes > 0L) { "Page ${page.pageIndex + 1} downloaded empty." }
                if (!temporary.renameTo(destination)) throw IllegalStateException("Could not save page ${page.pageIndex + 1}.")
                totalBytes += pageBytes
                dao.updatePagePath(bookId, page.pageIndex, destination.absolutePath)
            } finally {
                connection.disconnect()
                temporary.delete()
            }
        }
        dao.markDownloaded(bookId, System.currentTimeMillis())
    }

    fun updateProgress(bookId: String, page: Int) {
        viewModelScope.launch { dao.updateProgress(bookId, page, System.currentTimeMillis()) }
    }

    fun deleteBook(book: ReaderBookEntity) {
        viewModelScope.launch(Dispatchers.IO) { deleteBookNow(book) }
    }

    fun deleteStory(chapters: List<ReaderBookEntity>) {
        viewModelScope.launch(Dispatchers.IO) { chapters.forEach(::deleteBookNow) }
    }

    private suspend fun deleteBookNow(book: ReaderBookEntity) {
        dao.deletePages(book.id)
        dao.deleteBook(book.id)
        File(getApplication<Application>().filesDir, "reader/${book.id}").deleteRecursively()
    }

    override fun onCleared() {
        extensionRuntime.close()
        extensionRepository.close()
        super.onCleared()
    }

    private suspend fun importUri(resolver: ContentResolver, uri: Uri) {
        val sourceName = resolver.getType(uri)?.let { mime ->
            resolver.query(uri, arrayOf("_display_name"), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } ?: uri.lastPathSegment ?: "Imported story"
        val bookId = UUID.randomUUID().toString()
        val bookDir = File(getApplication<Application>().filesDir, "reader/$bookId").apply { mkdirs() }
        val pages = if (sourceName.substringAfterLast('.', "").lowercase() in setOf("cbz", "zip")) {
            extractArchive(resolver, uri, bookDir, bookId)
        } else {
            val destination = File(bookDir, "page-0001.${extensionFor(resolver.getType(uri))}")
            resolver.openInputStream(uri)?.use { input -> destination.outputStream().use(input::copyTo) }
            listOf(ReaderPageEntity(bookId, 0, destination.absolutePath))
        }
        if (pages.isNotEmpty()) {
            dao.insertBook(
                ReaderBookEntity(
                    id = bookId,
                    title = sourceName.substringBeforeLast('.'),
                    sourceName = sourceName,
                    coverUrl = pages.firstOrNull()?.filePath,
                ),
            )
            dao.insertPages(pages)
        } else {
            bookDir.deleteRecursively()
        }
    }

    private fun extractArchive(
        resolver: ContentResolver,
        uri: Uri,
        bookDir: File,
        bookId: String,
    ): List<ReaderPageEntity> {
        val entries = mutableListOf<ReaderPageEntity>()
        resolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val extension = name.substringAfterLast('.', "").lowercase()
                    if (!entry.isDirectory && extension in setOf("jpg", "jpeg", "png", "webp", "gif")) {
                        val index = entries.size
                        val output = File(bookDir, "page-${index.toString().padStart(5, '0')}.$extension")
                        output.outputStream().use { zip.copyTo(it) }
                        entries += ReaderPageEntity(bookId, index, output.absolutePath)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        return entries
    }

    private fun extensionFor(mime: String?): String = when (mime) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        else -> "jpg"
    }

    private companion object {
        const val MAX_PAGE_BYTES = 64L * 1024 * 1024
        const val MAX_CHAPTER_BYTES = 512L * 1024 * 1024
    }
}

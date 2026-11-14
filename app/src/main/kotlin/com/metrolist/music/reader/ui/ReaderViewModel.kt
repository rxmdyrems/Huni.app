package com.metrolist.music.reader.ui

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.room.withTransaction
import androidx.lifecycle.viewModelScope
import com.metrolist.music.reader.db.ReaderBookEntity
import com.metrolist.music.reader.db.ReaderDatabase
import com.metrolist.music.reader.db.ReaderPageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
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
    val books = dao.books().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun book(bookId: String) = dao.book(bookId)

    fun pages(bookId: String) = dao.pages(bookId)

    fun importUris(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            uris.forEach { importUri(getApplication<Application>().contentResolver, it) }
        }
    }

    suspend fun importRemoteChapter(title: String, sourceName: String, pageUrls: List<String>): String = withContext(Dispatchers.IO) {
        require(pageUrls.isNotEmpty()) { "Chapter contains no readable pages." }
        require(pageUrls.all { it.startsWith("https://") || it.startsWith("http://") }) { "Page URL is not HTTP(S)." }
        val id = UUID.randomUUID().toString()
        database.withTransaction {
            dao.insertBook(ReaderBookEntity(id, title, sourceName))
            dao.insertPages(pageUrls.mapIndexed { index, url -> ReaderPageEntity(id, index, url) })
        }
        id
    }

    fun updateProgress(bookId: String, page: Int) {
        viewModelScope.launch { dao.updateProgress(bookId, page) }
    }

    fun deleteBook(book: ReaderBookEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deletePages(book.id)
            dao.deleteBook(book.id)
            File(getApplication<Application>().filesDir, "reader/${book.id}").deleteRecursively()
        }
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
            dao.insertBook(ReaderBookEntity(bookId, sourceName.substringBeforeLast('.'), sourceName))
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
}

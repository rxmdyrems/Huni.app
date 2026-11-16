package com.metrolist.music.data.engine

import android.content.Context
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mihon.kit.MihonKit
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/** Dynamic extension APK adapter. All executable source code is loaded by mihon-kit. */
class ExtensionRuntime(private val context: Context) {
    /** Primary HttpSource cache keyed by extension package, as required by the reader API. */
    private val sourceCache = ConcurrentHashMap<String, HttpSource>()
    private val sourcesByPackage = ConcurrentHashMap<String, List<HttpSource>>()

    fun loadSource(apkFile: File, pkg: String): HttpSource = loadSources(apkFile, pkg).firstOrNull()
        ?: throw IllegalArgumentException("Extension does not provide an HTTP source supported by the reader.")

    fun loadSources(apkFile: File, pkg: String): List<HttpSource> {
        sourceCache[pkg]?.let { cached -> return sourcesByPackage[pkg] ?: listOf(cached) }
        sourcesByPackage[pkg]?.let { return it }
        synchronized(this) {
            sourceCache[pkg]?.let { cached -> return sourcesByPackage[pkg] ?: listOf(cached) }
            sourcesByPackage[pkg]?.let { return it }
            // Android rejects loading DEX from a writable APK on recent releases.
            // Downloaded extensions live in app-private storage, so make the APK
            // immutable before handing its path to MihonKit's dynamic loader.
            if (apkFile.canWrite() && !apkFile.setReadOnly()) {
                throw IOException("Could not make extension APK read-only: ${apkFile.name}")
            }
            val loaded = runBlocking(Dispatchers.IO) { MihonKit.loadExtensionApk(context.applicationContext, apkFile) }
            require(loaded.packageName == pkg) { "Extension package does not match the catalog." }
            val sources = loaded.sources.filterIsInstance<HttpSource>()
            require(sources.isNotEmpty()) { "Extension does not provide an HTTP source supported by the reader." }
            sourcesByPackage[pkg] = sources
            sourceCache[pkg] = sources.first()
            return sources
        }
    }

    fun sourceFor(apkFile: File, pkg: String, sourceId: Long): HttpSource {
        val sources = loadSources(apkFile, pkg)
        return sources.firstOrNull { it.id == sourceId }
            ?: throw IllegalArgumentException("Extension source is unavailable.")
    }

    suspend fun fetchPopular(apkFile: File, pkg: String, sourceId: Long, page: Int = 1): MangasPage =
        withContext(Dispatchers.IO) { sourceFor(apkFile, pkg, sourceId).getPopularManga(page) }

    suspend fun fetchSearch(apkFile: File, pkg: String, sourceId: Long, query: String, page: Int = 1): MangasPage =
        withContext(Dispatchers.IO) {
            val source = sourceFor(apkFile, pkg, sourceId)
            source.getSearchManga(page, query, source.getFilterList() ?: FilterList())
        }

    suspend fun fetchDetails(apkFile: File, pkg: String, sourceId: Long, manga: SManga): SManga =
        withContext(Dispatchers.IO) {
            sourceFor(apkFile, pkg, sourceId).getMangaUpdate(manga, emptyList(), true, false).manga
        }

    suspend fun fetchChapters(apkFile: File, pkg: String, sourceId: Long, manga: SManga): List<SChapter> =
        withContext(Dispatchers.IO) {
            sourceFor(apkFile, pkg, sourceId).getMangaUpdate(manga, emptyList(), false, true).chapters
        }

    suspend fun fetchPages(apkFile: File, pkg: String, sourceId: Long, chapter: SChapter): List<Page> =
        withContext(Dispatchers.IO) {
            val source = sourceFor(apkFile, pkg, sourceId)
            source.getPageList(chapter).map { page ->
                if (page.imageUrl.isNullOrBlank()) page.imageUrl = source.getImageUrl(page)
                page
            }
        }

    fun remove(pkg: String) {
        sourcesByPackage.remove(pkg)
        sourceCache.remove(pkg)
    }

    fun close() {
        sourcesByPackage.clear()
        sourceCache.clear()
    }
}

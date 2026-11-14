package com.metrolist.music.data.engine

import com.metrolist.music.data.model.ExtensionChapter
import com.metrolist.music.data.model.ExtensionConfig
import com.metrolist.music.data.model.ExtensionManga
import com.metrolist.music.data.model.ExtensionMangaDetails
import com.metrolist.music.data.model.ExtensionResult
import com.metrolist.music.data.model.MangaSelectors
import com.metrolist.music.data.model.validationError
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ExtensionEngine(
    private val client: HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        install(HttpTimeout) { requestTimeoutMillis = 20_000; connectTimeoutMillis = 10_000; socketTimeoutMillis = 20_000 }
    },
) {
    suspend fun popular(config: ExtensionConfig): ExtensionResult<List<ExtensionManga>> =
        list(config, config.popular.url, config.popular.selectors, emptyMap())

    suspend fun search(config: ExtensionConfig, query: String): ExtensionResult<List<ExtensionManga>> {
        if (query.isBlank()) return ExtensionResult.Error("Enter a search term.")
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        return list(config, config.search.url, config.search.selectors, mapOf("query" to encoded))
    }

    suspend fun details(config: ExtensionConfig, mangaUrl: String): ExtensionResult<ExtensionMangaDetails> =
        safe(config) {
            val requestUrl = resolve(config, config.details.url, mapOf("mangaUrl" to mangaUrl))
            val document = getDocument(requestUrl)
            val selectors = config.details.selectors
            ExtensionMangaDetails(
                title = requiredText(document, selectors.title, "details title"),
                author = requiredText(document, selectors.author, "details author"),
                coverUrl = requiredUrl(document, selectors.cover, requestUrl, "details cover"),
            )
        }

    suspend fun chapters(config: ExtensionConfig, mangaUrl: String): ExtensionResult<List<ExtensionChapter>> =
        safe(config) {
            val requestUrl = resolve(config, config.chapters.url, mapOf("mangaUrl" to mangaUrl))
            val document = getDocument(requestUrl)
            val selectors = config.chapters.selectors
            val titles = requiredElements(document, selectors.title, "chapter title")
            val links = requiredElements(document, selectors.link, "chapter link")
            if (titles.size != links.size) throw SelectorException("Chapter title and link selectors matched different numbers of elements.")
            titles.indices.mapNotNull { index ->
                val title = elementText(titles[index]).takeIf(String::isNotBlank)
                val link = elementUrl(links[index], document.location())
                if (title == null || link == null) null else ExtensionChapter(title, link)
            }.ifEmpty { throw SelectorException("No usable chapters were found.") }
        }

    suspend fun pages(config: ExtensionConfig, chapterUrl: String): ExtensionResult<List<String>> =
        safe(config) {
            val requestUrl = resolve(config, config.pages.url, mapOf("chapterUrl" to chapterUrl))
            val document = getDocument(requestUrl)
            val selector = config.pages.selectors.image
            val elements = requiredElements(document, selector, "page image")
            elements.mapNotNull { elementImageUrl(it, document.location()) }
                .distinct()
                .ifEmpty { throw SelectorException("The page image selector matched no usable image URLs.") }
        }

    fun close() = client.close()

    private suspend fun list(
        config: ExtensionConfig,
        endpoint: String,
        selectors: MangaSelectors,
        variables: Map<String, String>,
    ): ExtensionResult<List<ExtensionManga>> = safe(config) {
        val requestUrl = resolve(config, endpoint, variables)
        val document = getDocument(requestUrl)
        val titles = requiredElements(document, selectors.title, "manga title")
        val links = requiredElements(document, selectors.link, "manga link")
        val covers = requiredElements(document, selectors.cover, "manga cover")
        if (titles.size != links.size) throw SelectorException("Manga title and link selectors matched different numbers of elements.")
        if (covers.size != titles.size) throw SelectorException("Manga cover selector matched a different number of elements than title selectors.")
        titles.indices.mapNotNull { index ->
            val title = elementText(titles[index]).takeIf(String::isNotBlank)
            val link = elementUrl(links[index], document.location())
            if (title == null || link == null) null else ExtensionManga(
                title = title,
                coverUrl = elementImageUrl(covers[index], document.location())
                    ?: throw SelectorException("The manga cover selector matched an element without an image URL."),
                link = link,
            )
        }.ifEmpty { throw SelectorException("No usable manga entries were found.") }
    }

    private suspend fun <T> safe(config: ExtensionConfig, block: suspend () -> T): ExtensionResult<T> = try {
        config.validationError()?.let { throw IllegalArgumentException(it) }
        ExtensionResult.Success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        ExtensionResult.Error("${config.name}: ${error.message ?: "Request failed."}")
    }

    private suspend fun getDocument(url: String): Document = withContext(Dispatchers.IO) {
        val response = client.get(url) {
            header("User-Agent", "Huni-Reader/1.0 (CSS extension engine)")
            header("Accept", "text/html,application/xhtml+xml")
        }
        if (response.status.value !in 200..299) throw IllegalStateException("Site returned HTTP ${response.status.value}.")
        val html = response.bodyAsText()
        if (html.length > MAX_HTML_CHARS) throw IllegalStateException("Source page is too large to parse safely.")
        Jsoup.parse(html, url)
    }

    private fun resolve(config: ExtensionConfig, template: String, variables: Map<String, String>): String {
        var endpoint = template
        variables.forEach { (key, value) -> endpoint = endpoint.replace("{{$key}}", value) }
        val absolute = if (endpoint.startsWith("https://") || endpoint.startsWith("http://")) endpoint
        else "${config.baseUrl.trimEnd('/')}/${endpoint.trimStart('/')}"
        val uri = URI(absolute)
        require(uri.scheme == "http" || uri.scheme == "https") { "Only HTTP and HTTPS source URLs are allowed." }
        require(uri.host != null && uri.userInfo == null) { "Invalid source URL." }
        return uri.toASCIIString()
    }

    private fun requiredText(document: Document, selector: String, label: String): String {
        val element = requiredElements(document, selector, label).first()
        return elementText(element).takeIf(String::isNotBlank)
            ?: throw SelectorException("The $label selector matched an empty element.")
    }

    private fun requiredElements(document: Document, selector: String, label: String): List<Element> {
        if (selector.isBlank()) throw SelectorException("No CSS selector configured for $label.")
        return try {
            document.select(selector).toList().ifEmpty { throw SelectorException("The CSS selector for $label matched no elements: $selector") }
        } catch (error: SelectorException) {
            throw error
        } catch (error: Exception) {
            throw SelectorException("Invalid CSS selector for $label: ${error.message}")
        }
    }

    private fun requiredUrl(document: Document, selector: String, baseUrl: String, label: String): String {
        val element = requiredElements(document, selector, label).first()
        return elementImageUrl(element, baseUrl) ?: throw SelectorException("The $label selector matched an element without an image URL.")
    }

    private fun elementText(element: Element): String = element.text().ifBlank { element.attr("title") }.trim()

    private fun elementUrl(element: Element, baseUrl: String): String? =
        sequenceOf("href", "data-url", "src").map { attr -> element.absUrl(attr).ifBlank { element.attr(attr) } }
            .firstOrNull(String::isNotBlank)?.let { resolveRelative(baseUrl, it) }

    private fun elementImageUrl(element: Element, baseUrl: String): String? =
        sequenceOf("src", "data-src", "data-original", "href").map { attr -> element.absUrl(attr).ifBlank { element.attr(attr) } }
            .firstOrNull(String::isNotBlank)?.let { resolveRelative(baseUrl, it) }

    private fun resolveRelative(baseUrl: String, url: String): String? = runCatching {
        URI(baseUrl).resolve(url).toASCIIString().takeIf { it.startsWith("https://") || it.startsWith("http://") }
    }.getOrNull()

    private class SelectorException(message: String) : IllegalStateException(message)

    private companion object { const val MAX_HTML_CHARS = 20 * 1024 * 1024 }
}

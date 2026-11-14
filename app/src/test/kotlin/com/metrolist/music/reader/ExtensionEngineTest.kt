package com.metrolist.music.reader

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import com.metrolist.music.data.engine.ExtensionEngine
import com.metrolist.music.data.model.ChapterSelectors
import com.metrolist.music.data.model.DetailsSelectors
import com.metrolist.music.data.model.ExtensionConfig
import com.metrolist.music.data.model.ExtensionEndpoint
import com.metrolist.music.data.model.ExtensionResult
import com.metrolist.music.data.model.MangaSelectors
import com.metrolist.music.data.model.PageSelectors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionEngineTest {
    private val config = ExtensionConfig(
        id = "test-source",
        name = "Test Source",
        baseUrl = "https://source.test",
        lang = "en",
        popular = ExtensionEndpoint("/popular", MangaSelectors("a.title", "img.cover", "a.title")),
        search = ExtensionEndpoint("/search?q={{query}}", MangaSelectors("a.title", "img.cover", "a.title")),
        details = ExtensionEndpoint("{{mangaUrl}}", DetailsSelectors("h1.title", ".author", "img.detail-cover")),
        chapters = ExtensionEndpoint("{{mangaUrl}}/chapters", ChapterSelectors("a.chapter", "a.chapter")),
        pages = ExtensionEndpoint("{{chapterUrl}}", PageSelectors("img.page")),
    )

    @Test fun popularSearchDetailsChaptersAndPagesUseCssSelectorsAndTemplates() = runBlocking {
        val requested = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            requested += request.url.toString()
            val body = when {
                request.url.encodedPath == "/popular" || request.url.encodedPath == "/search" -> """
                    <article><a class="title" href="/manga/alpha">Alpha</a><img class="cover" src="/images/alpha.jpg"></article>
                """.trimIndent()
                request.url.encodedPath == "/manga/alpha" -> """
                    <h1 class="title">Alpha</h1><span class="author">A. Writer</span><img class="detail-cover" src="/images/detail.jpg">
                """.trimIndent()
                request.url.encodedPath == "/manga/alpha/chapters" -> """
                    <a class="chapter" href="/chapter/one">Chapter 1</a><a class="chapter" href="/chapter/two">Chapter 2</a>
                """.trimIndent()
                request.url.encodedPath == "/chapter/one" -> """
                    <img class="page" src="/pages/1.jpg"><img class="page" data-src="/pages/2.jpg">
                """.trimIndent()
                else -> error("Unexpected request: ${request.url}")
            }
            respond(ByteReadChannel(body), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8"))
        })
        val engine = ExtensionEngine(client)
        try {
            val popular = success(engine.popular(config))
            assertEquals("Alpha", popular.single().title)
            assertEquals("https://source.test/images/alpha.jpg", popular.single().coverUrl)
            assertEquals("https://source.test/manga/alpha", popular.single().link)

            val search = success(engine.search(config, "blue sky"))
            assertEquals("Alpha", search.single().title)
            assertTrue(requested.any { it.contains("q=blue+sky") || it.contains("q=blue%20sky") })

            val detail = success(engine.details(config, "https://source.test/manga/alpha"))
            assertEquals("A. Writer", detail.author)
            assertEquals("https://source.test/images/detail.jpg", detail.coverUrl)

            val chapters = success(engine.chapters(config, "https://source.test/manga/alpha"))
            assertEquals(2, chapters.size)
            assertEquals("https://source.test/chapter/one", chapters.first().link)

            val pages = success(engine.pages(config, "https://source.test/chapter/one"))
            assertEquals(listOf("https://source.test/pages/1.jpg", "https://source.test/pages/2.jpg"), pages)
        } finally {
            engine.close()
        }
    }

    private fun <T> success(result: ExtensionResult<T>): T = when (result) {
        is ExtensionResult.Success -> result.data
        is ExtensionResult.Error -> throw AssertionError(result.message)
    }

    @Test fun selectorAndServerFailuresReturnVisibleErrors() = runBlocking {
        val selectorEngine = ExtensionEngine(HttpClient(MockEngine {
            respond(ByteReadChannel("<html><body>no matches</body></html>"), HttpStatusCode.OK)
        }))
        val serverEngine = ExtensionEngine(HttpClient(MockEngine {
            respond(ByteReadChannel("offline"), HttpStatusCode.ServiceUnavailable)
        }))
        try {
            val selectorError = selectorEngine.popular(config) as ExtensionResult.Error
            assertTrue(selectorError.message.contains("selector", ignoreCase = true))
            val serverError = serverEngine.popular(config) as ExtensionResult.Error
            assertTrue(serverError.message.contains("503"))
        } finally {
            selectorEngine.close()
            serverEngine.close()
        }
    }
}

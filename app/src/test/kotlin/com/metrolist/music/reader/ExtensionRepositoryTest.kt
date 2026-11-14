package com.metrolist.music.reader

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metrolist.music.data.repository.ExtensionRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class ExtensionRepositoryTest {
    private lateinit var directory: File

    @Before fun setUp() {
        directory = File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "extension-repository-test").apply {
            deleteRecursively()
            mkdirs()
        }
    }

    @Test fun pastedJsonIsValidatedPersistedReloadedAndRemoved() = runBlocking {
        val repository = ExtensionRepository(directory)
        try {
            val added = repository.addFromInput(configJson)
            assertEquals("sample", added.single().id)
            assertTrue(File(directory, "extensions/sample.json").isFile)
            assertEquals("Sample Source", repository.installed().single().name)
            repository.remove("sample")
            assertTrue(repository.installed().isEmpty())
        } finally {
            repository.close()
        }
    }

    @Test fun urlCanReturnJsonIndexAndAllConfigsAreInstalled() = runBlocking {
        val client = HttpClient(MockEngine {
            respond(ByteReadChannel("[{\"id\":\"sample\",\"name\":\"Sample Source\",\"baseUrl\":\"https://example.test\",\"lang\":\"en\",\"popular\":$mangaEndpoint,\"search\":$mangaEndpoint,\"details\":$detailsEndpoint,\"chapters\":$chapterEndpoint,\"pages\":$pageEndpoint}]"), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        val repository = ExtensionRepository(directory, client)
        try {
            val installed = repository.addFromInput("https://example.test/extensions.json")
            assertEquals("sample", installed.single().id)
            assertEquals("sample", repository.fetchIndex("https://example.test/extensions.json").single().id)
        } finally {
            repository.close()
        }
    }

    @Test fun invalidIdsAndNonHttpUrlsAreRejected() = runBlocking {
        val repository = ExtensionRepository(directory)
        try {
            val invalid = configJson.replace("\"id\":\"sample\"", "\"id\":\"../escape\"")
            val idError = runCatching { repository.addFromInput(invalid) }.exceptionOrNull()
            assertTrue(idError?.message.orEmpty().contains("id"))
            val urlError = runCatching { repository.addFromInput("file:///etc/passwd") }.exceptionOrNull()
            assertTrue(urlError?.message.orEmpty().contains("HTTP", ignoreCase = true))
        } finally {
            repository.close()
        }
    }

    private val mangaEndpoint = "{\"url\":\"/popular\",\"selectors\":{\"title\":\"a.title\",\"cover\":\"img.cover\",\"link\":\"a.title\"}}"
    private val detailsEndpoint = "{\"url\":\"{{mangaUrl}}\",\"selectors\":{\"title\":\"h1.title\",\"author\":\".author\",\"cover\":\"img.cover\"}}"
    private val chapterEndpoint = "{\"url\":\"{{mangaUrl}}/chapters\",\"selectors\":{\"title\":\"a.chapter\",\"link\":\"a.chapter\"}}"
    private val pageEndpoint = "{\"url\":\"{{chapterUrl}}\",\"selectors\":{\"image\":\"img.page\"}}"
    private val configJson get() = """
        {
          "id":"sample", "name":"Sample Source", "baseUrl":"https://example.test", "lang":"en",
          "popular":$mangaEndpoint, "search":$mangaEndpoint, "details":$detailsEndpoint,
          "chapters":$chapterEndpoint, "pages":$pageEndpoint
        }
    """.trimIndent()
}

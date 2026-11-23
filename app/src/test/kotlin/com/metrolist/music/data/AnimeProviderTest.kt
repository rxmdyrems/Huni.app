package com.metrolist.music.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metrolist.music.data.engine.AnimeJavaScriptRuntime
import com.metrolist.music.data.model.AnimeProviderMeta
import com.metrolist.music.data.repository.AnimeProviderRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class AnimeProviderTest {
    private lateinit var directory: File

    @Before fun setUp() {
        directory = File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "anime-provider-test").apply {
            deleteRecursively()
            mkdirs()
        }
    }

    @Test fun repositoryRefreshInstallDisableEnableRemoveAndRefreshState() = runBlocking {
        val repository = AnimeProviderRepository(directory, client { requestUrl ->
            if (requestUrl == REPO_URL) manifest("sample") else JAVASCRIPT
        })
        try {
            val catalog = repository.refreshRepository(REPO_URL).getOrThrow()
            val provider = catalog.providers.single()
            assertEquals("Sample anime", provider.name)
            repository.install(provider)
            assertTrue(repository.providerFile(provider.id).readText().contains("function getInfo"))
            assertTrue(repository.getInstalled().single().enabled)
            repository.setEnabled(REPO_URL, provider.id, false)
            assertFalse(repository.getInstalled().single().enabled)
            repository.setEnabled(REPO_URL, provider.id, true)
            assertTrue(repository.getInstalled().single().enabled)

            repository.refreshRepository(REPO_URL).getOrThrow()
            assertEquals(1, repository.getRepositories().single().providers.size)
            repository.removeProvider(REPO_URL, provider.id)
            assertTrue(repository.getInstalled().isEmpty())
            repository.removeRepository(REPO_URL)
            assertTrue(repository.getRepositories().isEmpty())
        } finally {
            repository.close()
        }
    }

    @Test fun rejectsInvalidSourceIdAndDuplicateIdsInManifest() = runBlocking {
        val invalid = AnimeProviderRepository(directory, client { manifest("../escape") })
        try {
            assertTrue(invalid.refreshRepository(REPO_URL).isFailure)
        } finally {
            invalid.close()
        }
        val duplicate = AnimeProviderRepository(directory, client { """{"name":"Repo","sources":[${source("one")},${source("one")}] }""" })
        try {
            assertTrue(duplicate.refreshRepository(REPO_URL).isFailure)
        } finally {
            duplicate.close()
        }
    }

    @Test fun keepsPreviousInstalledScriptWhenFailedUpdateCannotFetch() = runBlocking {
        var code = JAVASCRIPT
        var version = "1.0.0"
        val repository = AnimeProviderRepository(directory, client { requestUrl ->
            if (requestUrl == REPO_URL) manifest("sample", version) else code
        })
        try {
            val provider = repository.refreshRepository(REPO_URL).getOrThrow().providers.single()
            repository.install(provider)
            val original = repository.providerFile(provider.id).readText()
            version = "1.1.0"
            val updatedProvider = repository.refreshRepository(REPO_URL).getOrThrow().providers.single()
            code = ""
            try {
                repository.install(updatedProvider)
                throw AssertionError("Expected empty update to fail")
            } catch (_: IllegalArgumentException) {
                assertEquals(original, repository.providerFile(provider.id).readText())
            }
        } finally {
            repository.close()
        }
    }

    @Test fun runtimeCallsSynchronousProviderContractAndRejectsMissingMethod() {
        val runtime = AnimeJavaScriptRuntime()
        try {
            runtime.load(meta(), JAVASCRIPT)
            val results = runtime.search("huni")
            assertEquals("A sample", results.single().title)
            assertEquals("sample", results.single().sourceId)
            assertEquals("Episode 1", runtime.episodes(results.single().url).single().title)
            assertEquals("sample", runtime.episodes(results.single().url).single().sourceId)
            assertEquals("https://video.example.test/watch.m3u8", runtime.videoSources("https://anime.example.test/e1").single().url)

            try {
                runtime.load(meta(), "function search() { return []; }")
                throw AssertionError("Expected provider contract failure")
            } catch (error: IllegalArgumentException) {
                assertTrue(error.message.orEmpty().contains("getEpisodes"))
            }
        } finally {
            runtime.close()
        }
    }

    @Test fun runtimeRejectsJavaAccessAndTimesOutNonterminatingProviders() {
        val runtime = AnimeJavaScriptRuntime()
        try {
            try {
                runtime.load(meta(), "var hacked = Packages.java.lang.System; function search(){return [];}")
                throw AssertionError("Expected Java access to be blocked")
            } catch (_: Exception) {
                // The host class shutter denies Java package access.
            }
            runtime.load(meta(), """
                function search() { while (true) {} }
                function getEpisodes() { return []; }
                function getVideoSources() { return []; }
            """.trimIndent())
            try {
                runtime.search("x")
                throw AssertionError("Expected JavaScript deadline to be enforced")
            } catch (error: Exception) {
                assertTrue(error.message.orEmpty().contains("timed out"))
            }
        } finally {
            runtime.close()
        }
    }

    private fun client(body: (String) -> String): HttpClient = HttpClient(MockEngine { request ->
        val payload = body(request.url.toString())
        respond(ByteReadChannel(payload), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    })

    private fun source(id: String, version: String = "1.0.0") = """{"id":"$id","name":"Sample anime","version":"$version","type":"anime","lang":"en","file":"provider.js"}"""

    private fun manifest(id: String, version: String = "1.0.0") = """{"name":"Test Repo","description":"Provider test","sources":[${source(id, version)}]}"""

    private fun meta() = AnimeProviderMeta(
        id = "sample",
        name = "Sample anime",
        version = "1.0.0",
        type = "anime",
        lang = "en",
        file = "https://example.test/provider.js",
        logo = null,
        nsfw = false,
        repositoryUrl = REPO_URL,
        description = "Provider test",
    )

    private companion object {
        const val REPO_URL = "https://example.test/repository.json"
        const val JAVASCRIPT = """
            function getInfo() { return { name: 'Sample anime', version: '1.0.0' }; }
            function search(query, page) { return [{ id: 'a1', title: 'A sample', url: 'https://anime.example.test/a1' }]; }
            function getEpisodes(url) { return [{ id: 'e1', title: 'Episode 1', number: 1, url: 'https://anime.example.test/e1' }]; }
            function getVideoSources(url) { return [{ url: 'https://video.example.test/watch.m3u8', quality: '1080p', container: 'hls' }]; }
        """
    }
}

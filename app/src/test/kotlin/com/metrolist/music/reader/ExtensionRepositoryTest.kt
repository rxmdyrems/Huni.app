package com.metrolist.music.reader

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.metrolist.music.data.model.ExtensionMeta
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
class ExtensionRepositoryTest {
    private lateinit var directory: File

    @Before fun setUp() {
        directory = File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "extension-repository-test").apply {
            deleteRecursively()
            mkdirs()
        }
    }

    @Test fun parsesKeiyoushiIndexJsonAndMapsNestedFields() = runBlocking {
        val client = client { catalogJson }
        val repository = ExtensionRepository(directory, client)
        try {
            val meta = repository.fetchCatalog(REPO_URL).getOrThrow().single()
            assertEquals("Sample Manga", meta.name)
            assertEquals("eu.kanade.tachiyomi.extension.en.sample", meta.pkg)
            assertEquals("https://github.com/example/sample.apk", meta.apk)
            assertEquals("en", meta.lang)
            assertEquals(10402L, meta.code)
            assertEquals("1.4.2", meta.version)
            assertTrue(meta.nsfw)
            assertEquals("123456789", meta.sources.single().id)
            assertEquals("https://example.test", meta.sources.single().baseUrl)
        } finally {
            repository.close()
        }
    }

    @Test fun relativeApkFieldUsesRawRepositoryApkPattern() = runBlocking {
        val flat = """[{"name":"Sample","pkg":"eu.test.extension","apk":"sample.apk","lang":"en","code":1,"version":"1.0","nsfw":false,"sources":[{"name":"Sample","lang":"en","id":"42","baseUrl":"https://example.test"}]}]"""
        val repository = ExtensionRepository(directory, client { flat })
        try {
            val meta = repository.fetchCatalog(REPO_URL).getOrThrow().single()
            assertEquals("https://raw.githubusercontent.com/example/repo/apk/sample.apk", meta.apk)
        } finally {
            repository.close()
        }
    }

    @Test fun apkDownloadManifestReloadAndUninstallWork() = runBlocking {
        val repository = ExtensionRepository(directory, client { ByteArray(1024) { it.toByte() } })
        try {
            val meta = ExtensionMeta(
                name = "Sample",
                pkg = "eu.test.extension",
                apk = "https://example.test/sample.apk",
                lang = "en",
                code = 1L,
                version = "1.0",
                nsfw = false,
                sources = emptyList(),
            )
            val downloaded = repository.downloadExtension(meta)
            assertTrue(downloaded.isFile)
            assertEquals(1024, downloaded.length().toInt())
            repository.markInstalled(meta)
            assertEquals(meta, repository.getInstalled().single())
            repository.removeExtension(meta.pkg)
            assertTrue(repository.getInstalled().isEmpty())
            assertFalse(downloaded.exists())
        } finally {
            repository.close()
        }
    }

    @Test fun defaultRepoCanBeRemovedAddedAndEditedPersistently() {
        val repository = ExtensionRepository(directory)
        try {
            assertEquals("Keiyoushi (Default)", repository.getRepos().single().label)
            val original = repository.getRepos().single().url
            repository.removeRepo(original)
            assertTrue(repository.getRepos().isEmpty())
            repository.addRepo("https://example.test/index.json", "Test repository")
            repository.updateRepo("https://example.test/index.json", "https://example.test/v2/index.json")
            assertEquals("https://example.test/v2/index.json", repository.getRepos().single().url)
            assertEquals("Test repository", repository.getRepos().single().label)
            val reloaded = ExtensionRepository(directory)
            try {
                assertEquals(repository.getRepos(), reloaded.getRepos())
            } finally {
                reloaded.close()
            }
        } finally {
            repository.close()
        }
    }

    @Test fun malformedCatalogReturnsFailureInsteadOfCrashing() = runBlocking {
        val repository = ExtensionRepository(directory, client { "{not-json" })
        try {
            assertTrue(repository.fetchCatalog(REPO_URL).isFailure)
        } finally {
            repository.close()
        }
    }

    private fun client(body: () -> Any): HttpClient = HttpClient(MockEngine {
        val payload = body()
        val channel = when (payload) {
            is ByteArray -> ByteReadChannel(payload)
            else -> ByteReadChannel(payload.toString())
        }
        respond(channel, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    })

    private val catalogJson = """
        {
          "name":"Keiyoushi",
          "extensionList":{"extensions":[{
            "name":"Sample Manga",
            "packageName":"eu.kanade.tachiyomi.extension.en.sample",
            "resources":{"apkUrl":"https://github.com/example/sample.apk"},
            "versionCode":"10402",
            "versionName":"1.4.2",
            "contentWarning":"CONTENT_WARNING_NSFW",
            "sources":[{"id":"123456789","name":"Sample","language":"en","homeUrl":"https://example.test"}]
          }]}
        }
    """.trimIndent()

    private companion object {
        const val REPO_URL = "https://raw.githubusercontent.com/example/repo/index.json"
    }
}

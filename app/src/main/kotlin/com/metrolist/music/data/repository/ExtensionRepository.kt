package com.metrolist.music.data.repository

import android.system.Os
import com.metrolist.music.data.model.ExtensionMeta
import com.metrolist.music.data.model.ExtensionSourceMeta
import com.metrolist.music.data.model.RepoEntry
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL

/** Persists Mihon extension APKs and parses JSON extension repository indexes. */
class ExtensionRepository(
    filesDir: File,
    private val client: HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
        }
    },
) {
    private val directory = File(filesDir, "extensions")
    private val manifestFile = File(directory, "installed.json")
    private val reposFile = File(directory, "repositories.json")
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    /** Fetch and decode either the flat extension schema or Keiyoushi's index.json schema. */
    suspend fun fetchCatalog(repoUrl: String): Result<List<ExtensionMeta>> = try {
        Result.success(withContext(Dispatchers.IO) {
            validateHttpUrl(repoUrl)
            val response = client.get(repoUrl) { header("User-Agent", "Huni-Reader/1.0") }
            if (response.status.value !in 200..299) throw IllegalStateException("HTTP ${response.status.value}")
            val body = ByteArrayOutputStream().use { output ->
                copyBody(response, output, MAX_INDEX_BYTES)
                output.toByteArray().toString(Charsets.UTF_8)
            }
            parseCatalog(body, repoUrl)
        })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    /** Download the catalog APK atomically to filesDir/extensions/{pkg}.apk. */
    suspend fun downloadExtension(meta: ExtensionMeta): File = withContext(Dispatchers.IO) {
        require(validPackage(meta.pkg)) { "Invalid extension package name." }
        val response = client.get(downloadUrl(meta.apk)) { header("User-Agent", "Huni-Reader/1.0") }
        if (response.status.value !in 200..299) throw IllegalStateException("HTTP ${response.status.value}")
        if (!directory.exists() && !directory.mkdirs()) throw IllegalStateException("Could not create extension storage.")
        val destination = apkFile(meta.pkg)
        val temporary = File(directory, "${meta.pkg}.apk.tmp")
        try {
            temporary.outputStream().use { output ->
                val size = copyBody(response, output, MAX_APK_BYTES)
                require(size > 0) { "APK is empty or exceeds the download limit." }
                output.fd.sync()
            }
            Os.rename(temporary.absolutePath, destination.absolutePath)
            destination
        } catch (error: Exception) {
            temporary.delete()
            throw error
        }
    }

    /** Local installed-extension manifest; corrupted entries are ignored, never crash the screen. */
    fun getInstalled(): List<ExtensionMeta> = runCatching {
        if (!manifestFile.isFile) emptyList()
        else json.decodeFromString<List<ExtensionMeta>>(manifestFile.readText())
            .filter { validPackage(it.pkg) && apkFile(it.pkg).isFile }
            .sortedBy { it.name.lowercase() }
    }.getOrDefault(emptyList())

    fun markInstalled(meta: ExtensionMeta) {
        require(validPackage(meta.pkg)) { "Invalid extension package name." }
        synchronized(this) {
            val updated = (getInstalled().filterNot { it.pkg == meta.pkg } + meta).sortedBy { it.name.lowercase() }
            writeJsonAtomically(manifestFile, json.encodeToString(updated))
        }
    }

    fun removeExtension(pkg: String) {
        if (!validPackage(pkg)) return
        synchronized(this) {
            apkFile(pkg).delete()
            File(directory, "$pkg.apk.tmp").delete()
            writeJsonAtomically(manifestFile, json.encodeToString(getInstalled().filterNot { it.pkg == pkg }))
        }
    }

    fun getRepos(): List<RepoEntry> = synchronized(this) {
        if (!reposFile.isFile) {
            val defaults = listOf(RepoEntry(DEFAULT_REPO_URL, DEFAULT_REPO_LABEL))
            writeJsonAtomically(reposFile, json.encodeToString(defaults))
            defaults
        } else {
            runCatching { json.decodeFromString<List<RepoEntry>>(reposFile.readText()) }
                .getOrDefault(emptyList())
                .filter { runCatching { validateHttpUrl(it.url); it.label.isNotBlank() }.getOrDefault(false) }
        }
    }

    fun addRepo(url: String, label: String) = synchronized(this) {
        validateHttpUrl(url)
        require(label.isNotBlank()) { "Repository label is required." }
        val updated = getRepos().filterNot { it.url == url } + RepoEntry(url.trim(), label.trim())
        writeJsonAtomically(reposFile, json.encodeToString(updated))
    }

    fun removeRepo(url: String) = synchronized(this) {
        writeJsonAtomically(reposFile, json.encodeToString(getRepos().filterNot { it.url == url }))
    }

    fun updateRepo(url: String, newUrl: String) {
        val currentLabel = getRepos().firstOrNull { it.url == url }?.label ?: return
        updateRepo(url, newUrl, currentLabel)
    }

    fun updateRepo(url: String, newUrl: String, newLabel: String) = synchronized(this) {
        validateHttpUrl(newUrl)
        require(newLabel.isNotBlank()) { "Repository label is required." }
        val existing = getRepos().firstOrNull { it.url == url } ?: return@synchronized
        val updated = getRepos().map {
            if (it.url == url) it.copy(url = newUrl.trim(), label = newLabel.trim()) else it
        }.distinctBy { it.url }
        writeJsonAtomically(reposFile, json.encodeToString(updated.ifEmpty { listOf(existing.copy(url = newUrl.trim(), label = newLabel.trim())) }))
    }

    fun apkFile(pkg: String): File = File(directory, "$pkg.apk")

    fun close() = client.close()

    private fun parseCatalog(body: String, repoUrl: String): List<ExtensionMeta> {
        val root = json.parseToJsonElement(body)
        val entries = when (root) {
            is JsonArray -> root
            is JsonObject -> {
                val extensionList = root["extensionList"] as? JsonObject
                (extensionList?.get("extensions") as? JsonArray)
                    ?: (root["extensions"] as? JsonArray)
                    ?: throw IllegalArgumentException("Catalog JSON does not contain an extensions list.")
            }
            else -> throw IllegalArgumentException("Catalog JSON must be an object or array.")
        }
        return entries.mapNotNull { element -> parseExtension(element, repoUrl) }
            .distinctBy { it.pkg }
            .sortedWith(compareBy<ExtensionMeta> { it.name.lowercase() }.thenBy { it.lang.lowercase() })
    }

    private fun parseExtension(element: JsonElement, repoUrl: String): ExtensionMeta? {
        val item = element as? JsonObject ?: return null
        val name = item.string("name")?.takeIf(String::isNotBlank) ?: return null
        val pkg = (item.string("pkg") ?: item.string("packageName"))?.takeIf(::validPackage) ?: return null
        val resources = item["resources"] as? JsonObject
        val apkField = item.string("apk") ?: resources?.string("apkUrl") ?: return null
        val sourceElements = (item["sources"] as? JsonArray).orEmpty()
        val sources = sourceElements.mapNotNull { sourceElement ->
            val source = sourceElement as? JsonObject ?: return@mapNotNull null
            val sourceName = source.string("name")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val sourceId = source.string("id")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            ExtensionSourceMeta(
                name = sourceName,
                lang = source.string("lang") ?: source.string("language") ?: "all",
                id = sourceId,
                baseUrl = source.string("baseUrl") ?: source.string("homeUrl") ?: "",
            )
        }
        val language = item.string("lang") ?: sources.firstOrNull()?.lang ?: "all"
        val code = (item.string("code") ?: item.string("versionCode"))?.toLongOrNull() ?: 0L
        val version = item.string("version") ?: item.string("versionName") ?: ""
        val nsfw = item["nsfw"].asBoolean()
            ?: item.string("contentWarning")?.contains("NSFW", ignoreCase = true)
            ?: false
        val apk = resolveApkField(apkField, repoUrl)
        return ExtensionMeta(name, pkg, apk, language, code, version, nsfw, sources)
    }

    /** Keiyoushi index.json currently carries absolute release APK URLs; flat indexes may carry a filename. */
    private fun resolveApkField(apkField: String, repoUrl: String): String {
        if (apkField.startsWith("https://") || apkField.startsWith("http://")) {
            validateHttpUrl(apkField)
            return apkField
        }
        val fileName = apkField.trimStart('/')
        require(fileName.isNotBlank() && !fileName.split('/').any { it == ".." }) { "Invalid APK path." }
        val root = repoUrl.substringBeforeLast('/', repoUrl).trimEnd('/')
        return "$root/apk/$fileName"
    }

    private fun downloadUrl(apk: String): String {
        if (apk.startsWith("https://") || apk.startsWith("http://")) {
            validateHttpUrl(apk)
            return apk
        }
        return "${DEFAULT_REPO_ROOT}/apk/${apk.trimStart('/')}".also(::validateHttpUrl)
    }

    private fun writeJsonAtomically(file: File, value: String) {
        if (!directory.exists() && !directory.mkdirs()) throw IllegalStateException("Could not create extension storage.")
        val temp = File(directory, "${file.name}.tmp")
        try {
            temp.writeText(value)
            Os.rename(temp.absolutePath, file.absolutePath)
        } catch (error: Exception) {
            temp.delete()
            throw error
        }
    }

    private suspend fun copyBody(
        response: io.ktor.client.statement.HttpResponse,
        output: java.io.OutputStream,
        maximumBytes: Long,
    ): Long {
        val declaredLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        require(declaredLength == null || declaredLength <= maximumBytes) { "Response exceeds the download limit." }
        val channel = response.bodyAsChannel()
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val count = channel.readAvailable(buffer, 0, buffer.size)
            if (count == -1) break
            if (count == 0) continue
            total += count
            require(total <= maximumBytes) { "Response exceeds the download limit." }
            output.write(buffer, 0, count)
        }
        return total
    }

    private fun JsonObject.string(key: String): String? = this[key]?.let { value ->
        when (value) {
            JsonNull -> null
            is JsonPrimitive -> value.contentOrNull
            else -> null
        }
    }

    private fun JsonElement?.asBoolean(): Boolean? = when (this) {
        is JsonPrimitive -> booleanOrNull ?: contentOrNull?.let { text ->
            text == "1" || text.equals("true", ignoreCase = true)
        }
        else -> null
    }

    private fun validPackage(pkg: String): Boolean = pkg.matches(Regex("[A-Za-z0-9_.-]{3,200}")) && !pkg.contains("..")

    private fun validateHttpUrl(value: String) {
        val parsed = runCatching { URL(value) }.getOrElse { throw IllegalArgumentException("Invalid URL: ${it.message}") }
        require(parsed.protocol == "https" || parsed.protocol == "http") { "Only HTTP and HTTPS repository URLs are allowed." }
        require(parsed.host.isNotBlank() && parsed.userInfo == null) { "Repository URL must have a host and cannot include credentials." }
    }

    private companion object {
        const val DEFAULT_REPO_URL = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.json"
        const val DEFAULT_REPO_ROOT = "https://raw.githubusercontent.com/keiyoushi/extensions/repo"
        const val DEFAULT_REPO_LABEL = "Keiyoushi (Default)"
        const val MAX_INDEX_BYTES = 12L * 1024 * 1024
        const val MAX_APK_BYTES = 150L * 1024 * 1024
    }
}

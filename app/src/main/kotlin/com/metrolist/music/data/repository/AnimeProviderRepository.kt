package com.metrolist.music.data.repository

import com.metrolist.music.data.engine.AnimeJavaScriptRuntime
import com.metrolist.music.data.model.AnimeProviderMeta
import com.metrolist.music.data.model.AnimeProviderRepoCatalog
import com.metrolist.music.data.model.InstalledAnimeProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * Independent store for user-installed JavaScript anime providers. It deliberately
 * does not share files or metadata with the Mihon APK reader extension repository.
 */
class AnimeProviderRepository(
    filesDir: File,
    private val client: HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        followRedirects = false
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            socketTimeoutMillis = REQUEST_TIMEOUT_MS
        }
    },
) {
    private val directory = File(filesDir, "anime-providers")
    private val stateFile = File(directory, "state.json")
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    suspend fun refreshRepository(url: String): Result<AnimeProviderRepoCatalog> = try {
        Result.success(withContext(Dispatchers.IO) {
            validateHttpsUrl(url)
            val body = fetchText(url, MAX_MANIFEST_BYTES)
            val root = json.parseToJsonElement(body) as? JsonObject
                ?: throw IllegalArgumentException("Provider manifest must be a JSON object.")
            val name = root.string("name")?.trim()?.takeIf(String::isNotEmpty)
                ?: throw IllegalArgumentException("Provider manifest is missing its name.")
            val description = root.string("description").orEmpty().take(MAX_DESCRIPTION_LENGTH)
            val entries = root["sources"] as? JsonArray
                ?: throw IllegalArgumentException("Provider manifest is missing its sources list.")
            require(entries.size <= MAX_PROVIDERS_PER_REPOSITORY) { "Provider repository contains too many entries." }
            val providers = entries.mapIndexedNotNull { index, element ->
                val item = element as? JsonObject
                    ?: throw IllegalArgumentException("Provider entry ${index + 1} must be an object.")
                val id = item.string("id")?.trim().orEmpty()
                val providerName = item.string("name")?.trim().orEmpty()
                val version = item.string("version")?.trim().orEmpty()
                val type = item.string("type")?.trim()?.lowercase() ?: "anime"
                val lang = item.string("lang")?.trim()?.takeIf(String::isNotEmpty) ?: "en"
                val file = item.string("file")?.trim().orEmpty()
                require(validId(id)) { "Invalid provider source id: $id" }
                require(providerName.isNotEmpty() && providerName.length <= MAX_NAME_LENGTH) { "Invalid provider name for $id." }
                require(validVersion(version)) { "Invalid provider version for $id." }
                require(type == "anime") { "Unsupported provider type '$type' for $id." }
                require(lang.length <= 32 && lang.matches(Regex("[A-Za-z0-9_-]+"))) { "Invalid language for $id." }
                val fileUrl = resolveUrl(url, file)
                validateHttpsUrl(fileUrl)
                val logo = item.string("logo")?.trim()?.takeIf(String::isNotEmpty)?.let { resolveUrl(url, it) }
                logo?.let(::validateHttpsUrl)
                val nsfw = item["nsfw"]?.let { (it as? JsonPrimitive)?.booleanOrNull } ?: false
                AnimeProviderMeta(
                    id = id,
                    name = providerName,
                    version = version,
                    type = type,
                    lang = lang,
                    file = fileUrl,
                    logo = logo,
                    nsfw = nsfw,
                    repositoryUrl = url.trim(),
                    description = item.string("description")?.take(MAX_DESCRIPTION_LENGTH).orEmpty(),
                )
            }.distinctBy { it.id }
            if (providers.size != entries.size) throw IllegalArgumentException("Provider IDs must be unique within a repository.")
            val repo = AnimeProviderRepoCatalog(
                url = url.trim(),
                name = name.take(MAX_NAME_LENGTH),
                description = description,
                providers = providers,
                refreshedAt = System.currentTimeMillis(),
            )
            synchronized(this) { writeRepositories(readRepositories().filterNot { it.url == repo.url } + repo) }
            repo
        })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    fun getRepositories(): List<AnimeProviderRepoCatalog> = synchronized(this) { readRepositories() }

    fun getInstalled(): List<InstalledAnimeProvider> = synchronized(this) { readInstalled() }

    fun providerFile(id: String): File {
        require(validId(id)) { "Invalid provider source id." }
        return File(directory, "${id.sha256()}.js")
    }

    suspend fun install(meta: AnimeProviderMeta): InstalledAnimeProvider = withContext(Dispatchers.IO) {
        validateMeta(meta)
        synchronized(this@AnimeProviderRepository) {
            val catalog = readRepositories().firstOrNull { it.url == meta.repositoryUrl }
                ?: throw IllegalArgumentException("Refresh this provider repository before installing from it.")
            require(catalog.providers.any { it == meta }) { "Provider metadata is not present in the refreshed repository." }
            require(readInstalled().none { it.meta.id == meta.id && it.meta.repositoryUrl != meta.repositoryUrl }) {
                "Another installed repository already uses provider ID '${meta.id}'."
            }
        }
        val code = fetchBytes(meta.file, MAX_PROVIDER_BYTES)
        require(code.isNotEmpty()) { "Provider script is empty." }
        require(code.toString(Charsets.UTF_8).toByteArray(Charsets.UTF_8).contentEquals(code)) {
            "Provider script must be valid UTF-8."
        }
        val script = code.toString(Charsets.UTF_8)
        require(!script.contains('\u0000')) { "Provider script contains a NUL byte." }
        AnimeJavaScriptRuntime().use { runtime -> runtime.load(meta, script) }
        if (!directory.exists() && !directory.mkdirs()) throw IllegalStateException("Could not create provider storage.")
        val destination = providerFile(meta.id)
        val temporary = File(directory, "${destination.name}.tmp")
        val backup = File(directory, "${destination.name}.bak")
        try {
            synchronized(this@AnimeProviderRepository) {
                val current = readInstalled()
                require(current.none { it.meta.id == meta.id && it.meta.repositoryUrl != meta.repositoryUrl }) {
                    "Another installed repository already uses provider ID '${meta.id}'."
                }
                val previous = current.firstOrNull { it.meta.id == meta.id }
                temporary.outputStream().use { output ->
                    output.write(code)
                    output.fd.sync()
                }
                if (destination.isFile) {
                    destination.copyTo(backup, overwrite = true)
                    FileOutputStream(backup, true).use { it.fd.sync() }
                }
                try {
                    Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                    val installed = InstalledAnimeProvider(meta, enabled = previous?.enabled ?: true, installedAt = System.currentTimeMillis())
                    writeInstalled(current.filterNot { it.meta.id == meta.id } + installed)
                    backup.delete()
                    installed
                } catch (error: Exception) {
                    if (backup.isFile) {
                        Files.move(backup.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                    } else {
                        destination.delete()
                    }
                    throw error
                }
            }
        } catch (error: Exception) {
            temporary.delete()
            backup.delete()
            throw error
        }
    }

    fun setEnabled(repositoryUrl: String, id: String, enabled: Boolean) = synchronized(this) {
        require(validId(id)) { "Invalid provider source id." }
        val installed = readInstalled()
        require(installed.any { it.meta.repositoryUrl == repositoryUrl && it.meta.id == id }) { "Provider is not installed." }
        writeInstalled(installed.map {
            if (it.meta.repositoryUrl == repositoryUrl && it.meta.id == id) it.copy(enabled = enabled) else it
        })
    }

    fun removeProvider(repositoryUrl: String, id: String) = synchronized(this) {
        require(validId(id)) { "Invalid provider source id." }
        val providers = readInstalled()
        val removed = providers.firstOrNull { it.meta.repositoryUrl == repositoryUrl && it.meta.id == id }
        if (removed != null) {
            // Provider file names are content-independent but guarded by both the validated ID
            // and the metadata record, so no manifest value becomes a filesystem path.
            providerFile(id).delete()
            File(directory, "${providerFile(id).name}.tmp").delete()
            writeInstalled(providers.filterNot { it.meta.repositoryUrl == repositoryUrl && it.meta.id == id })
        }
    }

    fun removeRepository(url: String) = synchronized(this) {
        writeRepositories(readRepositories().filterNot { it.url == url })
        // Installed scripts are intentionally retained until explicitly removed by the user.
    }

    fun close() = client.close()

    private suspend fun fetchText(url: String, maximumBytes: Long): String = fetchBytes(url, maximumBytes).toString(Charsets.UTF_8)

    private suspend fun fetchBytes(url: String, maximumBytes: Long): ByteArray {
        validateHttpsUrl(url)
        val response = client.get(url)
        if (response.status.value !in 200..299) throw IllegalStateException("Provider repository returned HTTP ${response.status.value}.")
        val length = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        require(length == null || length <= maximumBytes) { "Provider response exceeds the size limit." }
        return ByteArrayOutputStream().use { output ->
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(16 * 1024)
            var total = 0L
            while (true) {
                val count = channel.readAvailable(buffer, 0, buffer.size)
                if (count == -1) break
                if (count == 0) continue
                total += count
                require(total <= maximumBytes) { "Provider response exceeds the size limit." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
    }

    private fun validateMeta(meta: AnimeProviderMeta) {
        require(validId(meta.id)) { "Invalid provider source id." }
        require(meta.type == "anime") { "Only anime providers are supported." }
        require(meta.name.isNotBlank() && meta.name.length <= MAX_NAME_LENGTH) { "Invalid provider name." }
        require(validVersion(meta.version)) { "Invalid provider version." }
        validateHttpsUrl(meta.file)
        validateHttpsUrl(meta.repositoryUrl)
    }

    private fun readRepositories(): List<AnimeProviderRepoCatalog> = runCatching {
        if (stateFile.isFile) json.decodeFromString<ProviderState>(stateFile.readText()).repositories else emptyList()
    }.getOrDefault(emptyList())

    private fun readInstalled(): List<InstalledAnimeProvider> = runCatching {
        if (!stateFile.isFile) emptyList()
        else json.decodeFromString<ProviderState>(stateFile.readText()).installed.filter {
            validId(it.meta.id) && providerFile(it.meta.id).isFile
        }.distinctBy { it.meta.id }
    }.getOrDefault(emptyList())

    private fun writeRepositories(repositories: List<AnimeProviderRepoCatalog>) {
        writeState(ProviderState(repositories = repositories, installed = readInstalled()))
    }

    private fun writeInstalled(installed: List<InstalledAnimeProvider>) {
        writeState(ProviderState(repositories = readRepositories(), installed = installed))
    }

    private fun writeState(state: ProviderState) {
        if (!directory.exists() && !directory.mkdirs()) throw IllegalStateException("Could not create provider storage.")
        val temp = File(directory, "state.json.tmp")
        try {
            FileOutputStream(temp).use { output ->
                output.write(json.encodeToString(state).toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            Files.move(temp.toPath(), stateFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (error: Exception) {
            temp.delete()
            throw error
        }
    }

    private fun resolveUrl(manifestUrl: String, entry: String): String {
        val parsed = URI(entry)
        return if (parsed.isAbsolute) parsed.toString() else URI(manifestUrl).resolve(parsed).toString()
    }

    private fun validateHttpsUrl(value: String) {
        val uri = runCatching { URI(value.trim()) }.getOrElse { throw IllegalArgumentException("Invalid provider URL.") }
        require(uri.scheme.equals("https", true) && !uri.host.isNullOrBlank() && uri.userInfo == null) {
            "Provider repositories and scripts must use HTTPS without embedded credentials."
        }
    }

    private fun validId(value: String): Boolean = value.matches(Regex("[a-z0-9][a-z0-9._-]{0,63}")) && !value.contains("..")

    private fun validVersion(value: String): Boolean = value.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+][A-Za-z0-9.-]+)?"))

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    @kotlinx.serialization.Serializable
    private data class ProviderState(
        val repositories: List<AnimeProviderRepoCatalog> = emptyList(),
        val installed: List<InstalledAnimeProvider> = emptyList(),
    )

    private companion object {
        const val CONNECT_TIMEOUT_MS = 8_000L
        const val REQUEST_TIMEOUT_MS = 12_000L
        const val MAX_MANIFEST_BYTES = 2L * 1024 * 1024
        const val MAX_PROVIDER_BYTES = 1024L * 1024
        const val MAX_NAME_LENGTH = 96
        const val MAX_DESCRIPTION_LENGTH = 2_000
        const val MAX_PROVIDERS_PER_REPOSITORY = 1_000
    }
}

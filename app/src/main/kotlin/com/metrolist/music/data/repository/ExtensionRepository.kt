package com.metrolist.music.data.repository

import android.system.Os
import com.metrolist.music.data.model.ExtensionConfig
import com.metrolist.music.data.model.validationError
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.File
import java.net.URL

/** Stores user-installed JSON extension definitions under filesDir/extensions. */
class ExtensionRepository(
    filesDir: File,
    private val client: HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        install(HttpTimeout) { requestTimeoutMillis = 20_000; connectTimeoutMillis = 10_000; socketTimeoutMillis = 20_000 }
    },
) {
    private val directory = File(filesDir, "extensions")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchIndex(url: String): List<ExtensionConfig> = withContext(Dispatchers.IO) {
        val body = downloadText(url)
        parseIndex(body).also { configs -> configs.forEach(::requireValid) }
    }

    /** Accepts a pasted single config, a JSON index, or a URL returning either form. */
    suspend fun addFromInput(input: String): List<ExtensionConfig> {
        val trimmed = input.trim()
        require(trimmed.isNotEmpty()) { "Paste extension JSON or an HTTP(S) URL." }
        val body = if (trimmed.contains("://")) {
            validateHttpUrl(trimmed)
            downloadText(trimmed)
        } else {
            trimmed
        }
        val configs = parseInput(body)
        configs.forEach(::requireValid)
        withContext(Dispatchers.IO) { configs.forEach(::persist) }
        return configs
    }

    suspend fun installed(): List<ExtensionConfig> = withContext(Dispatchers.IO) {
        if (!directory.isDirectory) return@withContext emptyList()
        directory.listFiles { file -> file.isFile && file.extension == "json" }
            .orEmpty()
            .sortedBy { it.name.lowercase() }
            .mapNotNull { file -> runCatching { json.decodeFromString<ExtensionConfig>(file.readText()) }.getOrNull() }
            .filter { it.validationError() == null }
    }

    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        if (!id.matches(Regex("[A-Za-z0-9._-]{1,80}"))) return@withContext
        File(directory, "$id.json").delete()
        Unit
    }

    fun close() = client.close()

    private suspend fun downloadText(url: String): String {
        validateHttpUrl(url)
        val response = client.get(url) {
            header("User-Agent", "Huni-Reader/1.0 (JSON extension client)")
        }
        if (response.status.value !in 200..299) {
            throw IllegalStateException("Request failed with HTTP ${response.status.value}.")
        }
        val body = response.bodyAsText()
        if (body.length > MAX_JSON_CHARS) throw IllegalStateException("Extension JSON is too large (limit ${MAX_JSON_CHARS / (1024 * 1024)} MiB).")
        return body
    }

    private fun parseInput(body: String): List<ExtensionConfig> {
        try {
            val one = json.decodeFromString<ExtensionConfig>(body)
            return listOf(one)
        } catch (_: SerializationException) {
            // Accept a plain array or {"extensions": [...]} index after trying one config.
        } catch (_: IllegalArgumentException) {
            // Ktor/serialization may surface malformed object shape as an argument error.
        }
        return parseIndex(body)
    }

    private fun parseIndex(body: String): List<ExtensionConfig> {
        val root = json.parseToJsonElement(body)
        val elements = when (root) {
            is JsonArray -> root
            is JsonObject -> root["extensions"]?.jsonArray
                ?: throw IllegalArgumentException("JSON must be an extension config, an array, or an object with an 'extensions' array.")
            else -> throw IllegalArgumentException("JSON must contain an extension config or extension list.")
        }
        if (elements.isEmpty()) throw IllegalArgumentException("The extension index is empty.")
        return elements.map { json.decodeFromJsonElement(ExtensionConfig.serializer(), it) }
    }

    private fun requireValid(config: ExtensionConfig) {
        config.validationError()?.let { throw IllegalArgumentException("${config.name.ifBlank { config.id }}: $it") }
    }

    private fun persist(config: ExtensionConfig) {
        if (!directory.exists() && !directory.mkdirs()) throw IllegalStateException("Could not create private extension storage.")
        val destination = File(directory, "${config.id}.json")
        val temporary = File(directory, "${config.id}.json.tmp")
        try {
            temporary.writeText(json.encodeToString(config))
            Os.rename(temporary.absolutePath, destination.absolutePath)
        } catch (error: Exception) {
            temporary.delete()
            throw IllegalStateException("Could not save extension '${config.name}': ${error.message ?: "storage error"}", error)
        }
    }

    private fun validateHttpUrl(url: String) {
        val parsed = runCatching { URL(url) }.getOrElse { throw IllegalArgumentException("Invalid URL: ${it.message}") }
        require(parsed.protocol == "https" || parsed.protocol == "http") { "Only HTTP and HTTPS extension URLs are allowed." }
        require(parsed.host.isNotBlank() && parsed.userInfo == null) { "Extension URL must have a host and cannot contain embedded credentials." }
    }

}

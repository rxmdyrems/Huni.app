package com.metrolist.music.data.engine

import com.metrolist.music.data.model.AnimeEpisode
import com.metrolist.music.data.model.AnimeMediaItem
import com.metrolist.music.data.model.AnimeProviderMeta
import com.metrolist.music.data.model.AnimeVideoSource
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.ClassShutter
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.EvaluatorException
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Runs one user-installed provider in an isolated Rhino scope. Host access is limited
 * to the Huni.fetch(url) HTTP bridge; Java packages/classes are not visible to scripts.
 * Calls are synchronized because a provider scope must never be entered concurrently.
 */
class AnimeJavaScriptRuntime : AutoCloseable {
    private val lock = Any()
    private var providerScope: Scriptable? = null
    private var providerId: String? = null
    private var currentDeadlineNs = 0L
    private var networkRequestCount = 0
    private var networkBytes = 0
    private val factory = object : ContextFactory() {
        override fun makeContext(): Context = super.makeContext().apply {
            optimizationLevel = -1
            instructionObserverThreshold = INSTRUCTION_CHUNK
            setClassShutter(ClassShutter { false })
        }

        override fun observeInstructionCount(cx: Context, instructionCount: Int) {
            if (currentDeadlineNs != 0L && System.nanoTime() > currentDeadlineNs) {
                throw EvaluatorException("Provider JavaScript execution timed out.")
            }
        }
    }

    fun load(meta: AnimeProviderMeta, script: String) = synchronized(lock) {
        require(script.length <= MAX_SCRIPT_CHARS) { "Provider script exceeds the size limit." }
        require(meta.type == "anime") { "Unsupported provider type." }
        closeContext()
        factory.call { cx ->
            beginBudget(LOAD_TIMEOUT_NS)
            try {
                val scope = cx.initStandardObjects()
                installHostApi(cx, scope)
                cx.evaluateString(scope, script, "provider:${meta.id}", 1, null)
                REQUIRED_METHODS.forEach { method ->
                    require(ScriptableObject.getProperty(scope, method) is Function) {
                        "Provider is missing the required $method() function."
                    }
                }
                val info = callOptional(cx, scope, "getInfo", emptyArray()) as? Scriptable
                val declaredName = info?.let { propertyString(it, "name") }
                require(declaredName.isNullOrBlank() || declaredName == meta.name) {
                    "Provider metadata name does not match its repository entry."
                }
                val declaredVersion = info?.let { propertyString(it, "version") }
                require(declaredVersion.isNullOrBlank() || declaredVersion == meta.version) {
                    "Provider metadata version does not match its repository entry."
                }
                providerScope = scope
                providerId = meta.id
            } finally {
                currentDeadlineNs = 0L
            }
        }
    }

    fun search(query: String, page: Int = 1): List<AnimeMediaItem> = invokeList("search", arrayOf(query, page)) { item ->
        AnimeMediaItem(
            id = (item.string("id") ?: item.string("url") ?: throw IllegalArgumentException("Provider item is missing id.")).take(MAX_ID_CHARS),
            title = item.string("title")?.take(MAX_FIELD_CHARS)?.takeIf(String::isNotBlank)
                ?: throw IllegalArgumentException("Provider item is missing title."),
            url = item.string("url")?.take(MAX_URL_CHARS)?.also(::validateHttpUrl)
                ?: throw IllegalArgumentException("Provider item is missing URL."),
            cover = item.string("cover")?.take(MAX_URL_CHARS)?.also { if (it.isNotBlank()) validateHttpUrl(it) }.orEmpty(),
            type = "anime",
            sourceId = item.string("sourceId")?.take(MAX_ID_CHARS) ?: providerId.orEmpty(),
            description = item.string("description")?.take(MAX_FIELD_CHARS).orEmpty(),
        )
    }

    fun episodes(seriesUrl: String): List<AnimeEpisode> {
        validateHttpUrl(seriesUrl)
        return invokeList("getEpisodes", arrayOf(seriesUrl)) { item ->
            AnimeEpisode(
                id = (item.string("id") ?: item.string("url") ?: "").take(MAX_ID_CHARS),
                title = item.string("title")?.take(MAX_FIELD_CHARS)?.takeIf(String::isNotBlank)
                    ?: throw IllegalArgumentException("Provider episode is missing title."),
                number = item.optDouble("number").takeUnless { it.isNaN() },
                url = item.string("url")?.take(MAX_URL_CHARS)?.also(::validateHttpUrl)
                    ?: throw IllegalArgumentException("Provider episode is missing URL."),
                sourceId = providerId.orEmpty(),
            )
        }
    }

    fun videoSources(episodeUrl: String): List<AnimeVideoSource> {
        validateHttpUrl(episodeUrl)
        return invokeList("getVideoSources", arrayOf(episodeUrl)) { item ->
            AnimeVideoSource(
                url = item.string("url")?.take(MAX_URL_CHARS)?.also(::validateHttpUrl)
                    ?: throw IllegalArgumentException("Provider video source is missing URL."),
                quality = item.string("quality")?.take(64).orEmpty(),
                container = item.string("container")?.take(16)?.lowercase()?.takeIf { it in setOf("hls", "mp4") }
                    ?: throw IllegalArgumentException("Provider video container must be hls or mp4."),
                headers = item.optJSONObject("headers")?.let { headers ->
                    buildMap {
                        val keys = headers.keys()
                        while (keys.hasNext()) {
                            require(size < MAX_HEADERS) { "Provider returned too many stream headers." }
                            val key = keys.next()
                            if (key.length <= 128) put(key, headers.optString(key).take(2048))
                        }
                    }
                }.orEmpty(),
            )
        }
    }

    private fun <T> invokeList(method: String, args: Array<Any>, convert: (JSONObject) -> T): List<T> = synchronized(lock) {
        val scope = providerScope ?: throw IllegalStateException("Anime provider runtime is not loaded.")
        beginBudget(CALL_TIMEOUT_NS)
        try {
            factory.call { activeContext ->
                val raw = callRequired(activeContext, scope, method, args)
                val jsonObject = ScriptableObject.getProperty(scope, "JSON") as Scriptable
                val stringify = ScriptableObject.getProperty(jsonObject, "stringify") as Function
                val json = Context.toString(stringify.call(activeContext, scope, jsonObject, arrayOf(raw)))
                require(json.length <= MAX_RESULT_JSON_CHARS) { "Provider result exceeds the size limit." }
                val array = JSONArray(json)
                require(array.length() <= MAX_RESULT_ITEMS) { "Provider returned too many results." }
                (0 until array.length()).map { index ->
                    val value = array.optJSONObject(index) ?: throw IllegalArgumentException("Provider returned an invalid item.")
                    convert(value)
                }
            }
        } finally {
            currentDeadlineNs = 0L
        }
    }

    private fun installHostApi(cx: Context, scope: Scriptable) {
        val huni = cx.newObject(scope)
        val fetch = object : BaseFunction() {
            override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<out Any>): Any {
                val value = args.firstOrNull()?.let(Context::toString).orEmpty()
                val response = fetchText(value)
                return response
            }
        }
        ScriptableObject.putProperty(huni, "fetch", fetch)
        ScriptableObject.putProperty(scope, "Huni", huni)
        ScriptableObject.putProperty(scope, "console", cx.newObject(scope).also { console ->
            val log = object : BaseFunction() {
                override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<out Any>): Any = org.mozilla.javascript.Undefined.instance
            }
            ScriptableObject.putProperty(console, "log", log)
            ScriptableObject.putProperty(console, "warn", log)
            ScriptableObject.putProperty(console, "error", log)
        })
    }

    private fun fetchText(value: String): String {
        validateHttpUrl(value)
        checkDeadline()
        networkRequestCount++
        require(networkRequestCount <= MAX_REQUESTS_PER_CALL) { "Provider exceeded the request limit." }
        val connection = URL(value).openConnection() as HttpURLConnection
        // Connect and first blocking read can happen sequentially. Give each at most
        // half the remaining budget, then re-check the wall clock between reads.
        val timeout = (remainingTimeoutMs() / 2).coerceAtLeast(1)
        connection.connectTimeout = timeout
        connection.readTimeout = timeout
        connection.requestMethod = "GET"
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("User-Agent", "Huni-AnimeProvider/1.0")
        try {
            val status = connection.responseCode
            require(status in 200..299) { "Provider request failed with HTTP $status." }
            val length = connection.contentLengthLong
            require(length < 0 || length <= MAX_NETWORK_BYTES_PER_CALL - networkBytes) { "Provider response exceeds the size limit." }
            val stream = connection.inputStream
            return stream.use { input ->
                ByteArrayOutputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    while (true) {
                        checkDeadline()
                        val count = input.read(buffer)
                        if (count < 0) break
                        networkBytes += count
                        require(networkBytes <= MAX_NETWORK_BYTES_PER_CALL) { "Provider exceeded the network byte limit." }
                        output.write(buffer, 0, count)
                    }
                    output.toString(Charsets.UTF_8.name())
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun callRequired(cx: Context, scope: Scriptable, method: String, args: Array<Any>): Any {
        val function = ScriptableObject.getProperty(scope, method) as? Function
            ?: throw IllegalArgumentException("Provider is missing the $method() function.")
        return function.call(cx, scope, scope, args.map { Context.javaToJS(it, scope) }.toTypedArray())
    }

    private fun callOptional(cx: Context, scope: Scriptable, method: String, args: Array<Any>): Any? {
        val function = ScriptableObject.getProperty(scope, method) as? Function ?: return null
        return function.call(cx, scope, scope, args.map { Context.javaToJS(it, scope) }.toTypedArray())
    }

    private fun propertyString(scope: Scriptable, key: String): String? =
        ScriptableObject.getProperty(scope, key).takeUnless { it === Scriptable.NOT_FOUND }?.let(Context::toString)

    private fun validateHttpUrl(value: String) {
        val uri = runCatching { URI(value) }.getOrElse { throw IllegalArgumentException("Provider returned an invalid URL.") }
        require((uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) && !uri.host.isNullOrBlank() && uri.userInfo == null) {
            "Provider URLs must use HTTP(S) and cannot include embedded credentials."
        }
    }

    private fun closeContext() {
        providerScope = null
        providerId = null
    }

    private fun beginBudget(durationNs: Long) {
        currentDeadlineNs = System.nanoTime() + durationNs
        networkRequestCount = 0
        networkBytes = 0
    }

    private fun checkDeadline() {
        if (currentDeadlineNs == 0L || System.nanoTime() >= currentDeadlineNs) {
            throw EvaluatorException("Provider call timed out.")
        }
    }

    private fun remainingTimeoutMs(): Int {
        val remainingNs = currentDeadlineNs - System.nanoTime()
        if (remainingNs <= 0L) throw EvaluatorException("Provider call timed out.")
        return minOf(NETWORK_TIMEOUT_MS.toLong(), TimeUnit.NANOSECONDS.toMillis(remainingNs).coerceAtLeast(1L))
            .toInt().coerceAtLeast(1)
    }

    override fun close() = synchronized(lock) { closeContext() }

    private fun JSONObject.string(key: String): String? = opt(key).takeUnless { it == JSONObject.NULL }?.toString()

    private companion object {
        const val INSTRUCTION_CHUNK = 10_000
        const val MAX_SCRIPT_CHARS = 1_000_000
        const val MAX_RESULT_ITEMS = 500
        const val MAX_RESULT_JSON_CHARS = 2 * 1024 * 1024
        const val MAX_REQUESTS_PER_CALL = 8
        const val MAX_NETWORK_BYTES_PER_CALL = 2 * 1024 * 1024
        const val MAX_URL_CHARS = 4_096
        const val MAX_ID_CHARS = 128
        const val MAX_FIELD_CHARS = 8_192
        const val MAX_HEADERS = 32
        const val NETWORK_TIMEOUT_MS = 8_000
        val REQUIRED_METHODS = listOf("search", "getEpisodes", "getVideoSources")
        val LOAD_TIMEOUT_NS = TimeUnit.SECONDS.toNanos(8)
        val CALL_TIMEOUT_NS = TimeUnit.SECONDS.toNanos(8)
    }
}

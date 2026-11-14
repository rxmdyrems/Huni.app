/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.metrolist.music.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val description: String,
    val releaseDate: String,
    val assets: List<ReleaseAsset>,
    val versionCode: Int? = null,
)

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long,
    val architecture: String,
    val variant: String // "foss" or "gms"
)

object Updater {
    private val client = HttpClient()
    var lastCheckTime = -1L
        private set
    
    private var cachedReleaseInfo: ReleaseInfo? = null
    private var cachedAllReleases: List<ReleaseInfo> = emptyList()
    
    private const val CHECK_INTERVAL_MILLIS = 24 * 60 * 60 * 1000L
    private const val GITHUB_API_BASE = "https://api.github.com/repos/rxmdyrems/Huni.app"

    /** The release API has no versionCode field, so read it from the tagged app build file. */
    private suspend fun getVersionCodeForTag(tagName: String): Int {
        require(tagName.matches(Regex("v[0-9A-Za-z][0-9A-Za-z._-]{0,63}"))) { "Invalid release tag." }
        val buildFile = client.get(
            "https://raw.githubusercontent.com/rxmdyrems/Huni.app/$tagName/app/build.gradle.kts"
        ).bodyAsText()
        return parseVersionCode(buildFile)
    }

    internal fun parseVersionCode(buildFile: String): Int =
        Regex("(?m)^[ \\t]*versionCode[ \\t]*=[ \\t]*(\\d+)[ \\t]*$")
            .find(buildFile)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
            ?: error("Could not find versionCode in the tagged app build file.")

    fun isUpdateAvailable(currentVersionCode: Int, latestVersionCode: Int): Boolean =
        latestVersionCode > currentVersionCode

    /**
     * Get the current app's architecture and variant
     */
    private fun getCurrentAppVariant(): Pair<String, String> {
        val architecture = BuildConfig.ARCHITECTURE
        val variant = if (BuildConfig.CAST_AVAILABLE) "gms" else "foss"
        return architecture to variant
    }

    /**
     * Parse release assets from GitHub API response
     */
    private fun parseAssets(assetsArray: JSONArray): List<ReleaseAsset> {
        val assets = mutableListOf<ReleaseAsset>()
        
        for (i in 0 until assetsArray.length()) {
            val asset = assetsArray.getJSONObject(i)
            val name = asset.getString("name")
            
            // Skip non-APK files
            if (!name.endsWith(".apk")) continue
            
            val downloadUrl = asset.getString("browser_download_url")
            val size = asset.getLong("size")
            
            // Parse architecture and variant from filename
            val (arch, variant) = when {
                name == "Huni-with-Google-Cast.apk" -> "universal" to "gms"
                name == "Huni-izzy.apk" -> null to null
                name == "Huni.apk" || name.startsWith("Huni-") -> "universal" to "foss"
                name.startsWith("app-") && name.endsWith("-release.apk") -> {
                    val arch = name.removePrefix("app-").removeSuffix("-release.apk")
                    arch to "foss"
                }
                name.startsWith("app-") && name.endsWith("-with-Google-Cast.apk") -> {
                    val arch = name.removePrefix("app-").removeSuffix("-with-Google-Cast.apk")
                    arch to "gms"
                }
                else -> null to null
            }
            
            if (arch != null && variant != null) {
                assets.add(ReleaseAsset(name, downloadUrl, size, arch, variant))
            }
        }
        
        return assets
    }

    /**
     * Fetch latest release from GitHub API
     */
    suspend fun getLatestRelease(forceRefresh: Boolean = false): Result<ReleaseInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Return cached if available and not forcing refresh
                if (cachedReleaseInfo != null && !forceRefresh) {
                    return@runCatching cachedReleaseInfo!!
                }
                
                val response = client.get("$GITHUB_API_BASE/releases/latest")
                    .bodyAsText()
                val json = JSONObject(response)
                
                val tagName = json.getString("tag_name")
                val releaseInfo = ReleaseInfo(
                    tagName = tagName,
                    versionName = tagName.removePrefix("v"),
                    description = json.getString("body"),
                    releaseDate = json.getString("published_at"),
                    assets = parseAssets(json.getJSONArray("assets")),
                    versionCode = getVersionCodeForTag(tagName),
                )
                
                cachedReleaseInfo = releaseInfo
                lastCheckTime = System.currentTimeMillis()
                releaseInfo
            }
        }

    /**
     * Fetch all releases from GitHub API (paginated)
     */
    suspend fun getAllReleases(forceRefresh: Boolean = false): Result<List<ReleaseInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (cachedAllReleases.isNotEmpty() && !forceRefresh) {
                    return@runCatching cachedAllReleases
                }
                
                val releases = mutableListOf<ReleaseInfo>()
                var page = 1
                var hasMore = true
                
                while (hasMore && page <= 10) { // Limit to 10 pages
                    val response = client.get("$GITHUB_API_BASE/releases?page=$page&per_page=30")
                        .bodyAsText()
                    val json = JSONArray(response)
                    
                    if (json.length() == 0) {
                        hasMore = false
                        break
                    }
                    
                    for (i in 0 until json.length()) {
                        val releaseObj = json.getJSONObject(i)
                        releases.add(ReleaseInfo(
                            tagName = releaseObj.getString("tag_name"),
                            versionName = releaseObj.getString("tag_name").removePrefix("v"),
                            description = releaseObj.getString("body"),
                            releaseDate = releaseObj.getString("published_at"),
                            assets = parseAssets(releaseObj.getJSONArray("assets"))
                        ))
                    }
                    
                    page++
                }
                
                cachedAllReleases = releases
                releases
            }
        }

    /**
     * Get the download URL for the correct app variant
     */
    fun getDownloadUrlForCurrentVariant(releaseInfo: ReleaseInfo): String? {
        val (currentArch, currentVariant) = getCurrentAppVariant()
        
        return releaseInfo.assets
            .find { it.architecture == currentArch && it.variant == currentVariant }
            ?.downloadUrl
    }

    suspend fun downloadAndInstall(context: Context, releaseInfo: ReleaseInfo): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val apkUrl = getDownloadUrlForCurrentVariant(releaseInfo)
                    ?: error("No APK asset is available for this build")
                val apkBytes = client.get(apkUrl).body<ByteArray>()
                val checksumText = client.get("$apkUrl.sha256").bodyAsText()
                val expected = checksumText.trim().substringBefore(" ").substringBefore("\t")
                val actual = MessageDigest.getInstance("SHA-256")
                    .digest(apkBytes).joinToString("") { "%02x".format(it) }
                check(expected.equals(actual, ignoreCase = true)) { "Downloaded update checksum does not match" }
                val apkFile = File(context.cacheDir, "huni-update.apk")
                apkFile.writeBytes(apkBytes)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", apkFile)
                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }

    /**
     * Check if update is needed (respects 2-hour cache)
     */
    suspend fun checkForUpdate(forceRefresh: Boolean = false): Result<Pair<ReleaseInfo?, Boolean>> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Check if we should fetch (2 hour interval)
                val shouldFetch = forceRefresh || 
                    (System.currentTimeMillis() - lastCheckTime) > CHECK_INTERVAL_MILLIS
                
                if (!shouldFetch && cachedReleaseInfo != null) {
                    val latestVersionCode = cachedReleaseInfo!!.versionCode
                        ?: error("The cached release has no versionCode.")
                    val hasUpdate = isUpdateAvailable(BuildConfig.VERSION_CODE, latestVersionCode)
                    return@runCatching cachedReleaseInfo!! to hasUpdate
                }
                
                val result = getLatestRelease(forceRefresh = true)
                if (result.isSuccess) {
                    val releaseInfo = result.getOrThrow()
                    val latestVersionCode = releaseInfo.versionCode
                        ?: error("The release has no versionCode.")
                    val hasUpdate = isUpdateAvailable(BuildConfig.VERSION_CODE, latestVersionCode)
                    releaseInfo to hasUpdate
                } else {
                    throw result.exceptionOrNull() ?: Exception("Unknown error")
                }
            }
        }

    /**
     * Get the latest release info (cached)
     */
    fun getCachedLatestRelease(): ReleaseInfo? = cachedReleaseInfo
}

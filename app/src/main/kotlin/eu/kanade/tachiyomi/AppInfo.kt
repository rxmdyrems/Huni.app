package eu.kanade.tachiyomi

import com.metrolist.music.BuildConfig

/**
 * Small Mihon host API compatibility surface used by dynamically loaded extensions.
 * MangaDex 1.6.x calls [getVersionName] while constructing its request user agent.
 */
object AppInfo {
    fun getVersionName(): String = BuildConfig.BASE_VERSION_NAME
}

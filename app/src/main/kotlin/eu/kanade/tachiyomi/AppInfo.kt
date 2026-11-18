package eu.kanade.tachiyomi

import com.metrolist.music.BuildConfig

/** Mihon host API compatibility surface used by dynamically loaded extensions. */
object AppInfo {
    fun getVersionName(): String = BuildConfig.BASE_VERSION_NAME
}

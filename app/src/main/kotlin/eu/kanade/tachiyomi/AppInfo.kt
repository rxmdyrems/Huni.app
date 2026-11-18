package eu.kanade.tachiyomi

/**
 * Host information exposed to dynamically loaded Mihon extensions.
 *
 * This ABI is consumed by current extension-lib 1.3+ extensions, including
 * MangaDex, and must remain in the eu.kanade.tachiyomi package.
 */
@Suppress("UNUSED")
object AppInfo {
    fun getVersionCode(): Int = com.metrolist.music.BuildConfig.VERSION_CODE

    fun getVersionName(): String = com.metrolist.music.BuildConfig.VERSION_NAME

    fun getSupportedImageMimeTypes(): List<String> = listOf(
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/gif",
        "image/avif",
    )
}

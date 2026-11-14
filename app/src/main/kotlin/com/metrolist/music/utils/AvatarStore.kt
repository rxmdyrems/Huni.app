package com.metrolist.music.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.net.Uri
import android.system.Os
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.metrolist.music.R
import java.io.File
import java.io.InputStream

/** Owns the single app-private custom profile avatar used by onboarding, Home, and About. */
class AvatarStore(private val filesDir: File) {
    private val profileDir = File(filesDir, "profile")
    val avatarFile: File get() = File(profileDir, AVATAR_FILE_NAME)

    /** Returns false for cancellation or any unreadable/invalid image; the previous file is retained. */
    fun replace(uri: Uri?, open: (Uri) -> InputStream?): Boolean {
        if (uri == null) return false
        val input = try { open(uri) } catch (_: Exception) { null } ?: return false
        return input.use { stream -> replaceFrom(stream) }
    }

    /** Captures the current bytes so callers can roll back if persisting the avatar key fails. */
    fun snapshot(): ByteArray? = avatarFile.takeIf { it.isFile }?.readBytes()

    /** Atomically restores a prior snapshot; a null snapshot means there was no custom photo. */
    fun restore(snapshot: ByteArray?) {
        if (snapshot == null) {
            remove()
            return
        }
        val temp = File(profileDir, "$AVATAR_FILE_NAME.tmp")
        try {
            if (!profileDir.exists() && !profileDir.mkdirs()) return
            FileOutputStreamCompat.write(temp) { output ->
                output.write(snapshot)
                output.fd.sync()
            }
            Os.rename(temp.absolutePath, avatarFile.absolutePath)
        } catch (_: Exception) {
            temp.delete()
        }
    }

    /** Decode before touching the current file, then atomically rename a fully written replacement. */
    fun replaceFrom(input: InputStream): Boolean {
        val bitmap = try { BitmapFactory.decodeStream(input) } catch (_: Exception) { null } ?: return false
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            bitmap.recycle()
            return false
        }
        val size = minOf(bitmap.width, bitmap.height)
        val cropped = try {
            Bitmap.createBitmap(bitmap, (bitmap.width - size) / 2, (bitmap.height - size) / 2, size, size)
        } catch (_: Exception) {
            bitmap.recycle()
            return false
        }
        val temp = File(profileDir, "$AVATAR_FILE_NAME.tmp")
        return try {
            if (!profileDir.exists() && !profileDir.mkdirs()) return false
            FileOutputStreamCompat.write(temp) { output ->
                @Suppress("DEPRECATION")
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    Bitmap.CompressFormat.WEBP
                }
                check(cropped.compress(format, 85, output))
                output.fd.sync()
            }
            Os.rename(temp.absolutePath, avatarFile.absolutePath)
            true
        } catch (_: Exception) {
            temp.delete()
            false
        } finally {
            if (cropped !== bitmap) cropped.recycle()
            bitmap.recycle()
        }
    }

    /** Selecting a preset invalidates the prior custom image. */
    fun selectPreset() = remove()

    /** Full profile/data reset uses the same canonical deletion path. */
    fun remove() {
        avatarFile.delete()
        File(profileDir, "$AVATAR_FILE_NAME.tmp").delete()
    }

    fun exists(): Boolean = avatarFile.isFile && avatarFile.length() > 0

    companion object {
        const val AVATAR_FILE_NAME = "avatar.webp"
        const val CUSTOM_AVATAR_ID = "file"

        private val presetDrawables = mapOf(
            "avatar_amber" to R.drawable.avatar_amber,
            "avatar_violet" to R.drawable.avatar_violet,
            "avatar_green" to R.drawable.avatar_green,
            "avatar_maroon" to R.drawable.avatar_maroon,
            "avatar_ocean" to R.drawable.avatar_ocean,
            "avatar_teal" to R.drawable.avatar_teal,
            "avatar_rose" to R.drawable.avatar_rose,
            "avatar_orange" to R.drawable.avatar_orange,
            "avatar_indigo" to R.drawable.avatar_indigo,
            "avatar_slate" to R.drawable.avatar_slate,
            "avatar_mint" to R.drawable.avatar_mint,
            "avatar_coral" to R.drawable.avatar_coral,
        )

        fun customAvatarId(fallbackPreset: String, revision: Long = System.nanoTime()): String =
            "$CUSTOM_AVATAR_ID:${validPreset(fallbackPreset)}:$revision"

        /** Derive a fresh cache revision for each successful replacement, including one after process restart. */
        fun nextCustomAvatarId(fallbackPreset: String, previousId: String): String {
            val previousRevision = previousId.takeIf(::isCustomAvatarId)
                ?.substringAfterLast(':')
                ?.toLongOrNull()
                ?: Long.MIN_VALUE
            val revision = maxOf(System.nanoTime(), previousRevision + 1)
            return customAvatarId(fallbackPreset, revision)
        }

        fun isCustomAvatarId(id: String): Boolean = id == CUSTOM_AVATAR_ID || id.startsWith("$CUSTOM_AVATAR_ID:")

        fun fallbackPreset(id: String): String {
            val candidate = if (id.startsWith("$CUSTOM_AVATAR_ID:")) {
                id.removePrefix("$CUSTOM_AVATAR_ID:").substringBefore(':')
            } else {
                id
            }
            return candidate.takeIf(presetDrawables::containsKey) ?: "avatar_amber"
        }

        private fun validPreset(id: String): String = id.takeIf(presetDrawables::containsKey) ?: "avatar_amber"

        internal fun drawableFor(id: String): Int = presetDrawables[fallbackPreset(id)] ?: R.drawable.avatar_amber

        /** Stable per-revision cache keys ensure replaced bytes at the canonical path are never rendered stale. */
        fun customAvatarCacheKey(id: String): String =
            "huni-profile-avatar-${id.substringAfterLast(':', "legacy")}"
    }
}

/** Shared avatar renderer for onboarding, Home, and About. */
@Composable
fun UserAvatarImage(
    avatarId: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = androidx.compose.runtime.remember(context) { AvatarStore(context.filesDir) }
    if (AvatarStore.isCustomAvatarId(avatarId) && store.exists()) {
        val revision = AvatarStore.customAvatarCacheKey(avatarId)
        val imageUri = Uri.fromFile(store.avatarFile).buildUpon().appendQueryParameter("revision", revision).build()
        val request = ImageRequest.Builder(context)
            .data(imageUri)
            .memoryCacheKey(revision)
            .diskCacheKey(revision)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        Image(
            painter = painterResource(AvatarStore.drawableFor(avatarId)),
            contentDescription = contentDescription ?: "Profile avatar",
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}

private object FileOutputStreamCompat {
    fun write(file: File, block: (java.io.FileOutputStream) -> Unit) {
        java.io.FileOutputStream(file).use(block)
    }
}

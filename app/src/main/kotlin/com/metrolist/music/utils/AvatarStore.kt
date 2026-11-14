package com.metrolist.music.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.net.Uri
import android.system.Os
import java.io.File
import java.io.InputStream

/** Owns the single app-private custom profile avatar used by onboarding and About. */
class AvatarStore(private val filesDir: File) {
    private val profileDir = File(filesDir, "profile")
    val avatarFile: File get() = File(profileDir, AVATAR_FILE_NAME)

    /** Returns false for cancellation or any unreadable/invalid image; the previous file is retained. */
    fun replace(uri: Uri?, open: (Uri) -> InputStream?): Boolean {
        if (uri == null) return false
        val input = try { open(uri) } catch (_: Exception) { null } ?: return false
        return input.use { stream -> replaceFrom(stream) }
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

        fun customAvatarId(fallbackPreset: String, revision: Long = System.nanoTime()): String =
            "$CUSTOM_AVATAR_ID:${validPreset(fallbackPreset)}:$revision"

        fun isCustomAvatarId(id: String): Boolean = id == CUSTOM_AVATAR_ID || id.startsWith("$CUSTOM_AVATAR_ID:")

        fun fallbackPreset(id: String): String {
            val candidate = if (id.startsWith("$CUSTOM_AVATAR_ID:")) id.removePrefix("$CUSTOM_AVATAR_ID:").substringBefore(':') else id.takeIf { it.startsWith("avatar_") } ?: "avatar_amber"
            return validPreset(candidate)
        }

        private fun validPreset(id: String): String =
            id.takeIf { it.startsWith("avatar_") && it.matches(Regex("[a-z_]+")) } ?: "avatar_amber"
    }
}

private object FileOutputStreamCompat {
    fun write(file: File, block: (java.io.FileOutputStream) -> Unit) {
        java.io.FileOutputStream(file).use(block)
    }
}

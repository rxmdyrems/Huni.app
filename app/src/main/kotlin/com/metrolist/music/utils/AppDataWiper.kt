package com.metrolist.music.utils

import android.content.Context
import android.content.Intent
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.datastore.preferences.core.edit
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.playback.MusicService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AppDataWiper {
    suspend fun wipe(context: Context): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.stopService(Intent(context, MusicService::class.java))
            context.stopService(Intent(context, ExoDownloadService::class.java))
            context.dataStore.edit { it.clear() }
            resetPreferencesSnapshot()
            check(context.getSharedPreferences("com.metrolist.music_preferences", Context.MODE_PRIVATE).edit().clear().commit())
            context.fileList().forEach { context.deleteFile(it) }
            check(File(context.filesDir, "profile").deleteRecursively())
            listOf("song.db", "reader.db").forEach { name ->
                context.deleteDatabase(name)
                File(context.getDatabasePath(name).path + "-wal").delete()
                File(context.getDatabasePath(name).path + "-shm").delete()
            }
            context.cacheDir.deleteRecursively()
            context.externalCacheDirs.forEach { it?.deleteRecursively() }
            context.noBackupFilesDir.deleteRecursively()
            runCatching { CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush() }
            runCatching { WebStorage.getInstance().deleteAllData() }
            true
        }.getOrElse { false }
    }
}

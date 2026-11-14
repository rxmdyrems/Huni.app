package com.metrolist.music.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AvatarStoreTest {
    private lateinit var directory: File
    private lateinit var store: AvatarStore

    @Before fun setUp() {
        directory = File((ApplicationProvider.getApplicationContext<Context>()).cacheDir, "avatar-store-test").apply {
            deleteRecursively()
            mkdirs()
        }
        store = AvatarStore(directory)
    }

    @Test fun initialSelectionCopiesDecodedImageToCanonicalFile() {
        val uri = android.net.Uri.parse("content://huni.test/first")
        assertTrue(store.replace(uri) { requested ->
            assertEquals(uri, requested)
            image(0xffff0000)
        })
        assertTrue(store.exists())
        assertEquals("avatar.webp", store.avatarFile.name)
        assertNotNull(BitmapFactory.decodeFile(store.avatarFile.absolutePath))
    }

    @Test fun twoSuccessiveReplacementsReplaceImageAndSurviveStoreRestart() {
        assertTrue(store.replaceFrom(image(0xffff0000)))
        val initialBytes = store.avatarFile.readBytes()
        assertTrue(store.replaceFrom(image(0xff0000ff)))
        val firstReplacement = store.avatarFile.readBytes()
        assertNotEquals(initialBytes.toList(), firstReplacement.toList())
        assertTrue(store.replaceFrom(image(0xff00ff00)))
        val finalBytes = store.avatarFile.readBytes()
        assertNotEquals(firstReplacement.toList(), finalBytes.toList())
        assertNotEquals(initialBytes.toList(), finalBytes.toList())
        val restartedStore = AvatarStore(directory)
        assertTrue(restartedStore.exists())
        assertArrayEquals(finalBytes, restartedStore.avatarFile.readBytes())
    }

    @Test fun cancellationLeavesCurrentAvatarUnchanged() {
        assertTrue(store.replaceFrom(image(0xff00ff00)))
        val before = store.avatarFile.readBytes()
        assertFalse(store.replace(null) { error("Cancelled picker must not open a URI") })
        assertArrayEquals(before, store.avatarFile.readBytes())
    }

    @Test fun unreadableReplacementKeepsOldAvatar() {
        assertTrue(store.replaceFrom(image(0xffffff00)))
        val before = store.avatarFile.readBytes()
        val uri = android.net.Uri.parse("content://huni.test/unreadable")
        assertFalse(store.replace(uri) { throw IOException("Simulated unreadable picker content") })
        assertArrayEquals(before, store.avatarFile.readBytes())
    }

    @Test fun removalDeletesCustomAvatar() {
        assertTrue(store.replaceFrom(image(0xff123456)))
        store.remove()
        assertFalse(store.exists())
    }

    @Test fun customAvatarTracksAndRestoresSelectedFallbackPreset() {
        val encoded = AvatarStore.customAvatarId("avatar_ocean", 123L)
        assertTrue(AvatarStore.isCustomAvatarId(encoded))
        assertEquals("avatar_ocean", AvatarStore.fallbackPreset(encoded))
        assertEquals("avatar_amber", AvatarStore.fallbackPreset("file"))
        assertEquals("avatar_teal", AvatarStore.fallbackPreset("avatar_teal"))
        assertEquals("avatar_amber", AvatarStore.fallbackPreset("avatar_missing"))
    }

    @Test fun replacingCustomAvatarCreatesANewerCacheRevision() {
        val original = AvatarStore.customAvatarId("avatar_ocean", 123L)
        val replacement = AvatarStore.nextCustomAvatarId("avatar_ocean", original)
        assertTrue(AvatarStore.isCustomAvatarId(replacement))
        assertEquals("avatar_ocean", AvatarStore.fallbackPreset(replacement))
        assertNotEquals(original, replacement)
        assertTrue(replacement.substringAfterLast(':').toLong() > 123L)
    }

    @Test fun presetSelectionInvalidatesCustomAvatar() {
        assertTrue(store.replaceFrom(image(0xffabcdef)))
        store.selectPreset()
        assertFalse(store.exists())
    }

    @Test fun restoreRollsBackAReplacementWhenPreferencePersistenceFails() {
        assertTrue(store.replaceFrom(image(0xffabcdef)))
        val previous = requireNotNull(store.snapshot())
        assertTrue(store.replaceFrom(image(0xff123456)))
        store.restore(previous)
        assertArrayEquals(previous, store.avatarFile.readBytes())
    }

    private fun image(color: Long): ByteArrayInputStream {
        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(color.toInt()) }
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        bitmap.recycle()
        return ByteArrayInputStream(bytes.toByteArray())
    }
}

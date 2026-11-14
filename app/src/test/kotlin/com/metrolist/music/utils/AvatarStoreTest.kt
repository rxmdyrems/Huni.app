package com.metrolist.music.utils

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
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
        assertNotNull(Bitmap.decodeFile(store.avatarFile.absolutePath))
    }

    @Test fun repeatedReplacementReplacesImageAndCanReloadFromDisk() {
        assertTrue(store.replaceFrom(image(0xffff0000)))
        val oldBytes = store.avatarFile.readBytes()
        assertTrue(store.replaceFrom(image(0xff0000ff)))
        assertNotEquals(oldBytes.toList(), store.avatarFile.readBytes().toList())
        assertTrue(AvatarStore(directory).exists())
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
        assertFalse(store.replaceFrom(ByteArrayInputStream(byteArrayOf(1, 2, 3))))
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
    }

    @Test fun presetSelectionInvalidatesCustomAvatar() {
        assertTrue(store.replaceFrom(image(0xffabcdef)))
        store.selectPreset()
        assertFalse(store.exists())
    }

    private fun image(color: Int): ByteArrayInputStream {
        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        bitmap.recycle()
        return ByteArrayInputStream(bytes.toByteArray())
    }
}

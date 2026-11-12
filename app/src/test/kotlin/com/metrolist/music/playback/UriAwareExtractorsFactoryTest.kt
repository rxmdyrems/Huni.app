package com.metrolist.music.playback

import android.net.Uri
import androidx.media3.extractor.FlacExtractor
import androidx.media3.extractor.Mp3Extractor
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@androidx.media3.common.util.UnstableApi
class UriAwareExtractorsFactoryTest {
    private val factory = UriAwareExtractorsFactory()

    @Test
    fun `content uri receives default extractors including mp3 and flac`() {
        val extractors = factory.createExtractors(Uri.parse("content://media/audio/1"), emptyMap())
        assertTrue(extractors.any { it is Mp3Extractor })
        assertTrue(extractors.any { it is FlacExtractor })
    }

    @Test
    fun `https uri keeps exactly the fast three extractors`() {
        val extractors = factory.createExtractors(Uri.parse("https://music.youtube.com/audio"), emptyMap())
        assertEquals(3, extractors.size)
        assertTrue(extractors[0] is MatroskaExtractor)
        assertTrue(extractors[1] is FragmentedMp4Extractor)
        assertTrue(extractors[2] is Mp4Extractor)
    }
}

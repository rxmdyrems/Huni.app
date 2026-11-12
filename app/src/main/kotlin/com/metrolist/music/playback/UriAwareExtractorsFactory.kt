package com.metrolist.music.playback

import android.net.Uri
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor

/** Uses the fast YouTube container set for network streams and the full set for local files. */
@androidx.media3.common.util.UnstableApi
internal class UriAwareExtractorsFactory : ExtractorsFactory {
    private fun networkExtractors(): Array<Extractor> =
        arrayOf(MatroskaExtractor(), FragmentedMp4Extractor(), Mp4Extractor())
    private val localFactory = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)

    override fun createExtractors(): Array<Extractor> = networkExtractors()

    override fun createExtractors(uri: Uri, responseHeaders: Map<String, List<String>>): Array<Extractor> =
        if (uri.scheme in LOCAL_SCHEMES) localFactory.createExtractors(uri, responseHeaders) else networkExtractors()

    private companion object {
        val LOCAL_SCHEMES = setOf("content", "file", "android.resource")
    }
}

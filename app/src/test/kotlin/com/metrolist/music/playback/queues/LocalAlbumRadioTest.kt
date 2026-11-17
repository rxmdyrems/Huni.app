package com.metrolist.music.playback.queues

import com.metrolist.music.db.entities.AlbumEntity
import com.metrolist.music.db.entities.AlbumWithSongs
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.SongEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LocalAlbumRadioTest {
    @Test
    fun `initial album queue item retains the source video id`() = runBlocking {
        val videoId = "video-id-from-search-and-album"
        val album =
            AlbumWithSongs(
                album = AlbumEntity(id = "album-id", title = "Example album", songCount = 1, duration = 180),
                artists = emptyList(),
                songs =
                    listOf(
                        Song(
                            song = SongEntity(id = videoId, title = "Example track", duration = 180),
                            artists = emptyList(),
                        ),
                    ),
            )

        val status = LocalAlbumRadio(album).getInitialStatus()

        assertEquals(videoId, status.items.single().mediaId)
        assertEquals(0, status.mediaItemIndex)
    }
}

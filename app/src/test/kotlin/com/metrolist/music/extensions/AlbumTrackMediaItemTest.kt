package com.metrolist.music.extensions

import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AlbumTrackMediaItemTest {
    @Test
    fun `album queue track and search result preserve the same video id`() {
        val videoId = "video-id-from-search-and-album"
        val searchItem =
            SongItem(
                id = videoId,
                title = "Example track",
                artists = listOf(Artist(name = "Example artist", id = "artist-id")),
                thumbnail = "https://example.com/cover.jpg",
            ).toMediaItem()
        val albumQueueItem =
            Song(
                song = SongEntity(id = videoId, title = "Example track"),
                artists = emptyList(),
            ).toMediaItem()

        assertEquals(videoId, searchItem.mediaId)
        assertEquals(searchItem.mediaId, albumQueueItem.mediaId)
    }
}

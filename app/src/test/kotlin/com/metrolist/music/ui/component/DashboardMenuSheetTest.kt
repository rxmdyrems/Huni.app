package com.metrolist.music.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardMenuSheetTest {
    @Test fun menuContainsSettingsMusicAnimeAndMangaDestinationsInOrder() {
        assertEquals(
            listOf("settings", "home", "anime", "manga_dashboard"),
            dashboardMenuItems.map { it.route },
        )
        assertEquals(
            listOf(
                com.metrolist.music.R.string.settings,
                com.metrolist.music.R.string.music_dashboard,
                com.metrolist.music.R.string.anime_dashboard,
                com.metrolist.music.R.string.manga_dashboard,
            ),
            dashboardMenuItems.map { it.label },
        )
        assertTrue(dashboardMenuItems.all { it.icon != 0 })
    }
}

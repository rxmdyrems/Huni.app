package com.metrolist.music.ui

import com.metrolist.music.ui.screens.settings.NavigationConfig
import com.metrolist.music.ui.screens.settings.NavigationTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationConfigTest {
    @Test fun invalidConfigRestoresThreeDefaults() {
        assertEquals(3, NavigationConfig.parse("READER").size)
    }

    @Test fun serializationUsesFixedOrder() {
        assertEquals("HOME,LIBRARY,READER", NavigationConfig.serialize(setOf(NavigationTab.READER, NavigationTab.HOME, NavigationTab.LIBRARY)))
    }

    @Test fun lastTwoEnabledTabsCannotBeRemoved() {
        val tabs = setOf(NavigationTab.HOME, NavigationTab.SEARCH)
        assertEquals(tabs, NavigationConfig.toggle(tabs, NavigationTab.HOME, false))
        assertTrue(NavigationConfig.toggle(tabs, NavigationTab.LIBRARY, true).contains(NavigationTab.LIBRARY))
    }
    @Test fun secondaryRoutesAreNeverRedirected() {
        val visibleTabs = setOf(NavigationTab.HOME, NavigationTab.LIBRARY)
        assertTrue(!NavigationConfig.shouldRedirectHiddenTopLevelRoute("settings", visibleTabs))
        assertTrue(!NavigationConfig.shouldRedirectHiddenTopLevelRoute("settings/appearance", visibleTabs))
        assertTrue(!NavigationConfig.shouldRedirectHiddenTopLevelRoute("settings/about", visibleTabs))
        assertTrue(!NavigationConfig.shouldRedirectHiddenTopLevelRoute("reader/detail", visibleTabs))
    }
}

package com.metrolist.music.ui.screens.settings

import com.metrolist.music.ui.screens.Screens

/** Stable user-facing order for the configurable bottom navigation. */
object NavigationConfig {
    val orderedTabs = listOf(
        NavigationTab.HOME,
        NavigationTab.SEARCH,
        NavigationTab.LIBRARY,
        NavigationTab.READER,
    )

    const val defaultValue = "READER"

    /** Stored values are hidden tabs; an empty value keeps every ordered tab visible. */
    fun parse(value: String): Set<NavigationTab> {
        val hidden = value.split(',').mapNotNull { token ->
            runCatching { NavigationTab.valueOf(token.trim()) }.getOrNull()
        }.toSet()
        val visible = orderedTabs.filterNot { it in hidden }.toSet()
        return if (visible.size >= 2) visible else orderedTabs.takeLast(2).toSet()
    }

    fun serialize(tabs: Set<NavigationTab>): String =
        orderedTabs.filterNot { it in tabs }.joinToString(",")

    fun toggle(tabs: Set<NavigationTab>, tab: NavigationTab, checked: Boolean): Set<NavigationTab> {
        val next = tabs.toMutableSet()
        if (checked) next += tab else if (next.size > 2) next -= tab
        return next
    }

    fun screens(tabs: Set<NavigationTab>): List<Screens> = orderedTabs.filter { it in tabs }.map {
        when (it) {
            NavigationTab.HOME -> Screens.Home
            NavigationTab.SEARCH -> Screens.Search
            NavigationTab.LIBRARY -> Screens.Library
            NavigationTab.READER -> Screens.Reader
        }
    }

    /** Only an exact top-level tab route may be redirected when that tab is hidden. */
    fun shouldRedirectHiddenTopLevelRoute(currentRoute: String?, visibleTabs: Set<NavigationTab>): Boolean {
        if (currentRoute == null) return false
        return screens(orderedTabs.toSet()).any { it.route == currentRoute } &&
            screens(visibleTabs).none { it.route == currentRoute }
    }

    fun route(tab: NavigationTab): String = when (tab) {
        NavigationTab.HOME -> Screens.Home.route
        NavigationTab.SEARCH -> Screens.Search.route
        NavigationTab.LIBRARY -> Screens.Library.route
        NavigationTab.READER -> Screens.Reader.route
    }
}

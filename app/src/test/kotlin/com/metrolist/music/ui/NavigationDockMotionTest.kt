package com.metrolist.music.ui

import com.metrolist.music.ui.component.navigationDockMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationDockMotionTest {
    @Test fun collapsedDockShrinksAndFadesLabels() {
        val expanded = navigationDockMetrics(0f, compact = false, keepExpanded = false)
        val collapsed = navigationDockMetrics(1f, compact = false, keepExpanded = false)
        assertTrue(collapsed.heightDp < expanded.heightDp)
        assertTrue(collapsed.horizontalPaddingDp > expanded.horizontalPaddingDp)
        assertEquals(0f, collapsed.labelAlpha)
    }

    @Test fun keepExpandedIgnoresScrollFraction() {
        assertEquals(
            navigationDockMetrics(0f, compact = true, keepExpanded = false),
            navigationDockMetrics(1f, compact = true, keepExpanded = true),
        )
    }

    @Test fun invalidScrollValuesAreClamped() {
        assertEquals(1f, navigationDockMetrics(Float.NaN, false, false).labelAlpha, 0.001f)
        assertEquals(0f, navigationDockMetrics(Float.POSITIVE_INFINITY, false, false).labelAlpha, 0.001f)
    }
}

package com.metrolist.music.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesParserTest {
    @Test
    fun `parses version sections and presents clean emoji headings and footer`() {
        val notes = ReleaseNotesParser.parse(
            """
            # Huni Changelog
            > This decorative pre-version quote is ignored.

            ### 🐞 Found a bug?
            - 📝 Open an issue: https://github.com/rxmdyrems/Huni.app/issues/new/choose
            - 📱 In the app: **Settings → About** or Email&nbsp;instead.

            ---
            <sub>🤍 Updated by Remy Camiguel · @rxmdyrems · Free and open source (GPL v3)</sub>

            ## Unreleased

            ## v0.9.7 (2026-11-13)
            ### 🚀 Added
            - First addition
            - Second addition
            <p>### 🎨 Changed</p>
            <p>- A visual&nbsp;change</p>
            ---
            ## v0.9.6 (2026-11-10)
            ### 🛠️ Fixed
            - A previous fix
            """.trimIndent(),
        )

        assertEquals(listOf("Unreleased", "0.9.7", "0.9.6"), notes.entries.map { it.version })
        assertEquals(2, notes.preambleSections.single().bullets.size)
        assertEquals(2, notes.entries[1].sections[0].bullets.size)
        assertEquals("Added", notes.entries[1].sections[0].normalizedHeading)
        assertEquals("Changed", notes.entries[1].sections[1].normalizedHeading)
        assertEquals(1, notes.entries[2].sections.single().bullets.size)
        assertTrue(notes.preambleSections.flatMap { it.bullets }.none { it.contains("decorative pre-version quote") })

        val shownText = buildList {
            notes.preambleSections.forEach { section ->
                section.heading?.let { add(it) }
                addAll(section.bullets)
            }
            notes.entries.forEach { entry ->
                add(entry.version)
                entry.sections.forEach { section ->
                    section.heading?.let { add(it) }
                    addAll(section.bullets)
                }
            }
            notes.footer?.let(::add)
        }
        assertFalse(shownText.any { it.contains("<sub>", ignoreCase = true) })
        assertFalse(shownText.any { it.contains("<p>", ignoreCase = true) })
        assertFalse(shownText.any { it.contains("---") })
        assertFalse(shownText.any { it.contains("&nbsp;", ignoreCase = true) })
        assertTrue(shownText.any { it.contains("Email instead") })
        assertTrue(notes.footer?.contains("Updated by Remy Camiguel") == true)
    }
}

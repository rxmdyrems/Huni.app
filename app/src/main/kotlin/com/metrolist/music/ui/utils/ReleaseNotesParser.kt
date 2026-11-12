package com.metrolist.music.ui.utils

/** A Markdown section heading and its release-note bullets. */
data class ReleaseNoteSection(
    val heading: String?,
    /** Emoji-free heading text for matching standard categories such as Added, Changed, and Fixed. */
    val normalizedHeading: String?,
    val bullets: List<String>,
)

data class ReleaseNoteEntry(
    val version: String,
    val date: String,
    val sections: List<ReleaseNoteSection>,
)

data class ReleaseNotesDocument(
    /** Sections before the first version heading, such as a project-wide bug-report block. */
    val preambleSections: List<ReleaseNoteSection>,
    val entries: List<ReleaseNoteEntry>,
    val footer: String?,
)

/** Parses the subset of Markdown used by the project changelog and GitHub release notes. */
object ReleaseNotesParser {
    private val versionHeading = Regex("^##\\s+v?([^\\s(]+)(?:\\s*\\(([^)]+)\\))?")
    private val subTag = Regex("(?is)<sub\\b[^>]*>(.*?)</sub>")
    private val emojiPrefix = Regex("^[\\p{So}\\p{Sk}\\p{M}\\u200D\\uFE0E\\uFE0F\\u20E3\\s]+")
    private val htmlTag = Regex("<[^>]+>")
    private val htmlParagraphOpen = Regex("(?i)<p\\b[^>]*>")

    fun parse(markdown: String): ReleaseNotesDocument {
        val decoded = decodeEntities(markdown)
        val footer = subTag.find(decoded)?.groupValues?.get(1)?.let { cleanInline(it) }
        val body = subTag.replace(decoded, "")
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p\\s*>"), "\n")
            .replace(htmlParagraphOpen, "")

        val preamble = SectionAccumulator()
        val entries = mutableListOf<ReleaseNoteEntry>()
        var currentVersion: String? = null
        var currentDate = ""
        var currentSections = SectionAccumulator()
        var foundVersionHeading = false

        fun finishEntry() {
            val version = currentVersion ?: return
            entries += ReleaseNoteEntry(version, currentDate, currentSections.build())
            currentVersion = null
            currentDate = ""
            currentSections = SectionAccumulator()
        }

        body.lineSequence().forEach { rawLine ->
            val line = cleanInline(rawLine).trim()
            if (line.isEmpty() || line == "---") return@forEach

            val versionMatch = versionHeading.find(line)
            if (versionMatch != null) {
                finishEntry()
                foundVersionHeading = true
                currentVersion = versionMatch.groupValues[1]
                currentDate = versionMatch.groupValues.getOrNull(2).orEmpty()
                return@forEach
            }

            // Project notes and support information may precede the first version. Ignore
            // decorative blockquotes there while retaining explicit ### sections and bullets.
            if (line.startsWith(">")) return@forEach
            if (line.startsWith("## ") || line.startsWith("# ")) return@forEach

            val target = if (foundVersionHeading) currentSections else preamble
            when {
                line.startsWith("### ") -> target.startSection(line.removePrefix("### ").trim())
                line.startsWith("- ") || line.startsWith("* ") ->
                    target.addBullet(line.drop(2).trim())
            }
        }
        finishEntry()

        return ReleaseNotesDocument(
            preambleSections = preamble.build(),
            entries = entries,
            footer = footer?.takeIf { it.isNotBlank() },
        )
    }

    /** Removes presentation-only lines and markup for the Updater's plain-text fallback. */
    fun cleanDisplayText(text: String): String {
        val body = subTag.replace(decodeEntities(text), "")
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p\\s*>"), "\n")
            .replace(htmlParagraphOpen, "")
        return body.lineSequence()
            .map { cleanInline(it).trim() }
            .filter { it.isNotEmpty() && it != "---" && !it.startsWith("## ") && !it.startsWith("# ") && !it.startsWith(">") }
            .joinToString("\n")
    }

    private fun decodeEntities(text: String): String = text
        .replace("&nbsp;", " ", ignoreCase = true)
        .replace("&#160;", " ", ignoreCase = true)
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)

    private fun cleanInline(text: String): String = text
        .replace(Regex("\\[([^\\]]+)]\\(([^)]+)\\)")) { match ->
            "${match.groupValues[1]} (${match.groupValues[2]})"
        }
        .replace(htmlTag, "")
        .replace("**", "")
        .replace("__", "")
        .replace("`", "")
        .trim()

    private class SectionAccumulator {
        private val sections = mutableListOf<ReleaseNoteSection>()
        private var heading: String? = null
        private var normalizedHeading: String? = null
        private var bullets = mutableListOf<String>()

        fun startSection(value: String) {
            flush()
            heading = value
            val cleanHeading = value.replace(emojiPrefix, "").trim()
            normalizedHeading = when (cleanHeading.lowercase()) {
                "added" -> "Added"
                "changed" -> "Changed"
                "fixed" -> "Fixed"
                else -> cleanHeading
            }
        }

        fun addBullet(value: String) {
            bullets += value
        }

        fun build(): List<ReleaseNoteSection> {
            flush()
            return sections.toList()
        }

        private fun flush() {
            if (heading != null || bullets.isNotEmpty()) {
                sections += ReleaseNoteSection(heading, normalizedHeading, bullets.toList())
            }
            heading = null
            normalizedHeading = null
            bullets = mutableListOf()
        }
    }
}

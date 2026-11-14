package com.metrolist.music.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ExtensionConfig(
    val id: String,
    val name: String,
    val baseUrl: String,
    val lang: String,
    val popular: ExtensionEndpoint<MangaSelectors>,
    val search: ExtensionEndpoint<MangaSelectors>,
    val details: ExtensionEndpoint<DetailsSelectors>,
    val chapters: ExtensionEndpoint<ChapterSelectors>,
    val pages: ExtensionEndpoint<PageSelectors>,
)

@Serializable
data class ExtensionEndpoint<S>(
    val url: String,
    val selectors: S,
)

@Serializable
data class MangaSelectors(
    val title: String,
    val cover: String,
    val link: String,
)

@Serializable
data class DetailsSelectors(
    val title: String,
    val author: String,
    val cover: String,
)

@Serializable
data class ChapterSelectors(
    val title: String,
    val link: String,
)

@Serializable
data class PageSelectors(
    val image: String,
)

data class ExtensionManga(
    val title: String,
    val coverUrl: String?,
    val link: String,
)

data class ExtensionMangaDetails(
    val title: String,
    val author: String,
    val coverUrl: String?,
)

data class ExtensionChapter(
    val title: String,
    val link: String,
)

sealed interface ExtensionResult<out T> {
    data class Success<T>(val data: T) : ExtensionResult<T>
    data class Error(val message: String) : ExtensionResult<Nothing>
}

/** Reject malformed or unsafe identifiers before they become private-storage paths. */
fun ExtensionConfig.validationError(): String? {
    if (!id.matches(Regex("[A-Za-z0-9._-]{1,80}"))) return "Extension id must contain only letters, numbers, dots, underscores, or hyphens."
    if (name.isBlank()) return "Extension name is required."
    if (baseUrl.isBlank() || !baseUrl.startsWith("https://") && !baseUrl.startsWith("http://")) return "Extension baseUrl must be an HTTP or HTTPS URL."
    val selectors = listOf(
        popular.selectors.title, popular.selectors.cover, popular.selectors.link,
        search.selectors.title, search.selectors.cover, search.selectors.link,
        details.selectors.title, details.selectors.author, details.selectors.cover,
        chapters.selectors.title, chapters.selectors.link,
        pages.selectors.image,
    )
    if (selectors.any(String::isBlank)) return "Required CSS selectors cannot be blank."
    val endpoints = listOf(popular.url, search.url, details.url, chapters.url, pages.url)
    if (endpoints.any(String::isBlank)) return "All endpoint URLs are required."
    return null
}

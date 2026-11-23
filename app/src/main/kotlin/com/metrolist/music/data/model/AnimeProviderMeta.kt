package com.metrolist.music.data.model

import kotlinx.serialization.Serializable

/** A provider entry advertised by a user-configured repository manifest. */
@Serializable
data class AnimeProviderMeta(
    val id: String,
    val name: String,
    val version: String,
    val type: String = "anime",
    val lang: String = "en",
    val file: String,
    val logo: String? = null,
    val nsfw: Boolean = false,
    val repositoryUrl: String,
    val description: String = "",
)

@Serializable
data class InstalledAnimeProvider(
    val meta: AnimeProviderMeta,
    val enabled: Boolean = true,
    val installedAt: Long,
)

@Serializable
data class AnimeProviderRepoCatalog(
    val url: String,
    val name: String,
    val description: String = "",
    val providers: List<AnimeProviderMeta>,
    val refreshedAt: Long,
)

@Serializable
data class AnimeMediaItem(
    val id: String,
    val title: String,
    val url: String,
    val cover: String = "",
    val type: String = "anime",
    val sourceId: String,
    val description: String = "",
)

@Serializable
data class AnimeEpisode(
    val id: String,
    val title: String,
    val number: Double? = null,
    val url: String,
    val sourceId: String = "",
)

@Serializable
data class AnimeVideoSource(
    val url: String,
    val quality: String = "",
    val container: String = "hls",
    val headers: Map<String, String> = emptyMap(),
)

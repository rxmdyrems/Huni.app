package com.metrolist.music.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ExtensionMeta(
    val name: String,
    val pkg: String,
    /** Absolute download URL, or a repo-relative APK filename resolved to /apk/{filename}. */
    val apk: String,
    val lang: String,
    val code: Long,
    val version: String,
    val nsfw: Boolean,
    val sources: List<ExtensionSourceMeta> = emptyList(),
    val iconUrl: String = "",
)

@Serializable
data class ExtensionSourceMeta(
    val name: String,
    val lang: String,
    val id: String,
    val baseUrl: String,
)

@Serializable
data class RepoEntry(
    val url: String,
    val label: String,
)

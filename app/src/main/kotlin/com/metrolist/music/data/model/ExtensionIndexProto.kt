package com.metrolist.music.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** Subset of Mihon's compressed ExtensionStore protobuf index used by the reader catalog. */
@Serializable
data class ExtensionIndexProto(
    @ProtoNumber(101) val extensionList: ExtensionListProto? = null,
)

@Serializable
data class ExtensionListProto(
    @ProtoNumber(1) val extensions: List<ExtensionEntryProto> = emptyList(),
)

@Serializable
data class ExtensionEntryProto(
    @ProtoNumber(1) val name: String = "",
    @ProtoNumber(2) val packageName: String = "",
    @ProtoNumber(3) val resources: ExtensionResourcesProto = ExtensionResourcesProto(),
    @ProtoNumber(4) val extensionLib: String = "",
    @ProtoNumber(5) val versionCode: Long = 0,
    @ProtoNumber(6) val versionName: String = "",
    @ProtoNumber(7) val contentWarning: Int = 1,
    @ProtoNumber(8) val sources: List<ExtensionSourceProto> = emptyList(),
)

@Serializable
data class ExtensionResourcesProto(
    @ProtoNumber(1) val apkUrl: String = "",
    @ProtoNumber(2) val iconUrl: String = "",
)

@Serializable
data class ExtensionSourceProto(
    @ProtoNumber(1) val id: Long = 0,
    @ProtoNumber(2) val name: String = "",
    @ProtoNumber(3) val language: String = "",
    @ProtoNumber(4) val homeUrl: String = "",
    @ProtoNumber(5) val mirrorUrls: List<String> = emptyList(),
    @ProtoNumber(7) val message: String? = null,
)

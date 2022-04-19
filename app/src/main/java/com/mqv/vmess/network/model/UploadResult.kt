package com.mqv.vmess.network.model

import com.google.gson.annotations.SerializedName

data class UploadResult(
    val url: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    @SerializedName("mime_type") val mimeType: String,
    @SerializedName("file_name") val fileName: String?,
    val size: Long?,
    val type: Type
)

enum class Type {
    @SerializedName("image")
    IMAGE,
    @SerializedName("video")
    VIDEO,
    @SerializedName("file")
    FILE
}
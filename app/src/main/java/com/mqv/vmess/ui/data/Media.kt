package com.mqv.vmess.ui.data

import android.net.Uri

data class Media(
    val id: Long,
    val uri: Uri,
    val path: String,
    val mimeType: String,
    val bucketId: String?,
    val duration: Long,
    val size: Long,
    val date: Long,
    val isVideo: Boolean,
    var isSelected: Boolean
) {
    companion object {
        const val ALL_BUCKET_ID = "com.mqv.vmess.ALL_MEDIA"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Media

        if (id != other.id) return false
        if (uri != other.uri) return false
        if (path != other.path) return false
        if (mimeType != other.mimeType) return false
        if (bucketId != other.bucketId) return false
        if (duration != other.duration) return false
        if (date != other.date) return false
        if (isVideo != other.isVideo) return false
        if (isSelected != other.isSelected) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + uri.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + (bucketId?.hashCode() ?: 0)
        result = 31 * result + duration.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + isVideo.hashCode()
        result = 31 * result + isSelected.hashCode()
        return result
    }
}
package com.mqv.vmess.util

import android.provider.MediaStore
import com.mqv.vmess.network.model.type.MessageMediaUploadType

object MediaUtil {
    @JvmStatic
    fun mapTypeToMessageUploadType(contentType: String?): MessageMediaUploadType {
        return when {
            isImageType(contentType) -> MessageMediaUploadType.PHOTO
            isVideoType(contentType) -> MessageMediaUploadType.VIDEO
            isAudioType(contentType) -> MessageMediaUploadType.AUDIO
            else -> MessageMediaUploadType.FILE
        }
    }

    @JvmStatic
    fun isAudioType(contentType: String?): Boolean {
        return if (contentType == null) {
            false
        } else contentType.startsWith("audio/") || contentType == MediaStore.Audio.Media.CONTENT_TYPE
    }

    @JvmStatic
    fun isVideoType(contentType: String?): Boolean {
        return if (contentType == null) {
            false
        } else contentType.startsWith("video/") || contentType == MediaStore.Video.Media.CONTENT_TYPE
    }

    @JvmStatic
    fun isImageType(contentType: String?): Boolean {
        return if (contentType == null) {
            false
        } else contentType.startsWith("image/") && contentType != "image/svg+xml" || contentType == MediaStore.Images.Media.CONTENT_TYPE
    }

    fun checkMimeType(mimeType: String): String {
        return when(mimeType) {
            "image/jpg" -> "image/jpeg"
            else -> mimeType
        }
    }
}
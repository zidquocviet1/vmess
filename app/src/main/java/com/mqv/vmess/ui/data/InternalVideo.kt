package com.mqv.vmess.ui.data

import android.graphics.Bitmap
import android.net.Uri

data class InternalVideo(
    val id: Long,
    val path: String,
    val duration: Long,
    val mimeType: String,
    val bucketId: String,
    val dateTaken: Long,
    val size: Long,
    val contentUri: Uri,
    val thumbnail: Bitmap
)
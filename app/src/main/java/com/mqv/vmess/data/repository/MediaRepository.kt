package com.mqv.vmess.data.repository

import android.content.Context
import com.mqv.vmess.ui.data.Media
import java.util.function.Consumer

interface MediaRepository {
    fun getMediaInBucket(context: Context, bucketId: String, callback: Consumer<List<Media>>)
}
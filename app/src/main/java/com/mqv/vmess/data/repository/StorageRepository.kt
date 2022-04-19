package com.mqv.vmess.data.repository

import com.mqv.vmess.network.ApiResponse
import com.mqv.vmess.network.model.ForwardMessagePayload
import com.mqv.vmess.network.model.UploadResult
import io.reactivex.rxjava3.core.Observable
import java.io.File

interface StorageRepository {
    fun uploadMessagePhoto(
        conversationId: String,
        messageId: String,
        image: File
    ): Observable<ApiResponse<UploadResult>>

    fun uploadMessageVideo(
        conversationId: String,
        messageId: String,
        video: File
    ): Observable<ApiResponse<UploadResult>>

    // Copy resources of message from that conversation to another one
    fun requestCopyResource(
        messageId: String,
        payload: ForwardMessagePayload
    ): Observable<ApiResponse<ForwardMessagePayload>>
}
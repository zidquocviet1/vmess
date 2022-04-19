package com.mqv.vmess.network.service

import com.mqv.vmess.network.ApiResponse
import com.mqv.vmess.network.model.ForwardMessagePayload
import com.mqv.vmess.network.model.UploadResult
import com.mqv.vmess.util.Const
import io.reactivex.rxjava3.core.Observable
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface StorageService {
    @POST("/storage/upload/message-photo/{conversation-id}/{message-id}")
    @Multipart
    fun uploadMessagePhoto(
        @Header(Const.AUTHORIZATION) token: String,
        @Path(value = "conversation-id") conversationId: String,
        @Path(value = "message-id") messageId: String,
        @Part file: MultipartBody.Part
    ): Observable<ApiResponse<UploadResult>>

    @POST("/storage/upload/message-video/{conversation-id}/{message-id}")
    @Multipart
    fun uploadMessageVideo(
        @Header(Const.AUTHORIZATION) token: String,
        @Path(value = "conversation-id") conversationId: String,
        @Path(value = "message-id") messageId: String,
        @Part video: MultipartBody.Part
    ): Observable<ApiResponse<UploadResult>>


    @PUT("/storage/copy")
    fun requestCopyResource(
        @Header(Const.AUTHORIZATION) token: String,
        @Body payload: ForwardMessagePayload,
        @Query(value = "messageId") messageId: String
    ): Observable<ApiResponse<ForwardMessagePayload>>

    @GET("/storage/download")
    @Streaming
    fun download(
        @Header(Const.AUTHORIZATION) token: String,
        @Query("full-path") url: String,
        @Query("video") isVideo: Boolean
    ): Call<ResponseBody>
}
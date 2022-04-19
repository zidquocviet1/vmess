package com.mqv.vmess.data.repository.impl

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.mqv.vmess.data.repository.StorageRepository
import com.mqv.vmess.network.ApiResponse
import com.mqv.vmess.network.model.ForwardMessagePayload
import com.mqv.vmess.network.model.UploadResult
import com.mqv.vmess.network.service.StorageService
import com.mqv.vmess.reactive.ReactiveExtension.authorizeToken
import io.reactivex.rxjava3.core.Observable
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class StorageRepositoryImpl @Inject constructor(
    private val storageService: StorageService
) : StorageRepository {
    private val mUser: FirebaseUser = FirebaseAuth.getInstance().currentUser!!

    override fun uploadMessagePhoto(
        conversationId: String,
        messageId: String,
        image: File
    ): Observable<ApiResponse<UploadResult>> {
        val body = image.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("image", image.name, body)

        return mUser.authorizeToken()
            .flatMapObservable { token ->
                storageService.uploadMessagePhoto(
                    token,
                    conversationId,
                    messageId,
                    part
                )
            }
    }

    override fun uploadMessageVideo(
        conversationId: String,
        messageId: String,
        video: File
    ): Observable<ApiResponse<UploadResult>> {
        val body = video.asRequestBody("video/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("video", video.name, body)

        return mUser.authorizeToken()
            .flatMapObservable { token ->
                storageService.uploadMessageVideo(
                    token,
                    conversationId,
                    messageId,
                    part
                )
            }
    }

    override fun requestCopyResource(
        messageId: String,
        payload: ForwardMessagePayload
    ): Observable<ApiResponse<ForwardMessagePayload>> =
        mUser.authorizeToken()
            .flatMapObservable { token ->
                storageService.requestCopyResource(token, payload, messageId)
            }
}
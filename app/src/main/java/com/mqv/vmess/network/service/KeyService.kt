package com.mqv.vmess.network.service

import com.mqv.vmess.network.ApiResponse
import com.mqv.vmess.network.model.PreKeyResponse
import com.mqv.vmess.network.model.PreKeyStateEntity
import com.mqv.vmess.network.model.SignedPreKeyEntity
import com.mqv.vmess.util.Const
import io.reactivex.rxjava3.core.Observable
import retrofit2.http.*

interface KeyService {
    @POST(value = "keys")
    fun setKeys(
        @Header(Const.AUTHORIZATION) token: String,
        @Body state: PreKeyStateEntity,
        @Query(value = "registrationId") registrationId: Int
    ): Observable<Unit>

    @GET(value = "keys/{userId}/{deviceId}")
    fun getPreKeys(
        @Header(Const.AUTHORIZATION) token: String,
        @Path(value = "userId") userId: String,
        @Path(value = "deviceId") deviceId: Int
    ): Observable<ApiResponse<PreKeyResponse>>

    @PUT(value = "keys/signed")
    fun setSignedPreKey(
        @Header(Const.AUTHORIZATION) token: String,
        @Body signedPreKey: SignedPreKeyEntity
    ): Observable<Unit>

    @GET(value = "keys/signed")
    fun getSignedPreKey(
        @Header(Const.AUTHORIZATION) token: String,
    ): Observable<ApiResponse<SignedPreKeyEntity>>
}
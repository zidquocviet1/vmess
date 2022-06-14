package com.mqv.vmess.data.repository

import com.mqv.vmess.network.ApiResponse
import com.mqv.vmess.network.model.PreKeyResponse
import com.mqv.vmess.network.model.PreKeyStateEntity
import com.mqv.vmess.network.model.SignedPreKeyEntity
import io.reactivex.rxjava3.core.Observable

interface KeyRepository {
    fun setKeys(state: PreKeyStateEntity, registrationId: Int): Observable<Unit>

    fun getPreKey(userId: String, deviceId: Int): Observable<ApiResponse<PreKeyResponse>>

    fun setSignedPreKey(signedPreKey: SignedPreKeyEntity): Observable<Unit>

    fun getSignedPreKey(): Observable<ApiResponse<SignedPreKeyEntity>>
}
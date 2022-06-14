package com.mqv.vmess.data.repository.impl

import com.google.firebase.auth.FirebaseAuth
import com.mqv.vmess.data.repository.KeyRepository
import com.mqv.vmess.network.ApiResponse
import com.mqv.vmess.network.model.PreKeyResponse
import com.mqv.vmess.network.model.PreKeyStateEntity
import com.mqv.vmess.network.model.SignedPreKeyEntity
import com.mqv.vmess.network.service.KeyService
import com.mqv.vmess.reactive.ReactiveExtension.authorizedAndGet
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

class KeyRepositoryImpl @Inject constructor(private val keyService: KeyService) : KeyRepository {
    private val mUser = FirebaseAuth.getInstance().currentUser!!

    override fun setKeys(state: PreKeyStateEntity, registrationId: Int): Observable<Unit> =
        mUser.authorizedAndGet { token -> keyService.setKeys(token, state, registrationId) }

    override fun getPreKey(userId: String, deviceId: Int): Observable<ApiResponse<PreKeyResponse>> =
        mUser.authorizedAndGet { token -> keyService.getPreKeys(token, userId, deviceId) }

    override fun setSignedPreKey(signedPreKey: SignedPreKeyEntity): Observable<Unit> =
        mUser.authorizedAndGet { token -> keyService.setSignedPreKey(token, signedPreKey) }

    override fun getSignedPreKey(): Observable<ApiResponse<SignedPreKeyEntity>> =
        mUser.authorizedAndGet { token -> keyService.getSignedPreKey(token) }
}
package com.mqv.vmess.data.repository.impl

import com.google.firebase.auth.FirebaseAuth
import com.mqv.vmess.data.repository.RtcRepository
import com.mqv.vmess.network.service.RtcService
import com.mqv.vmess.reactive.ReactiveExtension.authorizeToken
import com.mqv.vmess.webrtc.WebRtcCandidate
import com.mqv.vmess.webrtc.WebRtcDescription
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

class RtcRepositoryImpl @Inject constructor(
    val service: RtcService
) : RtcRepository {
    private val mUser = FirebaseAuth.getInstance().currentUser!!

    override fun makeCall(
        uid: String,
        description: String,
        isVideoCall: Boolean
    ): Observable<Boolean> {
        return mUser.authorizeToken()
            .flatMapObservable {
                service.makeCall(
                    it,
                    WebRtcDescription(uid, description),
                    isVideoCall
                )
            }
    }

    override fun stopCall(uid: String): Observable<Boolean> {
        return mUser.authorizeToken()
            .flatMapObservable { service.stopCall(it, uid) }
    }

    override fun denyCall(uid: String): Observable<Boolean> {
        return mUser.authorizeToken().flatMapObservable { service.denyCall(it, uid) }
    }

    override fun responseCall(uid: String, description: String): Observable<Boolean> {
        return mUser.authorizeToken()
            .flatMapObservable { service.responseCall(it, WebRtcDescription(uid, description)) }
    }

    override fun addIceCandidate(uid: String, candidates: WebRtcCandidate): Observable<Boolean> {
        return mUser.authorizeToken()
            .flatMapObservable { service.addIceCandidate(it, candidates, uid) }
    }

    override fun notifyHandled(uid: String): Observable<Boolean> {
        return mUser.authorizeToken()
            .flatMapObservable { service.notifyHandled(it, uid) }
    }

    override fun notifyBusy(uid: String): Observable<Boolean> {
        return mUser.authorizeToken().flatMapObservable { service.notifyBusy(it, uid) }
    }
}
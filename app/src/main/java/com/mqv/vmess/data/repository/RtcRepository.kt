package com.mqv.vmess.data.repository

import com.mqv.vmess.webrtc.WebRtcCandidate
import io.reactivex.rxjava3.core.Observable

interface RtcRepository {
    fun makeCall(uid: String, description: String, isVideoCall: Boolean): Observable<Boolean>
    fun stopCall(uid: String): Observable<Boolean>
    fun denyCall(uid: String): Observable<Boolean>
    fun responseCall(uid: String, description: String): Observable<Boolean>
    fun addIceCandidate(uid: String, candidates: WebRtcCandidate): Observable<Boolean>
    fun notifyHandled(uid: String): Observable<Boolean>
    fun notifyBusy(uid: String): Observable<Boolean>
}
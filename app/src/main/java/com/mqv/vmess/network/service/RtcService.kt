package com.mqv.vmess.network.service

import com.mqv.vmess.util.Const
import com.mqv.vmess.webrtc.WebRtcCandidate
import com.mqv.vmess.webrtc.WebRtcDescription
import io.reactivex.rxjava3.core.Observable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface RtcService {
    @POST(value = "rtc/make-call")
    fun makeCall(
        @Header(Const.AUTHORIZATION) token: String,
        @Body description: WebRtcDescription,
        @Query(value = "is-video-call") isVideoCall: Boolean
    ): Observable<Boolean>

    @POST(value = "rtc/response-call")
    fun responseCall(
        @Header(Const.AUTHORIZATION) token: String,
        @Body description: WebRtcDescription
    ): Observable<Boolean>

    @POST(value = "rtc/ice-candidate")
    fun addIceCandidate(
        @Header(Const.AUTHORIZATION) token: String,
        @Body candidates: WebRtcCandidate,
        @Query(value = "uid") uid: String
    ): Observable<Boolean>

    @POST(value = "rtc/stop-call")
    fun stopCall(
        @Header(Const.AUTHORIZATION) token: String,
        @Query(value = "uid") uid: String
    ): Observable<Boolean>

    @POST(value = "rtc/deny-call")
    fun denyCall(
        @Header(Const.AUTHORIZATION) token: String,
        @Query(value = "uid") uid: String
    ): Observable<Boolean>

    @POST(value = "rtc/notify-received")
    fun notifyHandled(
        @Header(Const.AUTHORIZATION) token: String,
        @Query(value = "uid") uid: String
    ): Observable<Boolean>

    @POST(value = "rtc/notify-busy")
    fun notifyBusy(
        @Header(Const.AUTHORIZATION) token: String,
        @Query(value = "uid") uid: String
    ): Observable<Boolean>
}
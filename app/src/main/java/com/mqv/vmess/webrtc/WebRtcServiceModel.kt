package com.mqv.vmess.webrtc

import com.mqv.vmess.data.repository.RtcRepository
import io.reactivex.rxjava3.disposables.CompositeDisposable

data class WebRtcServiceModel(
    val rtcRepository: RtcRepository,
    val compositeDisposable: CompositeDisposable,
    val recipient: String,
    val isVideoEnabled: Boolean,
    val isIncomingCall: Boolean
)
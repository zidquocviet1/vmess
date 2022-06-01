package com.mqv.vmess.webrtc

data class WebRtcCandidate(
    val id: String,
    val index: Int,
    val sdp: String
)
package com.mqv.vmess.webrtc

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

data class WebRtcMessage(
    val sdp: SessionDescription?,
    val candidate: IceCandidate?
) {
}
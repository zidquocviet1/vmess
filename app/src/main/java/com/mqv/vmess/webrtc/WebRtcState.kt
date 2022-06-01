package com.mqv.vmess.webrtc

enum class WebRtcState {
    START_CALL,
    CONNECTING,
    PRE_CONNECTED,
    CONNECTED,
    DISCONNECTING,
    DISCONNECTED,
    FAILED,
    UNAUTHORIZED
}

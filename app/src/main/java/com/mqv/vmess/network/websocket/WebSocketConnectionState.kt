package com.mqv.vmess.network.websocket

enum class WebSocketConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTING,
    AUTHENTICATION_FAILED,
    FAILED;

    fun isFailure() : Boolean = this == AUTHENTICATION_FAILED || this == FAILED
}
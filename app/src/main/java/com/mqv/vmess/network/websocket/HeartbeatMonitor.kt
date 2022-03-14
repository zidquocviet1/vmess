package com.mqv.vmess.network.websocket

/*
* Callback to provide websocket heartbeat information
* */
interface HeartbeatMonitor {
    fun onKeepAliveResponse(sentTime: Long)
    fun onMessageError(request: WebSocketRequestMessage)
    fun onUserPresence(request: WebSocketRequestMessage)
}
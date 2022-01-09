package com.mqv.realtimechatapplication.network.websocket

/*
* Callback to provide websocket heartbeat information
* */
interface HeartbeatMonitor {
    fun onKeepAliveResponse(sentTime: Long)
    fun onMessageError(request: WebSocketRequestMessage)
}
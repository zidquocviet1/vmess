package com.mqv.vmess.network.websocket

interface WebSocketFactory {
    fun createWebSocket(): WebSocketConnection
    fun createPresenceWebSocket(): WebSocketConnection
}
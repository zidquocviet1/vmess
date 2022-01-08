package com.mqv.realtimechatapplication.network.websocket

interface WebSocketFactory {
    fun createWebSocket(): WebSocketConnection
}
package com.mqv.vmess.network.websocket

import com.mqv.vmess.network.model.Chat

data class WebSocketResponseMessage(
    val id: Long,
    val status: Int,
    val message: String,
    val body: Chat
)

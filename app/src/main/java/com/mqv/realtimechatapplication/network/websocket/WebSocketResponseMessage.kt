package com.mqv.realtimechatapplication.network.websocket

import com.mqv.realtimechatapplication.network.model.Chat

data class WebSocketResponseMessage(
    val id: Long,
    val status: Int,
    val message: String,
    val body: Chat
)

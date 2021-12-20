package com.mqv.realtimechatapplication.network.websocket

import com.mqv.realtimechatapplication.network.model.Chat

data class WebSocketRequestMessage(val id: Long, val body: Chat)

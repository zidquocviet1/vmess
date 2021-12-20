package com.mqv.realtimechatapplication.network.websocket

import com.mqv.realtimechatapplication.network.model.Chat

data class WebSocketResponse(val status: Int, val body: Chat)

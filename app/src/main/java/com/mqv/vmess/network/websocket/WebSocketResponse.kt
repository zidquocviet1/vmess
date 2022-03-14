package com.mqv.vmess.network.websocket

import com.mqv.vmess.network.model.Chat

data class WebSocketResponse(val status: Int, val body: Chat)

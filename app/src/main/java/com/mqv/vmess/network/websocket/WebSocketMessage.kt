package com.mqv.vmess.network.websocket

import com.google.gson.annotations.SerializedName

data class WebSocketMessage(
    val type: Type,
    val request: WebSocketRequestMessage? = null,
    val response: WebSocketResponseMessage? = null
) {
    enum class Type {
        @SerializedName("unknown")
        UNKNOWN,
        @SerializedName("request")
        REQUEST,
        @SerializedName("response")
        RESPONSE
    }
}

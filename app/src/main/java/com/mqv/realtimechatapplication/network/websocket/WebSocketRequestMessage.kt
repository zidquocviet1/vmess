package com.mqv.realtimechatapplication.network.websocket

import com.google.gson.annotations.SerializedName
import com.mqv.realtimechatapplication.network.model.Chat

data class WebSocketRequestMessage(val id: Long, val status: Status, val body: Chat, val from: String) {
    enum class Status {
        @SerializedName("incoming")
        INCOMING_MESSAGE,
        @SerializedName("accepted")
        ACCEPTED_MESSAGE,
        @SerializedName("seen")
        SEEN_MESSAGE,
        @SerializedName("unknown")
        UNKNOWN
    }
}

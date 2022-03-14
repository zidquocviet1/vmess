package com.mqv.vmess.network.websocket

import com.google.gson.annotations.SerializedName
import com.mqv.vmess.network.model.Chat

data class WebSocketRequestMessage(val id: Long, val status: Status, val body: Chat, val from: String) {
    enum class Status {
        @SerializedName("incoming")
        INCOMING_MESSAGE,
        @SerializedName("accepted")
        ACCEPTED_MESSAGE,
        @SerializedName("seen")
        SEEN_MESSAGE,
        @SerializedName("ping")
        PING,
        @SerializedName("unknown")
        UNKNOWN
    }
}

package com.mqv.vmess.ui.adapter.payload

data class ConversationPresencePayload(
    val type: ConversationPresenceType,
    val timestamp: Long
)

enum class ConversationPresenceType {
    ONLINE,
    OFFLINE
}
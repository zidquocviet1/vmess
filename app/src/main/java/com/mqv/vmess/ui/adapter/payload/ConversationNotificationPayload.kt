package com.mqv.vmess.ui.adapter.payload

data class ConversationNotificationPayload(
    val type: ConversationNotificationType,
    val timestamp: Long
)

enum class ConversationNotificationType {
    ON,
    OFF
}
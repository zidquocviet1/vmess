package com.mqv.vmess.notification

import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.model.Conversation
import com.mqv.vmess.network.model.User

data class MessageNotificationMetadata(
    val sender: User,
    val conversation: Conversation,
    val message: Chat
)

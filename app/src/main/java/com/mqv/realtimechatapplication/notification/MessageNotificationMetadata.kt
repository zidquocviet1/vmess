package com.mqv.realtimechatapplication.notification

import com.mqv.realtimechatapplication.network.model.Chat
import com.mqv.realtimechatapplication.network.model.Conversation
import com.mqv.realtimechatapplication.network.model.User

data class MessageNotificationMetadata(
    val sender: User,
    val conversation: Conversation,
    val message: Chat
)

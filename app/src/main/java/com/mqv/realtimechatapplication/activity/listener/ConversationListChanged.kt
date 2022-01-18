package com.mqv.realtimechatapplication.activity.listener

import com.mqv.realtimechatapplication.network.model.Conversation

interface ConversationListChanged {
    fun removeConversationUI(conversation: Conversation)
    fun bindPresenceConversation(onlineUsersId: List<String>)
}
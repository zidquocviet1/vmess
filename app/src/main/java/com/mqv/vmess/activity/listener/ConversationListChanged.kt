package com.mqv.vmess.activity.listener

import com.mqv.vmess.network.model.Conversation

interface ConversationListChanged {
    fun removeConversationUI(conversation: Conversation)
    fun bindPresenceConversation(onlineUsersId: List<String>)
}
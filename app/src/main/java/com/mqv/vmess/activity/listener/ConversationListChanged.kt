package com.mqv.vmess.activity.listener

import com.mqv.vmess.network.model.Conversation

interface ConversationListChanged {
    fun addLoadingUI(onAdded: Runnable)
    fun removeLoadingUI()
    fun onMoreConversation(conversation: List<Conversation>)
    fun removeConversationUI(conversation: Conversation)
    fun bindPresenceConversation(onlineUsersId: List<String>)
}
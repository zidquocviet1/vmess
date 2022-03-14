package com.mqv.vmess.ui.data

import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.model.User
import com.mqv.vmess.network.model.type.ConversationType

interface BindableConversation<T> {
    fun bind(item: T)
    fun bindWelcomeMessage(welcomeMessage: Chat, nextItem: Chat?)
    fun bindMessageStatus(message: Chat, participants: List<User>, type: ConversationType)
}
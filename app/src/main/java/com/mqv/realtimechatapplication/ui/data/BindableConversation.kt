package com.mqv.realtimechatapplication.ui.data

import com.mqv.realtimechatapplication.network.model.Chat
import com.mqv.realtimechatapplication.network.model.User
import com.mqv.realtimechatapplication.network.model.type.ConversationType

interface BindableConversation<T> {
    fun bind(item: T)
    fun bindWelcomeMessage(welcomeMessage: Chat, nextItem: Chat?)
    fun bindMessageStatus(message: Chat, participants: List<User>, type: ConversationType)
}
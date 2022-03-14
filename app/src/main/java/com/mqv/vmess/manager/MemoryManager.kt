package com.mqv.vmess.manager

import com.mqv.vmess.network.model.Conversation

class MemoryManager {
    private var _conversationListCache: List<Conversation>? = null

    val conversationListCache get() = _conversationListCache

    fun setConversationList(data: List<Conversation>) {
        _conversationListCache = data
    }
}
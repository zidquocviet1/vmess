package com.mqv.realtimechatapplication.ui.data

import com.mqv.realtimechatapplication.network.model.type.ConversationType

data class ConversationMetadata(
    val conversationName: String,
    val conversationThumbnail: List<String?>,
    val conversationCreatedBy: String,
    val type: ConversationType,
    val otherUid: String
)

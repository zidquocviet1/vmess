package com.mqv.vmess.ui.data

import com.mqv.vmess.network.model.User
import com.mqv.vmess.network.model.type.ConversationType

data class ConversationMetadata(
    val conversationName: String,
    val conversationThumbnail: List<String?>,
    val conversationCreatedBy: String,
    val conversationParticipants: List<User>,
    val type: ConversationType,
    val otherUid: String
)

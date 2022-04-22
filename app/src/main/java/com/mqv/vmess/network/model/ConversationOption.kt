package com.mqv.vmess.network.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

data class ConversationOption(
    val id: Long,
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("mute_until") val muteUntil: Long,
    val option: Option,
    val timestamp: LocalDateTime
)

enum class Option {
    @SerializedName("ignore")
    IGNORE,
    @SerializedName("mute_notification")
    MUTE_NOTIFICATION,
    @SerializedName("reject_add")
    REJECT_ADD
}
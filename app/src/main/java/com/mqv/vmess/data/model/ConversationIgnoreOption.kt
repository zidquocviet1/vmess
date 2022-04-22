package com.mqv.vmess.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "conversation_ignore_option")
data class ConversationIgnoreOption(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    val timestamp: LocalDateTime
)
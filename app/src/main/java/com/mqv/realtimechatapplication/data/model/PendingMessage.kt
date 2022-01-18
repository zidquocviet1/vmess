package com.mqv.realtimechatapplication.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey
import com.mqv.realtimechatapplication.network.model.Chat

/*
* Store the outgoing messages because it may not be send.
* If the user has no internet connection, it will automatically enqueue to WorkManager and sent it back when reconnect.
* If the WorkManager enqueued and not send successfully, then we need to send it back when the user open the app.
* When the outgoing message was sent successfully, then remove it out from database.
* */
@Entity(
    tableName = "pending_message",
    foreignKeys = [ForeignKey(
        entity = Chat::class,
        parentColumns = ["chat_id"],
        childColumns = ["id"],
        onDelete = CASCADE
    )]
)
data class PendingMessage(
    @PrimaryKey
    @ColumnInfo(index = true)
    val id: String,
    val timestamp: Long,
    val workId: String
)

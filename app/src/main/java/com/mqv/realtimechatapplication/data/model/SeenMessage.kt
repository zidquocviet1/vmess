package com.mqv.realtimechatapplication.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey
import com.mqv.realtimechatapplication.network.model.Chat

@Entity(
    tableName = "seen_message",
    foreignKeys = [
        ForeignKey(
            entity = Chat::class,
            parentColumns = ["chat_id"],
            childColumns = ["id"],
            onDelete = CASCADE
        )
    ]
)
data class SeenMessage(
    @PrimaryKey
    @ColumnInfo(index = true)
    val id: String,
    val timestamp: Long
)

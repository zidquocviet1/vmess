package com.mqv.vmess.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_color")
data class ConversationColor @JvmOverloads constructor(
    @PrimaryKey
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "chat_color")
    var chatColor: String = DEFAULT_CHAT_COLOR,
    @ColumnInfo(name = "wallpaper_color")
    var wallpaperColor: String = DEFAULT_WALL_PAPER_COLOR
) {
    companion object {
        const val DEFAULT_CHAT_COLOR = "#FF6200EE"
        const val DEFAULT_WALL_PAPER_COLOR = "#FFFFFFFF"
    }
}

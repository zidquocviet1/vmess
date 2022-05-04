package com.mqv.vmess.data.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mqv.vmess.network.model.ConversationOption
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

@Entity(tableName = "conversation_notification_option")
@Parcelize
data class ConversationNotificationOption(
    @PrimaryKey(autoGenerate = true) val id: Long?,
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    val until: Long,
    val timestamp: LocalDateTime
) : Parcelable {
    companion object {
        val DEFAULT = ConversationNotificationOption(-1L, "", 0L, LocalDateTime.now())

        @JvmStatic
        fun fromConversationOption(option: ConversationOption): ConversationNotificationOption {
            return with(option) {
                ConversationNotificationOption(id, conversationId, muteUntil, timestamp)
            }
        }
    }
}
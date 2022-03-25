package com.mqv.vmess.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

@Entity(tableName = "friend_notification")
class FriendNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long?,
    @ColumnInfo(name = "sender_id")
    val senderId: String,
    val type: FriendNotificationType,
    @ColumnInfo(name = "has_read")
    var hasRead: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime
) {
    companion object {
        @JvmField
        val TEMP = FriendNotification(-1L, "", FriendNotificationType.ACCEPTED_FRIEND, false, LocalDateTime.now())
    }
}

enum class FriendNotificationType {
    @SerializedName("accepted_friend_request")
    ACCEPTED_FRIEND,
    @SerializedName("new_friend_request")
    REQUEST_FRIEND
}
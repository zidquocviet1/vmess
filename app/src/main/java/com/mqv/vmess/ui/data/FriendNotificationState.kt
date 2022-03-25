package com.mqv.vmess.ui.data

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.mqv.vmess.activity.ConversationActivity
import com.mqv.vmess.activity.br.MarkNotificationReadReceiver
import com.mqv.vmess.activity.preferences.PreferenceFriendRequestActivity
import com.mqv.vmess.data.model.FriendNotificationType
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime

@Parcelize
data class FriendNotificationState(
    val id: Long,
    val sender: People,
    val type: FriendNotificationType,
    var hasRead: Boolean = false,
    val createdAt: LocalDateTime
) : Parcelable {
    fun getIntentByType(context: Context): Intent {
        return when (type) {
            FriendNotificationType.ACCEPTED_FRIEND -> {
                val markAsReadIntent =
                    Intent(context, MarkNotificationReadReceiver::class.java).apply {
                        putExtra(MarkNotificationReadReceiver.EXTRA_NOTIFICATION_ID, id)
                    }

                Intent(context, ConversationActivity::class.java).apply {
                    putExtra(ConversationActivity.EXTRA_PARTICIPANT_ID, sender.uid)
                    putExtra(MarkNotificationReadReceiver.EXTRA_INTENT, markAsReadIntent)
                }
            }
            FriendNotificationType.REQUEST_FRIEND -> {
                val markAsReadIntent =
                    Intent(context, MarkNotificationReadReceiver::class.java).apply {
                        putExtra(MarkNotificationReadReceiver.EXTRA_NOTIFICATION_ID, id)
                    }

                Intent(
                    context,
                    PreferenceFriendRequestActivity::class.java
                ).apply {
                    putExtra(MarkNotificationReadReceiver.EXTRA_INTENT, markAsReadIntent)
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FriendNotificationState

        if (id != other.id) return false
        if (sender != other.sender) return false
        if (type != other.type) return false
        if (hasRead != other.hasRead) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + sender.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + hasRead.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}

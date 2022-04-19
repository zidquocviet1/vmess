package com.mqv.vmess.ui.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserSelection(
    val uid: String,
    val photoUrl: String?,
    val displayName: String,
    val isOnline: Boolean,
    var isSelected: Boolean,
    val isConversation: Boolean = false,
    val isGroup: Boolean = false,
    val conversationMetadata: ConversationMetadata? = null
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserSelection

        if (uid != other.uid) return false

        return true
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }
}

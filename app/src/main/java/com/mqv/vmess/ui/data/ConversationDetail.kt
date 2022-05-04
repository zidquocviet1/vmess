package com.mqv.vmess.ui.data

import android.os.Parcelable
import com.mqv.vmess.data.model.ConversationNotificationOption
import kotlinx.parcelize.Parcelize

@Parcelize
data class ConversationDetail(
    val metadata: ConversationMetadata,
    val notificationOption: ConversationNotificationOption
) : Parcelable
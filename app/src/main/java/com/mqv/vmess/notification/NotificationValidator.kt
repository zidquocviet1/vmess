package com.mqv.vmess.notification

import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.model.Conversation
import io.reactivex.rxjava3.core.Single

interface NotificationValidator {
    // Check for show the conversation or not
    // User turn off notification of conversation
    // User currently the same activity
    // User turn off global notification of the app
    fun shouldShowNotification(conversation: Conversation, message: Chat): Single<Boolean>
}
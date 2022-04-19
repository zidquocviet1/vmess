package com.mqv.vmess.notification

import android.content.Context
import com.mqv.vmess.R
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.model.Conversation
import com.mqv.vmess.network.model.User
import com.mqv.vmess.util.MessageUtil.isCallMessage
import com.mqv.vmess.util.MessageUtil.isFileMessage
import com.mqv.vmess.util.MessageUtil.isPhotoMessage
import com.mqv.vmess.util.MessageUtil.isShareMessage
import com.mqv.vmess.util.MessageUtil.isVideoMessage
import java.io.File
import java.util.function.Function
import java.util.stream.Collectors

data class MessageNotificationMetadata(
    val sender: User,
    val conversation: Conversation,
    val message: Chat
) {
    fun getTitle(context: Context): String {
        return when {
            isPhotoMessage(message) -> {
                context.getString(R.string.msg_conversation_list_you_sent_photo, sender.displayName)
            }
            isVideoMessage(message) -> {
                context.getString(R.string.msg_conversation_list_you_sent_video, sender.displayName)
            }
            isFileMessage(message) -> {
                context.getString(R.string.msg_conversation_list_you_sent_file, sender.displayName)
            }
            isCallMessage(message) -> {
                context.getString(R.string.msg_conversation_list_you_call, sender.displayName)
            }
            isShareMessage(message) -> {
                context.getString(R.string.msg_conversation_list_you_sent_link, sender.displayName)
            }
            else -> {
                message.content
            }
        }
    }
}

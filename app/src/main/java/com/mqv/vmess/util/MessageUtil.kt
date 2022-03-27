package com.mqv.vmess.util

import com.mqv.vmess.network.model.Chat

object MessageUtil {
    private const val DUMMY_FIRST_CHAT_PREFIX = "DUMMY_FIRST_CHAT"
    private const val WELCOME_CHAT_PREFIX = "WELCOME_CHAT"
    private const val ADDED_MEMBER_CHAT_ID = "ADDED_MEMBER_CHAT"
    private const val CHANGE_GROUP_NAME_CHAT_ID = "CHANGE_GROUP_NAME_CHAT"
    private const val REMOVE_MEMBER_CHAT_ID = "REMOVE_MEMBER_CHAT"
    private const val MEMBER_LEAVE_GROUP_CHAT_ID = "MEMBER_LEAVE_GROUP_CHAT"
    private const val CHANGE_GROUP_THUMBNAIL_ID = "CHANGE_GROUP_THUMBNAIL_CHAT"

    @JvmStatic
    fun isDummyFirstMessagePair(message: Chat?): Boolean {
        if (message == null) {
            return false
        }

        return isDummyProfileMessage(message) ||
                isWelcomeMessage(message)
    }

    @JvmStatic
    fun isDummyMessage(message: Chat?): Boolean {
        if (message == null) {
            return true
        }

        return isDummyProfileMessage(message) ||
                isWelcomeMessage(message) ||
                isAddedMemberMessage(message) ||
                isRemoveMemberMessage(message) ||
                isMemberLeaveGroupMessage(message) ||
                isChangeGroupNameMessage(message) ||
                isChangeThumbnailMessage(message)
    }

    @JvmStatic
    fun isNotificationMessage(message: Chat?): Boolean {
        if (message == null) {
            return false
        }

        return isAddedMemberMessage(message) ||
                isRemoveMemberMessage(message) ||
                isMemberLeaveGroupMessage(message) ||
                isChangeGroupNameMessage(message) ||
                isChangeThumbnailMessage(message)
    }

    @JvmStatic
    fun isDummyProfileMessage(message: Chat) = message.id.startsWith(DUMMY_FIRST_CHAT_PREFIX)

    @JvmStatic
    fun isWelcomeMessage(message: Chat) = message.id.startsWith(WELCOME_CHAT_PREFIX)

    @JvmStatic
    fun isAddedMemberMessage(message: Chat) = message.id.startsWith(ADDED_MEMBER_CHAT_ID)

    @JvmStatic
    fun isRemoveMemberMessage(message: Chat) = message.id.startsWith(REMOVE_MEMBER_CHAT_ID)

    @JvmStatic
    fun isMemberLeaveGroupMessage(message: Chat) = message.id.startsWith(MEMBER_LEAVE_GROUP_CHAT_ID)

    @JvmStatic
    fun isChangeGroupNameMessage(message: Chat) = message.id.startsWith(CHANGE_GROUP_NAME_CHAT_ID)

    @JvmStatic
    fun isChangeThumbnailMessage(message: Chat) = message.id.startsWith(CHANGE_GROUP_THUMBNAIL_ID)

    @JvmStatic
    fun isMultiMediaMessage(message: Chat) =
        (message.photos != null && message.photos.isNotEmpty()) ||
                (message.files != null && message.files.isNotEmpty()) ||
                message.share != null ||
                (message.videos != null && message.videos.isNotEmpty())
}
package com.mqv.realtimechatapplication.notification

private const val KEY_WHO_CONFIRM = "who_confirm"
private const val KEY_CONVERSATION_ID = "conversation_id"
private const val KEY_WHO_SENT = "who_sent"
private const val KEY_SENDER_ID = "sender_id"
private const val KEY_MESSAGE_ID = "message_id"
private const val KEY_TIMESTAMP = "timestamp"
private const val KEY_NOTIFICATION_TYPE = "type"
private const val KEY_SHOULD_SHOW = "should_show"

sealed class NotificationPayload(
    open val timestamp: Long
) {
    enum class NotificationType {
        FRIEND_REQUEST,
        ACCEPTED_FRIEND_REQUEST,
        INCOMING_MESSAGE,
        ADDED_TO_GROUP
    }

    class AcceptedFriendPayload(
        val whoConfirm: String,
        override val timestamp: Long
    ) : NotificationPayload(timestamp) {
        companion object {
            fun parsePayload(map: MutableMap<String, String>): NotificationPayload {
                val whoConfirm = map[KEY_WHO_CONFIRM]!!
                val timestamp = map[KEY_TIMESTAMP]!!.toLong()

                return AcceptedFriendPayload(whoConfirm, timestamp)
            }
        }

        override fun toString(): String {
            return "Who Accepted: $whoConfirm, Timestamp: $timestamp"
        }
    }

    class FriendRequestPayload(
        val whoSent: String,
        override val timestamp: Long
    ) : NotificationPayload(timestamp) {
        companion object {
            fun parsePayload(map: MutableMap<String, String>): NotificationPayload {
                val whoSent = map[KEY_WHO_SENT]!!
                val timestamp = map[KEY_TIMESTAMP]!!.toLong()

                return FriendRequestPayload(whoSent, timestamp)
            }
        }

        override fun toString(): String {
            return "Who Sent Request: $whoSent, Timestamp: $timestamp"
        }
    }

    class IncomingMessagePayload(
        val senderId: String,
        val messageId: String,
        val conversationId: String,
        val shouldShow: Boolean,
        override val timestamp: Long
    ) : NotificationPayload(timestamp) {
        companion object {
            fun parsePayload(map: MutableMap<String, String>): NotificationPayload {
                val senderId = map[KEY_SENDER_ID]!!
                val messageId = map[KEY_MESSAGE_ID]!!
                val conversationId = map[KEY_CONVERSATION_ID]!!
                val shouldShow = map[KEY_SHOULD_SHOW]!!.toBoolean()
                val timestamp = map[KEY_TIMESTAMP]!!.toLong()

                return IncomingMessagePayload(
                    senderId,
                    messageId,
                    conversationId,
                    shouldShow,
                    timestamp
                )
            }
        }

        override fun toString(): String {
            return "Sender: $senderId," +
                    " Message: $messageId," +
                    " ConversationId: $conversationId," +
                    " Should show: $shouldShow," +
                    " Timestamp: $timestamp"
        }
    }

    class ConversationGroupPayload(
        val conversationId: String,
        override val timestamp: Long
    ) : NotificationPayload(timestamp) {
        companion object {
            fun parsePayload(map: MutableMap<String, String>): NotificationPayload {
                val conversationId = map[KEY_CONVERSATION_ID]!!
                val timestamp = map[KEY_TIMESTAMP]!!.toLong()

                return ConversationGroupPayload(conversationId, timestamp)
            }
        }

        override fun toString(): String {
            return "Conversation: $conversationId, Timestamp: $timestamp"
        }
    }

    companion object {
        @JvmStatic
        fun handleRawPayload(map: MutableMap<String, String>): NotificationPayload {
            val rawType = map[KEY_NOTIFICATION_TYPE]!!

            return when (NotificationType.valueOf(rawType)) {
                NotificationType.ACCEPTED_FRIEND_REQUEST -> AcceptedFriendPayload.parsePayload(map)
                NotificationType.FRIEND_REQUEST -> FriendRequestPayload.parsePayload(map)
                NotificationType.INCOMING_MESSAGE -> IncomingMessagePayload.parsePayload(map)
                NotificationType.ADDED_TO_GROUP -> ConversationGroupPayload.parsePayload(map)
            }
        }
    }
}

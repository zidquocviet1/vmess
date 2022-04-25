package com.mqv.vmess.notification

import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.model.type.MessageStatus
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

private const val KEY_WHO_ACCEPTED = "who_accepted"
private const val KEY_WHO_UNFRIEND = "who_unfriend"
private const val KEY_CONVERSATION_ID = "conversation_id"
private const val KEY_WHO_SENT = "who_sent"
private const val KEY_SENDER_ID = "sender_id"
private const val KEY_MESSAGE_ID = "message_id"
private const val KEY_TIMESTAMP = "timestamp"
private const val KEY_NOTIFICATION_TYPE = "type"
private const val KEY_MESSAGE_CONTENT = "message_content"
private const val KEY_MESSAGE_TIMESTAMP = "message_timestamp"
private const val KEY_CHANGE_OPTION = "change_option"
private const val KEY_MEMBER = "member"
private const val KEY_NOTIFICATION_ID = "notification_id"
private const val KEY_WHO_SEEN = "who_seen"
private const val KEY_MESSAGE_STATUS = "message_status"

sealed class NotificationPayload(
    open val timestamp: Long
) {
    enum class NotificationType {
        FRIEND_REQUEST,
        ACCEPTED_FRIEND_REQUEST,
        INCOMING_MESSAGE,
        STATUS_MESSAGE,
        ADDED_TO_GROUP,
        UNFRIEND,
        GROUP_CHANGE_OPTION,
    }

    class AcceptedFriendPayload(
        val whoAccepted: String,
        val conversationId: String,
        val notificationId: Long,
        override val timestamp: Long
    ) : NotificationPayload(timestamp) {
        companion object {
            fun parsePayload(map: MutableMap<String, String>): NotificationPayload {
                val whoAccepted = map[KEY_WHO_ACCEPTED]!!
                val conversationId = map[KEY_CONVERSATION_ID]!!
                val notificationId = map[KEY_NOTIFICATION_ID]!!
                val timestamp = map[KEY_TIMESTAMP]!!.toLong()

                return AcceptedFriendPayload(
                    whoAccepted,
                    conversationId,
                    notificationId.toLong(),
                    timestamp
                )
            }
        }

        override fun toString(): String {
            return "Who Accepted: $whoAccepted, Conversation ID: $conversationId, Timestamp: $timestamp"
        }
    }

    class FriendRequestPayload(
        val whoSent: String,
        val notificationId: Long,
        override val timestamp: Long
    ) : NotificationPayload(timestamp) {
        companion object {
            fun parsePayload(map: MutableMap<String, String>): NotificationPayload {
                val whoSent = map[KEY_WHO_SENT]!!
                val notificationId = map[KEY_NOTIFICATION_ID]!!.toLong()
                val timestamp = map[KEY_TIMESTAMP]!!.toLong()

                return FriendRequestPayload(whoSent, notificationId, timestamp)
            }
        }

        override fun toString(): String {
            return "Who Sent Request: $whoSent, Timestamp: $timestamp"
        }
    }

    class IncomingMessagePayload(
        val messageJson: String,
        override val timestamp: Long
    ) : NotificationPayload(timestamp) {
        companion object {
            fun parsePayload(map: MutableMap<String, String>): NotificationPayload {
                val messageJson = map["message"]!!
                val timestamp = map[KEY_TIMESTAMP]!!.toLong()

                return IncomingMessagePayload(
                    messageJson,
                    timestamp
                )
            }
        }

        override fun toString(): String {
            return "MessageJson: $messageJson, Timestamp: $timestamp"
        }
    }

    class StatusMessagePayload(
        val messageId: String,
        val status: MessageStatus,
        val whoSeen: Optional<String>,
        override val timestamp: Long
    ) : NotificationPayload(timestamp) {

        companion object {
            fun parsePayload(map: MutableMap<String, String>): NotificationPayload {
                val messageId = map[KEY_MESSAGE_ID]!!
                val statusName = map[KEY_MESSAGE_STATUS]!!
                val whoSeen = map[KEY_WHO_SEEN]!!
                val timestamp = map[KEY_TIMESTAMP]!!.toLong()

                return StatusMessagePayload(
                    messageId,
                    MessageStatus.valueOf(statusName),
                    if (whoSeen.isEmpty()) Optional.empty() else Optional.of(whoSeen),
                    timestamp
                )
            }
        }

        override fun toString(): String {
            return "[Status Message Payload: ${status.name}, Message ID = $messageId, WhoSeen = ${whoSeen.orElse("NONE")}, Timestamp = $timestamp]"
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

    class UnfriendPayload(
        val whoUnfriend: String,
        override val timestamp: Long
    ) : NotificationPayload(timestamp) {
        companion object {
            fun parsePayload(map: MutableMap<String, String>): NotificationPayload {
                val whoUnfriend = map[KEY_WHO_UNFRIEND]!!
                val timestamp = map[KEY_TIMESTAMP]!!.toLong()

                return UnfriendPayload(whoUnfriend, timestamp)
            }
        }

        override fun toString(): String {
            return "Petitioner unfriend: $whoUnfriend, Timestamp: $timestamp"
        }
    }

    class GroupOptionChangedPayload(
        val message: Chat,
        val option: Option,
        val memberJson: String?,
        override val timestamp: Long
    ) : NotificationPayload(timestamp) {
        enum class Option {
            NAME,
            ADDED_MEMBER,
            REMOVE_MEMBER,
            THUMBNAIL,
            LEAVE_GROUP
        }

        companion object {
            fun parsePayload(map: MutableMap<String, String>): NotificationPayload {
                val messageId = map[KEY_MESSAGE_ID]!!
                val senderId = map[KEY_SENDER_ID]!!
                val conversationId = map[KEY_CONVERSATION_ID]!!
                val messageContent = map[KEY_MESSAGE_CONTENT]!!
                val messageTimestamp = map[KEY_MESSAGE_TIMESTAMP]!!.toLong()
                val timestamp = map[KEY_TIMESTAMP]!!.toLong()
                val option = map[KEY_CHANGE_OPTION]!!
                val member = map[KEY_MEMBER]

                val message = Chat(
                    messageId,
                    senderId,
                    messageContent,
                    conversationId,
                    LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(messageTimestamp),
                        ZoneId.systemDefault()
                    ),
                    null,
                    null,
                    null,
                    null,
                    mutableListOf<String>(),
                    null,
                    null,
                    false,
                )

                return GroupOptionChangedPayload(message, Option.valueOf(option), member, timestamp)
            }
        }

        override fun toString(): String {
            return "Message ID: ${message.id}," +
                    " Sender ID: ${message.senderId}," +
                    " Conversation ID: ${message.conversationId}," +
                    " Message Content: ${message.content}," +
                    " Message Timestamp: ${message.timestamp}," +
                    " Timestamp: $timestamp"
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
                NotificationType.STATUS_MESSAGE -> StatusMessagePayload.parsePayload(map)
                NotificationType.ADDED_TO_GROUP -> ConversationGroupPayload.parsePayload(map)
                NotificationType.UNFRIEND -> UnfriendPayload.parsePayload(map)
                NotificationType.GROUP_CHANGE_OPTION -> GroupOptionChangedPayload.parsePayload(map)
            }
        }
    }
}

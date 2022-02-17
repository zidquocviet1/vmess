package com.mqv.realtimechatapplication.ui.data

import android.content.Context
import com.mqv.realtimechatapplication.R
import com.mqv.realtimechatapplication.network.model.Conversation
import com.mqv.realtimechatapplication.network.model.User
import com.mqv.realtimechatapplication.network.model.type.ConversationType
import java.util.function.Predicate
import java.util.stream.Collectors

private const val NUMBER_USER_THRESHOLD = 3
private const val UNKNOWN_USER_ID = "-1"

object ConversationMapper {
    @JvmStatic
    fun mapToMetadata(
        conversation: Conversation,
        currentUser: User,
        context: Context
    ): ConversationMetadata {
        return when (conversation.type!!) {
            ConversationType.SELF -> parseForSelf(currentUser, context)
            ConversationType.NORMAL -> parseForNormal(conversation.participants, currentUser)
            ConversationType.GROUP -> parseForGroup(conversation, currentUser)
        }
    }

    private fun parseForSelf(currentUser: User, context: Context): ConversationMetadata {
        val conversationName = context.getString(R.string.title_just_you)
        val conversationCreatedBy = currentUser.displayName
        val type = ConversationType.SELF

        return ConversationMetadata(
            conversationName,
            listOf(currentUser.photoUrl),
            conversationCreatedBy,
            type,
            UNKNOWN_USER_ID
        )
    }

    private fun parseForNormal(participants: List<User>, currentUser: User): ConversationMetadata {
        val notSelfPredicate = Predicate { u: User -> u.uid != currentUser.uid }
        val other = participants.stream()
            .filter(notSelfPredicate)
            .findFirst()
            .orElseThrow {
                RuntimeException(
                    "Can't find the participant"
                )
            }
        val conversationName = other.displayName
        val conversationCreatedBy = other.displayName
        val type = ConversationType.NORMAL

        return ConversationMetadata(
            conversationName,
            listOf(other.photoUrl),
            conversationCreatedBy,
            type,
            other.uid
        )
    }

    private fun parseForGroup(conversation: Conversation, currentUser: User): ConversationMetadata {
        val notSelfPredicate = Predicate { u: User -> u.uid != currentUser.uid }
        val group = conversation.group
        val participants = conversation.participants

        // Select top 3 user to create a name of conversation by short name of themself
        val nonContainOwner = participants.stream()
            .filter(notSelfPredicate)
            .limit(NUMBER_USER_THRESHOLD.toLong())
            .collect(Collectors.toList())

        val conversationName = if (group.name == null) {
            nonContainOwner.stream()
                .map { u ->
                    val partial =
                        u.displayName.split(" ").toTypedArray()
                    partial[partial.size - 1]
                }.reduce("") { acc, str ->
                    "$acc, $str"
                }.substring(2)
        } else {
            group.name
        }

        val conversationThumbnail = if (group.thumbnail == null) {
            participants.stream()
                .filter(notSelfPredicate)
                .map { u -> u.photoUrl }
                .collect(Collectors.toList())
        } else {
            listOf(group.thumbnail)
        }

        val creatorTemp = User.Builder()
            .setUid(group.creatorId)
            .create()
        val creator = participants[participants.indexOf(creatorTemp)]
        val conversationCreatedBy = creator.displayName
        val type = ConversationType.GROUP

        return ConversationMetadata(
            conversationName,
            conversationThumbnail,
            conversationCreatedBy,
            type,
            UNKNOWN_USER_ID
        )
    }
}
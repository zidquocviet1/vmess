package com.mqv.vmess.ui.data

import android.graphics.Typeface
import android.view.View
import androidx.core.content.ContextCompat
import com.mqv.vmess.R
import com.mqv.vmess.databinding.ItemConversationBinding
import com.mqv.vmess.databinding.ItemRowGroupAvatarBinding
import com.mqv.vmess.databinding.ItemUserAvatarBinding
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.model.Conversation
import com.mqv.vmess.network.model.User
import com.mqv.vmess.network.model.type.ConversationType
import com.mqv.vmess.ui.ImageAvatarView
import com.mqv.vmess.ui.adapter.ChatListAdapter
import com.mqv.vmess.util.DateTimeHelper.getMessageDateTimeFormatted
import com.mqv.vmess.util.MessageUtil

class ConversationListItem(
    private val mBinding: ItemConversationBinding,
    private val mCurrentUser: User,
    mPlaintextCallback: ChatListAdapter.LocalPlaintextInterface
) : ConversationItem<Conversation>(
    mBinding.root.context,
    listOf(),
    mCurrentUser,
    mPlaintextCallback,
    ContextCompat.getColorStateList(mBinding.root.context, R.color.ic_background_tint)!!
) {
    private val mTextUnreadColor = ContextCompat.getColor(mContext, R.color.black)
    private val mDefaultTextViewColor = mBinding.textTitleConversation.currentTextColor

    private var mStatusNormalBinding: ItemUserAvatarBinding? = null
    private var mStatusGroupBinding: ItemRowGroupAvatarBinding? = null

    init {
        mBinding.statusGroupViewstub.setOnInflateListener { _, view ->
            mStatusGroupBinding = ItemRowGroupAvatarBinding.bind(view)
        }
        mBinding.statusNormalViewstub.setOnInflateListener { _, view ->
            mStatusNormalBinding = ItemUserAvatarBinding.bind(view)
        }
    }

    override fun getBindFor() = BindSource.CONVERSATION_LIST

    override fun getAvatarStatusView(type: ConversationType): List<ImageAvatarView> =
        if (type == ConversationType.GROUP) {
            listOf(
                ImageAvatarView(
                    mStatusGroupBinding?.layoutAvatar3!!,
                    mStatusGroupBinding?.imageAvatar3!!
                ),
                ImageAvatarView(
                    mStatusGroupBinding?.layoutAvatar2!!,
                    mStatusGroupBinding?.imageAvatar2!!
                ),
                ImageAvatarView(
                    mStatusGroupBinding?.layoutAvatar1!!,
                    mStatusGroupBinding?.imageAvatar1!!
                )
            )
        } else {
            listOf(
                ImageAvatarView(
                    mStatusNormalBinding?.imageAvatar!!,
                    mStatusNormalBinding?.imageAvatar!!
                )
            )
        }

    override fun bind(item: Conversation) {
        when (item.type!!) {
            ConversationType.SELF, ConversationType.NORMAL -> inflateViewStub(mBinding.statusNormalViewstub)
            ConversationType.GROUP -> inflateViewStub(mBinding.statusGroupViewstub)
        }

        bindConversationName(item)
        bindConversationThumbnail(item)
        bindRecentMessage(item)

        mBinding.iconEncrypted.visibility = if (item.encrypted == true) View.VISIBLE else View.GONE
    }

    override fun bindWelcomeMessage(welcomeMessage: Chat, nextItem: Chat?) {
        markAsUnread(!welcomeMessage.seenBy.contains(mCurrentUser.uid))

        mBinding.statusNormalViewstub.visibility = View.GONE
        mBinding.statusGroupViewstub.visibility = View.GONE
    }

    private fun getConversationMetadata(conversation: Conversation) =
        ConversationMapper.mapToMetadata(conversation, mCurrentUser, mContext)

    private fun markAsUnread(isUnread: Boolean) {
        if (isUnread) {
            mBinding.textTitleConversation.typeface = Typeface.DEFAULT_BOLD
            mBinding.textContentConversation.typeface = Typeface.DEFAULT_BOLD
            mBinding.textCreatedAt.typeface = Typeface.DEFAULT_BOLD
            mBinding.textTitleConversation.setTextColor(mTextUnreadColor)
            mBinding.textContentConversation.setTextColor(mTextUnreadColor)
            mBinding.textCreatedAt.setTextColor(mTextUnreadColor)
        } else {
            mBinding.textTitleConversation.typeface = Typeface.DEFAULT
            mBinding.textContentConversation.typeface = Typeface.DEFAULT
            mBinding.textCreatedAt.typeface = Typeface.DEFAULT
            mBinding.textTitleConversation.setTextColor(mDefaultTextViewColor)
            mBinding.textContentConversation.setTextColor(mDefaultTextViewColor)
            mBinding.textCreatedAt.setTextColor(mDefaultTextViewColor)
        }
    }

    // Public method
    fun bindRecentMessage(item: Conversation) {
        val recentMessage = item.lastChat
        val metadata = getConversationMetadata(item)
        val textContent = StringBuilder()

        if (MessageUtil.isDummyProfileMessage(recentMessage)) return

        if (MessageUtil.isWelcomeMessage(recentMessage)) {
            bindWelcomeMessage(recentMessage, null)

            when (metadata.type) {
                ConversationType.GROUP -> textContent.append(
                    mContext.getString(
                        R.string.dummy_first_chat_group,
                        metadata.conversationCreatedBy
                    )
                )
                else -> textContent.append(mContext.getString(R.string.dummy_first_chat))
            }
        } else if (MessageUtil.isChangeGroupNameMessage(recentMessage)) {
            bindWelcomeMessage(recentMessage, null)

            val senderName = getSenderFromChat(recentMessage, item.participants)?.displayName
                ?: mContext.getString(R.string.dummy_user_name)

            textContent.append(
                mContext.getString(
                    R.string.msg_who_changed_the_group_name,
                    senderName,
                    recentMessage.content
                )
            )
        } else if (MessageUtil.isAddedMemberMessage(recentMessage)) {
            bindWelcomeMessage(recentMessage, null)

            val senderName = getSenderFromChat(recentMessage, item.participants)?.displayName
                ?: mContext.getString(R.string.dummy_user_name)
            val memberName = getSenderById(recentMessage.content, item.participants)?.displayName
                ?: mContext.getString(R.string.dummy_user_name)
            textContent.append(
                mContext.getString(
                    R.string.msg_who_added_another_to_the_group,
                    senderName,
                    memberName
                )
            )
        } else if (MessageUtil.isRemoveMemberMessage(recentMessage)) {
            bindWelcomeMessage(recentMessage, null)

            val senderName = getSenderFromChat(recentMessage, item.participants)?.displayName
                ?: mContext.getString(R.string.dummy_user_name)
            val memberName = recentMessage.content

            textContent.append(
                mContext.getString(
                    R.string.msg_who_remove_another_member,
                    senderName,
                    memberName
                )
            )
        } else if (MessageUtil.isMemberLeaveGroupMessage(recentMessage)) {
            bindWelcomeMessage(recentMessage, null)

            val senderName = getSenderFromChat(recentMessage, item.participants)?.displayName
                ?: mContext.getString(R.string.dummy_user_name)
            textContent.append(
                mContext.getString(
                    R.string.msg_who_leave_group,
                    senderName
                )
            )
        } else if (MessageUtil.isChangeThumbnailMessage(recentMessage)) {
            bindWelcomeMessage(recentMessage, null)

            val senderName = getSenderFromChat(recentMessage, item.participants)?.displayName
                ?: mContext.getString(R.string.dummy_user_name)
            textContent.append(
                mContext.getString(
                    R.string.msg_who_change_group_thumbnail,
                    senderName
                )
            )
        } else {
            bindConversationStatus(item)

            if (recentMessage.isUnsent) {
                textContent.append(
                    getUnsentMessage(recentMessage, item.participants)
                )
            } else {
                val message =
                    getContentFromMessage(recentMessage, metadata, item.encrypted ?: false, item.id)

                // Bugs will occur if normal message has content the same with dummy message
                if (message == mContext.getString(R.string.dummy_encrypted_message)) {
                    mBinding.textContentConversation.typeface =
                        if (!recentMessage.seenBy.contains(mCurrentUser.uid)) Typeface.create(
                            mBinding.textContentConversation.typeface,
                            Typeface.BOLD_ITALIC
                        ) else Typeface.create(
                            mBinding.textContentConversation.typeface,
                            Typeface.ITALIC
                        )
                } else {
                    mBinding.textContentConversation.typeface =
                        if (recentMessage.senderId != mCurrentUser.uid && !recentMessage.seenBy.contains(
                                mCurrentUser.uid
                            )
                        ) Typeface.create(
                            mBinding.textContentConversation.typeface,
                            Typeface.BOLD
                        ) else Typeface.DEFAULT
                }
                textContent.append(message)
            }
        }
        val recentMessageReadableTimestamp = mContext.getString(
            R.string.title_conversation_timestamp,
            getMessageDateTimeFormatted(mContext, recentMessage.timestamp, true)
        )

        mBinding.textCreatedAt.text = recentMessageReadableTimestamp
        mBinding.textContentConversation.text = textContent
    }

    fun bindConversationStatus(conversation: Conversation) {
        val recentMessage = conversation.lastChat
        val userId = mCurrentUser.uid
        val senderId = recentMessage.senderId

        /*
         * If the recently chat belong to another user. Then make the status icon is invisible.
         * Chat's sender id == null. That's mean the chat is dummy first chat.
         * */
        val seenUserId = recentMessage.seenBy

        if (senderId == null || senderId != userId) {
            mStatusGroupBinding?.root?.visibility = View.GONE
            mStatusNormalBinding?.root?.visibility = View.GONE

            markAsUnread(seenUserId.isEmpty() || !seenUserId.contains(userId))
        } else {
            mStatusGroupBinding?.root?.visibility = View.VISIBLE
            mStatusNormalBinding?.root?.visibility = View.VISIBLE

            bindMessageStatus(
                recentMessage,
                conversation.participants,
                conversation.type,
            )
        }
    }

    fun bindPresence(isActive: Boolean) {
        mBinding.includedImageAvatar.imageActive.visibility =
            if (isActive) View.VISIBLE else View.GONE
        mBinding.imageConversationGroup.imageActive.visibility =
            if (isActive) View.VISIBLE else View.GONE
    }

    fun bindNotificationStatus(isTurnOff: Boolean) {
        mBinding.iconNotification.visibility =
            if (isTurnOff) View.VISIBLE else View.GONE
    }

    fun bindConversationName(item: Conversation) {
        mBinding.textTitleConversation.text = getConversationMetadata(item).conversationName
    }

    fun bindConversationThumbnail(item: Conversation) {
        val thumbnails = getConversationMetadata(item).conversationThumbnail

        if (thumbnails.size == 1) {
            mBinding.includedImageAvatar.root.visibility = View.VISIBLE
            mBinding.imageConversationGroup.root.visibility = View.INVISIBLE

            renderImageToView(thumbnails[0], mBinding.includedImageAvatar.imageAvatar)
        } else {
            mBinding.includedImageAvatar.root.visibility = View.INVISIBLE
            mBinding.imageConversationGroup.root.visibility = View.VISIBLE

            renderImageToView(thumbnails[0], mBinding.imageConversationGroup.avatarUser1)
            renderImageToView(thumbnails[1], mBinding.imageConversationGroup.avatarUser2)
        }
    }

    fun bindUnsentMessage(item: Conversation) {
        mBinding.textContentConversation.text = getUnsentMessage(item.lastChat, item.participants)
    }

    fun showLoading() {
        mBinding.progressBarLoading.visibility = View.VISIBLE
        mBinding.includedImageAvatar.root.visibility = View.GONE
        mBinding.imageConversationGroup.root.visibility = View.GONE
        mBinding.textTitleConversation.visibility = View.GONE
        mBinding.textContentConversation.visibility = View.GONE
        mBinding.textCreatedAt.visibility = View.GONE
        mBinding.layoutStatus.visibility = View.GONE
    }

    private fun getContentFromMessage(
        item: Chat,
        metadata: ConversationMetadata,
        isEncrypted: Boolean,
        conversationId: String
    ): String {
        val whoSentDisplayName = if (isSelf(item)) mContext.getString(R.string.msg_you) else
            (getSenderFromChat(
                item,
                metadata.conversationParticipants
            )?.displayName ?: mContext.getString(R.string.dummy_user_name))

        return if (MessageUtil.isCallMessage(item)) {
            mContext.getString(R.string.msg_conversation_list_you_call, whoSentDisplayName)
        } else if (MessageUtil.isShareMessage(item)) {
            mContext.getString(R.string.msg_conversation_list_you_sent_link, whoSentDisplayName)
        } else if (MessageUtil.isVideoMessage(item)) {
            mContext.getString(R.string.msg_conversation_list_you_sent_video, whoSentDisplayName)
        } else if (MessageUtil.isPhotoMessage(item)) {
            mContext.getString(R.string.msg_conversation_list_you_sent_photo, whoSentDisplayName)
        } else if (MessageUtil.isFileMessage(item)) {
            mContext.getString(R.string.msg_conversation_list_you_sent_file, whoSentDisplayName)
        } else {
            if (item.senderId == mCurrentUser.uid) {
                "$whoSentDisplayName: ${
                    item.loadOutgoingMessageContent(
                        conversationId,
                        isEncrypted
                    )
                }"
            } else {
                if (metadata.type == ConversationType.GROUP) {
                    "${
                        getSenderFromChat(
                            item,
                            metadata.conversationParticipants
                        )?.displayName ?: mContext.getString(R.string.dummy_user_name)
                    }: ${item.getContentMessage(isEncrypted)}"
                } else {
                    item.getContentMessage(isEncrypted)
                }
            }
        }
    }

    private fun Chat.getContentMessage(isEncrypted: Boolean): String =
        if (isEncrypted) decryptPlaintextMessage(content, senderId) else content
}
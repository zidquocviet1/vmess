package com.mqv.realtimechatapplication.ui.data

import android.graphics.Typeface
import android.view.View
import androidx.core.content.ContextCompat
import com.mqv.realtimechatapplication.R
import com.mqv.realtimechatapplication.databinding.ItemConversationBinding
import com.mqv.realtimechatapplication.databinding.ItemRowGroupAvatarBinding
import com.mqv.realtimechatapplication.databinding.ItemUserAvatarBinding
import com.mqv.realtimechatapplication.network.model.Chat
import com.mqv.realtimechatapplication.network.model.Conversation
import com.mqv.realtimechatapplication.network.model.User
import com.mqv.realtimechatapplication.network.model.type.ConversationType
import com.mqv.realtimechatapplication.ui.ImageAvatarView
import com.mqv.realtimechatapplication.util.Const
import com.mqv.realtimechatapplication.util.DateTimeHelper.getMessageDateTimeFormatted

class ConversationListItem(
    private val mBinding: ItemConversationBinding,
    private val mCurrentUser: User,
) : ConversationItem<Conversation>(
    mBinding.root.context,
    listOf(),
    mCurrentUser,
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

        if (recentMessage.id.startsWith(Const.DUMMY_FIRST_CHAT_PREFIX)) return

        if (recentMessage.id.startsWith(Const.WELCOME_CHAT_PREFIX)) {
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
        } else {
            bindConversationStatus(item)

            if (recentMessage.senderId == mCurrentUser.uid) {
                textContent.append(mContext.getString(R.string.msg_owner_chat))
            } else {
                if (metadata.type == ConversationType.GROUP) {
                    textContent.append(
                        "${
                            getSenderFromChat(
                                recentMessage,
                                item.participants
                            )!!.displayName
                        }: "
                    )
                }
            }
            textContent.append(recentMessage.content)
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
        mBinding.textContentConversation.text = getUnsentMessage(item.lastChat)
    }
}
package com.mqv.vmess.ui.data

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.mqv.vmess.R
import com.mqv.vmess.databinding.ItemChatBinding
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.model.User
import com.mqv.vmess.network.model.type.ConversationType
import com.mqv.vmess.ui.ImageAvatarView
import com.mqv.vmess.util.DateTimeHelper.getMessageDateTimeFormatted
import com.mqv.vmess.util.MessageUtil
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.stream.Collectors

class ConversationMessageItem(
    private val mBinding: ItemChatBinding,
    private val mMessages: List<Chat>,
    private val mParticipants: List<User>,
    private val mCurrentUser: User,
    private val mMetadata: ConversationMetadata?,
    private val mItemColor: ColorStateList
) : ConversationItem<Chat>(mBinding.root.context, mParticipants, mCurrentUser, mItemColor) {

    private val mChatCornerRadius =
        mContext.resources.getDimensionPixelSize(R.dimen.chat_corner_radius)
    private val mChatCornerRadiusSmall =
        mContext.resources.getDimensionPixelSize(R.dimen.chat_corner_radius_normal)

    init {
//        when (mMetadata.type) {
//            ConversationType.GROUP -> {
//                mBinding.layoutImageStatusMore.visibility = View.VISIBLE
//            }
//            else -> mBinding.layoutImageStatusMore.visibility = View.GONE
//        }
    }

    override fun getBindFor() = BindSource.CONVERSATION

    override fun getAvatarStatusView(type: ConversationType): List<ImageAvatarView> =
        if (type == ConversationType.GROUP) {
            listOf(
                ImageAvatarView(mBinding.imageMessageStatus, mBinding.imageMessageStatus),
                ImageAvatarView(mBinding.layoutImageStatusMore, mBinding.imageMoreMessageStatus)
            )
        } else {
            listOf(ImageAvatarView(mBinding.imageMessageStatus, mBinding.imageMessageStatus))
        }

    override fun bind(item: Chat) {
        val itemChain = getItemChain(item)
        val preItem = itemChain.first
        val nextItem = itemChain.second

        if (item.senderId == null) {
            bindWelcomeMessage(item, nextItem)
        } else {
            if (isSelf(item)) {
                bindSenderMessage(item)
            } else {
                bindReceiverMessage(item)
            }

            bindUnsentMessage(item)
            bindMessageShape(item)
            showTimestamp(preItem, item)
        }
    }

    override fun bindWelcomeMessage(welcomeMessage: Chat, nextItem: Chat?) {
        mMetadata?.let {
            val message = if (mMetadata.type == ConversationType.GROUP) mContext.getString(
                R.string.dummy_first_chat_group,
                mMetadata.conversationCreatedBy
            ) else mContext.getString(R.string.dummy_first_chat)
            mBinding.textWelcome.text = message
        }

        mBinding.layoutWelcome.visibility = if (nextItem != null) View.GONE else View.VISIBLE
        mBinding.layoutReceiver.visibility = View.GONE
        mBinding.layoutSender.visibility = View.GONE
    }

    @SuppressLint("SetTextI18n")
    override fun bindMessageStatus(
        message: Chat,
        participants: List<User>,
        type: ConversationType
    ) {
        super.bindMessageStatus(message, participants, type)

//        val numberOfPeopleHaveSeen = message.seenBy.size
//
//        if (numberOfPeopleHaveSeen > 1) {
//            mBinding.textMoreNumber.text = "+" + "${numberOfPeopleHaveSeen - 1}"
//        }
    }

    private fun bindSenderMessage(item: Chat) {
        mBinding.layoutReceiver.visibility = View.GONE
        mBinding.layoutSender.visibility = View.VISIBLE
        mBinding.layoutWelcome.visibility = View.GONE
        mBinding.textSenderContent.text = item.content
        mBinding.senderChatBackground.backgroundTintList = mItemColor

        bindStatus(item)
    }

    private fun bindReceiverMessage(item: Chat) {
        mBinding.layoutSender.visibility = View.GONE
        mBinding.layoutReceiver.visibility = View.VISIBLE
        mBinding.layoutWelcome.visibility = View.GONE
        mBinding.textReceiverContent.text = item.content
        mBinding.imageReceiver.visibility = View.VISIBLE

        // Render the sender profile image. Not otherUser because we have a group type
        renderImageToView(getSenderFromChat(item)!!.photoUrl, mBinding.imageReceiver)
    }

    private fun bindUnsentMessage(item: Chat, background: View, contentView: TextView) {
        renderUnsentMessage(background, contentView, getUnsentMessage(item))
        bindMessageShape(item)
    }

    private fun renderUnsentMessage(background: View, contentView: TextView, content: String) {
        val backgroundTintColor = ColorStateList.valueOf(
            ContextCompat.getColor(
                mContext,
                R.color.hint_text_color_with_icon
            )
        )
        val backgroundDrawable =
            ContextCompat.getDrawable(mContext, R.drawable.background_rounded_chat_unsent)
        background.alpha = 0.2f
        background.backgroundTintList = backgroundTintColor
        background.background = backgroundDrawable
        contentView.text = content
        contentView.textSize = 14f
        contentView.setTypeface(mBinding.textSenderContent.typeface, Typeface.ITALIC)
        contentView.setTextColor(
            ContextCompat.getColor(
                mContext,
                R.color.hint_text_color_with_icon
            )
        )
    }

    fun bindUnsentMessage(item: Chat) {
        if (item.isUnsent) {
            when (isSelf(item)) {
                true -> bindUnsentMessage(
                    item,
                    mBinding.senderChatBackground,
                    mBinding.textSenderContent
                )
                false -> bindUnsentMessage(
                    item,
                    mBinding.receiverChatBackground,
                    mBinding.textReceiverContent
                )
            }
        }
    }

    fun bindStatus(item: Chat) {
        findLastSeenStatus(item)
        mMetadata?.let { bindMessageStatus(item, mParticipants, mMetadata.type) }
    }

    fun bindMessageShape(item: Chat?) {
        val itemChain = getItemChain(item)
        val preItem = itemChain.first
        val nextItem = itemChain.second

        if (item == null) return

        /*
         * Check if the preItem is the dummy chat or not.
         * If the preItem is the dummy chat so the current item will show normally.
         * */
        if (MessageUtil.isDummyMessage(preItem)) {
            val senderId = item.senderId ?: return
            if (nextItem != null && !shouldShowTimestamp(
                    item,
                    nextItem
                ) && senderId == nextItem.senderId
            ) {
                reformatCornerRadius(
                    item,
                    mChatCornerRadius,
                    mChatCornerRadius,
                    mChatCornerRadius,
                    mChatCornerRadiusSmall
                )
                if (shouldShowTimestamp(item, nextItem)) {
                    showIconReceiver()
                } else {
                    hiddenIconReceiver()
                }
            } else {
                reformatCornerRadius(
                    item,
                    mChatCornerRadius,
                    mChatCornerRadius,
                    mChatCornerRadius,
                    mChatCornerRadius
                )
            }
            return
        }

        /*
         * Section to render bunch of sender chat item.
         * */
        if (isReceiveMoreThanTwo(item, nextItem)) {
            /*
             * Don't check the nextItem is null or not. Because isReceiveMoreThanTwo method did that.
             * */
            if (shouldShowTimestamp(item, nextItem)) {
                showIconReceiver()
            } else {
                hiddenIconReceiver()
            }
        } else {
            showIconReceiver()
        }
        renderBunchOfChats(preItem!!, item, nextItem)
        findLastSeenStatus(item)
    }

    fun showLoading() {
        mBinding.progressBarLoading.visibility = View.VISIBLE
        mBinding.layoutSender.visibility = View.GONE
        mBinding.layoutReceiver.visibility = View.GONE
    }

    /*
     * The method to check the duration time of two chat in a row larger than 10 minutes or not.
     * */
    fun showTimestamp(preItem: Chat?, item: Chat) {
        if (preItem == null || MessageUtil.isDummyMessage(preItem)) {
            return
        }
        if (MessageUtil.isWelcomeMessage(preItem)) {
            mBinding.textTimestamp.visibility = View.VISIBLE
            mBinding.textTimestamp.text = getMessageDateTimeFormatted(mContext, item.timestamp)
            return
        }
        if (shouldShowTimestamp(preItem, item)) {
            mBinding.textTimestamp.visibility = View.VISIBLE
            mBinding.textTimestamp.text = getMessageDateTimeFormatted(mContext, item.timestamp)
        } else {
            mBinding.textTimestamp.visibility = View.GONE
        }
    }

    private fun findLastSeenStatus(item: Chat) {
        if (isSelf(item) && !MessageUtil.isDummyMessage(item)) {
            val mSenderChatList: List<Chat> = mMessages.stream()
                .filter { c ->
                    c != null &&
                            c.senderId != null &&
                            isSelf(c) &&
                            !MessageUtil.isDummyMessage(c) &&
                            c.seenBy.isNotEmpty()
                }
                .collect(Collectors.toList())

            if (mSenderChatList.contains(item)) {
                changeSeenStatusVisibility(mSenderChatList.indexOf(item) == mSenderChatList.size - 1)
            } else {
                val nextSeenChat = mSenderChatList.stream()
                    .filter { c -> c.timestamp >= item.timestamp }
                    .findFirst()
                changeSeenStatusVisibility(!nextSeenChat.isPresent)
            }
        }
    }

    private fun changeSeenStatusVisibility(shouldShow: Boolean) {
        mBinding.imageMessageStatus.visibility =
            if (shouldShow) View.VISIBLE else View.INVISIBLE

//        if (isGroup()) {
//            mBinding.layoutImageStatusMore.visibility =
//                if (shouldShow) View.VISIBLE else View.GONE
//        }

//        if (mMetadata.type != ConversationType.GROUP) {
////            mBinding.imageMessageStatus.visibility =
////                if (shouldShow) View.VISIBLE else View.INVISIBLE
//        } else {
//            mBinding.layoutImageStatusMore.visibility =
//                if (shouldShow) View.VISIBLE else View.INVISIBLE
//        }
    }

    private fun hiddenIconReceiver() {
        mBinding.imageReceiver.visibility = View.INVISIBLE
    }

    private fun showIconReceiver() {
        mBinding.imageReceiver.visibility = View.VISIBLE
    }

    private fun getBackground(item: Chat): View {
        return if (isSelf(item)) {
            mBinding.senderChatBackground
        } else {
            mBinding.receiverChatBackground
        }
    }

    /*
     * Check the Current Chat vs Next Item is own by one user or not
     * */
    private fun isReceiveMoreThanTwo(item: Chat, nextItem: Chat?): Boolean {
        if (nextItem == null) return false
        val itemSenderId = item.senderId
        val nextItemSenderId = nextItem.senderId
        val currentUserId = mCurrentUser.uid
        return itemSenderId == nextItemSenderId && itemSenderId != currentUserId
    }

    /*
     * Render the bunch of chats with the dynamic corner radius of background
     * */
    private fun renderBunchOfChats(
        prev: Chat,
        cur: Chat,
        next: Chat?
    ) {
        val preSenderId = prev.senderId
        val curSenderId = cur.senderId
        if (preSenderId != curSenderId) {
            if (next != null && curSenderId == next.senderId && !shouldShowTimestamp(cur, next)) {
                reformatCornerRadius(
                    cur,
                    mChatCornerRadius,
                    mChatCornerRadius,
                    mChatCornerRadius,
                    mChatCornerRadiusSmall
                )
            } else {
                reformatCornerRadius(
                    cur,
                    mChatCornerRadius,
                    mChatCornerRadius,
                    mChatCornerRadius,
                    mChatCornerRadius
                )
            }
        } else {
            if (!shouldShowTimestamp(prev, cur)) {
                if (next != null) {
                    if (shouldShowTimestamp(cur, next) || curSenderId != next.senderId) {
                        reformatCornerRadius(
                            cur,
                            mChatCornerRadiusSmall,
                            mChatCornerRadius,
                            mChatCornerRadius,
                            mChatCornerRadius
                        )
                    } else {
                        reformatCornerRadius(
                            cur,
                            mChatCornerRadiusSmall,
                            mChatCornerRadius,
                            mChatCornerRadius,
                            mChatCornerRadiusSmall
                        )
                    }
                } else {
                    reformatCornerRadius(
                        cur,
                        mChatCornerRadiusSmall,
                        mChatCornerRadius,
                        mChatCornerRadius,
                        mChatCornerRadius
                    )
                }
            } else {
                if (next != null) {
                    if (shouldShowTimestamp(prev, cur) && shouldShowTimestamp(cur, next)) {
                        reformatCornerRadius(
                            cur,
                            mChatCornerRadius,
                            mChatCornerRadius,
                            mChatCornerRadius,
                            mChatCornerRadius
                        )
                        return
                    }
                    if (curSenderId != next.senderId) {
                        reformatCornerRadius(
                            cur,
                            mChatCornerRadius,
                            mChatCornerRadius,
                            mChatCornerRadius,
                            mChatCornerRadius
                        )
                    } else {
                        if (shouldShowTimestamp(prev, cur)) {
                            reformatCornerRadius(
                                cur,
                                mChatCornerRadius,
                                mChatCornerRadius,
                                mChatCornerRadius,
                                mChatCornerRadiusSmall
                            )
                        } else {
                            reformatCornerRadius(
                                cur,
                                mChatCornerRadiusSmall,
                                mChatCornerRadius,
                                mChatCornerRadius,
                                mChatCornerRadiusSmall
                            )
                        }
                    }
                } else {
                    reformatCornerRadius(
                        cur,
                        mChatCornerRadius,
                        mChatCornerRadius,
                        mChatCornerRadius,
                        mChatCornerRadius
                    )
                }
            }
        }
    }

    @Suppress("NAME_SHADOWING")
    private fun reformatCornerRadius(
        item: Chat,
        topLeft: Int,
        topRight: Int,
        bottomRight: Int,
        bottomLeft: Int
    ) {
        var topLeft = topLeft
        var topRight = topRight
        var bottomRight = bottomRight
        var bottomLeft = bottomLeft

        if (item.senderId == null) return

        val v = getBackground(item)
        val drawable = v.background as GradientDrawable
        val isChatFromSender = isSelf(item)

        if (isChatFromSender) {
            topLeft = topLeft xor topRight
            topRight = topLeft xor topRight
            topLeft = topLeft xor topRight
            bottomLeft = bottomLeft xor bottomRight
            bottomRight = bottomLeft xor bottomRight
            bottomLeft = bottomLeft xor bottomRight
        } else if (isStartOfClusterOrSingleMessage(topLeft, topRight, bottomRight, bottomLeft)) {
            showSenderName(item)
        } else {
            mBinding.textReceiverName.visibility = View.GONE
        }

        drawable.cornerRadii = floatArrayOf(
            topLeft.toFloat(), topLeft.toFloat(),
            topRight.toFloat(), topRight.toFloat(),
            bottomRight.toFloat(), bottomRight.toFloat(),
            bottomLeft.toFloat(), bottomLeft
                .toFloat()
        )
        v.background = drawable
    }

    /*
     * The method to check the duration time of two chat in a row larger than 10 minutes or not.
     * */
    private fun shouldShowTimestamp(item: Chat, nextItem: Chat?): Boolean {
        if (MessageUtil.isDummyMessage(item) || MessageUtil.isDummyMessage(nextItem)) {
            return true
        }

        val from = item.timestamp
        val to = nextItem!!.timestamp
        val minuteDuration = ChronoUnit.MINUTES.between(from, to)
        return minuteDuration > 10
    }

    private fun getItemChain(currentItem: Chat?): Pair<Chat?, Chat?> {
        val position = mMessages.indexOf(currentItem)
        val preItem = if (position - 1 >= 0) mMessages[position - 1] else null
        val nextItem = if (position + 1 < mMessages.size) mMessages[position + 1] else null

        return Pair(preItem, nextItem)
    }

    private fun showSenderName(message: Chat) {
        if (isGroup()) {
            getSenderFromChat(message)?.let { u ->
                with(mBinding.textReceiverName) {
                    text = u.displayName
                    visibility = View.VISIBLE
                }
            }
        } else {
            mBinding.textReceiverName.visibility = View.GONE
        }
    }

    private fun isGroup() = if (mMetadata != null) mMetadata.type == ConversationType.GROUP else false

    /*
    * Detect the incoming message by shape itself
    * */
    private fun isStartOfClusterOrSingleMessage(
        topLeft: Int,
        topRight: Int,
        bottomRight: Int,
        bottomLeft: Int
    ): Boolean {
        val arr = intArrayOf(topLeft, topRight, bottomLeft, bottomRight)
        val max = Arrays.stream(arr).summaryStatistics().max

        return (topLeft == max) || arr.distinct().size == 1
    }
}

package com.mqv.vmess.ui.data

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewStub
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.mqv.vmess.R
import com.mqv.vmess.message.MessageDecryption
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.model.User
import com.mqv.vmess.network.model.type.ConversationType
import com.mqv.vmess.network.model.type.MessageStatus
import com.mqv.vmess.ui.ImageAvatarView
import com.mqv.vmess.ui.adapter.ChatListAdapter
import com.mqv.vmess.util.Picture
import java.util.*

abstract class ConversationItem<T>(
    val mContext: Context,
    private val mParticipants: List<User>?,
    private val mCurrentUser: User,
    private val mPlaintextCallback: ChatListAdapter.LocalPlaintextInterface,
    mIconColor: ColorStateList
) : BindableConversation<T> {
    private val mErrorChatColor =
        ColorStateList.valueOf(ContextCompat.getColor(mContext, android.R.color.holo_red_light))
    protected val mReceivedIconDrawable =
        ContextCompat.getDrawable(mContext, R.drawable.ic_round_check_circle)
    protected val mNotReceivedIconDrawable =
        ContextCompat.getDrawable(mContext, R.drawable.ic_round_check_circle_outline)
    protected val mSendingIconDrawable =
        ContextCompat.getDrawable(mContext, R.drawable.ic_outline_circle)
    private val mErrorIconDrawable =
        ContextCompat.getDrawable(mContext, R.drawable.ic_round_error)

    init {
        mReceivedIconDrawable!!.setTintList(mIconColor)
        mNotReceivedIconDrawable!!.setTintList(mIconColor)
        mSendingIconDrawable!!.setTintList(mIconColor)
        mErrorIconDrawable!!.setTintList(mErrorChatColor)
    }

    enum class BindSource {
        CONVERSATION_LIST,
        CONVERSATION
    }

    abstract fun getAvatarStatusView(type: ConversationType): List<ImageAvatarView>
    abstract fun getBindFor(): BindSource

    override fun bindMessageStatus(
        message: Chat,
        participants: List<User>,
        type: ConversationType
    ) {
        val seenBy = message.seenBy
        val target = getAvatarStatusView(type)

        if (seenBy != null && seenBy.size > 0) {
            when (type) {
                ConversationType.SELF -> target.forEach { t ->
                    t.parentLayout.visibility = View.GONE
                }
                ConversationType.NORMAL ->
                    seenBy.stream()
                        .filter { u -> u != mCurrentUser.uid }
                        .findAny()
                        .ifPresent { id ->
                            renderImageToView(
                                getSenderById(id, participants)!!.photoUrl,
                                target[0].avatarView
                            )
                        }
                ConversationType.GROUP -> {
                    if (getBindFor() == BindSource.CONVERSATION_LIST) {
                        var availableImageSlot = 3
                        var currentImageIndex = 0

                        seenBy.stream()
                            .flatMap { id ->
                                participants.stream()
                                    .filter { u -> u.uid == id }
                            }
                            .forEach { u ->
                                if (availableImageSlot > 0) {
                                    renderImageToView(
                                        u.photoUrl,
                                        target[currentImageIndex].avatarView
                                    )
                                    target[currentImageIndex].parentLayout.visibility = View.VISIBLE
                                    target[currentImageIndex].parentLayout.background =
                                        ContextCompat.getDrawable(
                                            mContext,
                                            R.drawable.avatar_stroke_ripple
                                        )
                                    target[currentImageIndex].parentLayout.setPadding(
                                        mContext.resources.getDimensionPixelSize(R.dimen.chat_status_row_user_padding),
                                        mContext.resources.getDimensionPixelSize(R.dimen.chat_status_row_user_padding),
                                        mContext.resources.getDimensionPixelSize(R.dimen.chat_status_row_user_padding),
                                        mContext.resources.getDimensionPixelSize(R.dimen.chat_status_row_user_padding),
                                    )

                                    currentImageIndex += 1
                                    availableImageSlot -= 1
                                }
                            }

                        for (index in target.size downTo currentImageIndex) {
                            if (index < target.size) {
                                target[index].parentLayout.visibility = View.GONE
                            }
                        }

                        if (currentImageIndex == 1) {
                            target[0].parentLayout.setPadding(0, 0, 0, 0)
                        }
                    } else {
                        renderImageToView(
                            getSenderById(seenBy[0], participants)?.photoUrl,
                            target[0].avatarView
                        )
//                        target[1].avatarView.setImageDrawable(Picture.getErrorAvatarLoaded(mContext))
//                        target[1].parentLayout.visibility =
//                            if (seenBy.size == 1) View.GONE else View.VISIBLE
                    }
                }
            }
        } else {
            when (type) {
                ConversationType.GROUP -> {
                    if (getBindFor() == BindSource.CONVERSATION_LIST) {
                        target[0].parentLayout.setPadding(0, 0, 0, 0)
                        target[0].parentLayout.background = null
                        target[1].parentLayout.visibility = View.GONE
                        target[2].parentLayout.visibility = View.GONE
                    } else {
                        target[1].parentLayout.visibility = View.GONE
                    }
                }
                else -> {}
            }
            when (message.status) {
                MessageStatus.RECEIVED -> target[0].avatarView.setImageDrawable(
                    mReceivedIconDrawable
                )
                MessageStatus.NOT_RECEIVED -> target[0].avatarView.setImageDrawable(
                    mNotReceivedIconDrawable
                )
                MessageStatus.SENDING -> target[0].avatarView.setImageDrawable(mSendingIconDrawable)
                MessageStatus.ERROR -> target[0].avatarView.setImageDrawable(mErrorIconDrawable)
                else -> {}
            }
        }
    }

    protected fun getUnsentMessage(message: Chat, participants: List<User>? = mParticipants): String {
        return if (isSelf(message)) {
            mContext.getString(R.string.title_sender_chat_unsent)
        } else {
            mContext.getString(R.string.title_receiver_chat_unsent, getSenderFromChat(message, participants)?.displayName ?: mContext.getString(R.string.dummy_user_name))
        }
    }

    protected fun renderImageToView(photoUrl: String?, target: ImageView) {
        Picture.loadUserAvatar(mContext, photoUrl).into(target)
    }

    protected fun isSelf(item: Chat): Boolean {
        return item.senderId != null && item.senderId == mCurrentUser.uid
    }

    protected fun getSenderFromChat(
        message: Chat,
        participants: List<User>? = mParticipants
    ): User? {
        return getSenderById(message.senderId, participants)
    }

    protected fun inflateViewStub(viewStub: ViewStub) {
        if (viewStub.parent != null) {
            viewStub.inflate()
        } else {
            viewStub.visibility = View.VISIBLE
        }
    }

    protected fun getSenderById(uid: String, participants: List<User>?): User? {
        val user = User.Builder()
            .setUid(uid)
            .create()
        val index = participants?.indexOf(user)

        return index?.run { return if (this == -1) getUserIfNotAsParticipant(uid) else participants[this] }
    }

    protected fun decryptPlaintextMessage(message: String, participant: String): String =
        MessageDecryption.decrypt(mContext, message, participant, 1)

    protected fun Chat.loadOutgoingMessageContent(isEncrypted: Boolean): String = if (isEncrypted) {
        mPlaintextCallback.loadPlaintextContentForOutgoingEncryptedMessage("", id)
    } else {
        content
    }

    protected fun Chat.loadOutgoingMessageContent(conversationId: String = "", isEncrypted: Boolean): String = if (isEncrypted) {
        mPlaintextCallback.loadPlaintextContentForOutgoingEncryptedMessage(conversationId, id)
    } else {
        content
    }

    private fun getUserIfNotAsParticipant(userId: String): User =
        sUserLeftGroup.stream()
            .filter { it.uid == userId }
            .findFirst()
            .orElse(createApplicationUser())

    private fun createApplicationUser(): User =
        User.Builder()
            .setUid(UUID.randomUUID().toString())
            .setDisplayName(mContext.getString(R.string.dummy_user_name))
            .create()

    companion object {
        private var sUserLeftGroup: List<User> = mutableListOf()

        @JvmStatic
        fun setUserLeftGroup(userLeftGroup: List<User>) {
            sUserLeftGroup = userLeftGroup
        }
    }
}
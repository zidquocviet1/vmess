package com.mqv.realtimechatapplication.notification

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.mqv.realtimechatapplication.MainApplication
import com.mqv.realtimechatapplication.activity.ConversationActivity
import com.mqv.realtimechatapplication.activity.MainActivity
import com.mqv.realtimechatapplication.data.MyDatabase
import com.mqv.realtimechatapplication.network.exception.ResourceNotFoundException
import com.mqv.realtimechatapplication.network.model.Conversation
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType
import com.mqv.realtimechatapplication.network.service.ConversationService
import com.mqv.realtimechatapplication.notification.NotificationPayload.*
import com.mqv.realtimechatapplication.reactive.ReactiveExtension.authorizeToken
import com.mqv.realtimechatapplication.reactive.RxHelper
import com.mqv.realtimechatapplication.work.LifecycleUtil
import io.reactivex.rxjava3.core.Completable
import java.util.concurrent.Executors

class NotificationHandler(
    private val mContext: Context,
    private val mDatabase: MyDatabase,
    private val mConversationService: ConversationService
) : NotificationEntry {
    private val mUser = FirebaseAuth.getInstance().currentUser!!
    private val mBoundExecutor = Executors.newFixedThreadPool(3)

    override fun handleNotificationPayload(payload: NotificationPayload) {
        when (payload) {
            is AcceptedFriendPayload -> handleAcceptedFriend(payload)
            is FriendRequestPayload -> handleFriendRequest(payload)
            is IncomingMessagePayload -> handleMessage(payload)
            is ConversationGroupPayload -> handleConversationGroup(payload)
        }
    }

    /*
    * Handle the firebase notification when the server send new message
    * Message from Group or User only
    * */
    private fun handleMessage(payload: IncomingMessagePayload) {
        val messageId = payload.messageId
        val senderId = payload.senderId
        val conversationId = payload.conversationId
        val shouldShow = payload.shouldShow

        fetchConversation(conversationId) { conversation ->
            val app = mContext.applicationContext as MainApplication
            val isInCurrentConversationOrMainActivity = app.activeActivity.run {
                if (this is ConversationActivity) {
                    return@run conversationId == this.extraConversationId
                } else if (this is MainActivity) {
                    return@run true
                }
                return@run false
            }

            if (shouldShow && (!LifecycleUtil.isAppForeground() || !isInCurrentConversationOrMainActivity)) {
                mBoundExecutor.execute {
                    val message = conversation.chats.stream()
                        .filter { it.id == messageId }
                        .findFirst()
                        .orElseThrow { ResourceNotFoundException() }
                    val sender = conversation.participants.stream()
                        .filter { it.uid == senderId }
                        .findFirst()
                        .orElseThrow { ResourceNotFoundException() }
                    NotificationUtil.sendIncomingMessageNotification(
                        mContext,
                        sender,
                        message,
                        conversation
                    )
                }
            }
        }
    }

    /*
    * Handle new conversation added, in this scenario when the user is added into a group by the others
    * */
    private fun handleConversationGroup(payload: ConversationGroupPayload) {
        fetchConversation(payload.conversationId) { conversation ->
            NotificationUtil.sendAddedConversationNotification(
                mContext,
                conversation
            )
        }
    }

    /*
    * Handle the request when user send new friend connection, accepted friend request.
    * */
    private fun handleFriendRequest(payload: FriendRequestPayload) {
        val whoSent = payload.whoSent
    }

    /*
    * Received when user's friend request is accepted by the others
    * */
    private fun handleAcceptedFriend(payload: AcceptedFriendPayload) {
        val whoConfirm = payload.whoConfirm
    }

    private fun fetchConversation(
        conversationId: String,
        onSuccess: (conversation: Conversation) -> Unit
    ) {
        mUser.authorizeToken()
            .flatMap { token -> mConversationService.findById(token, conversationId) }
            .compose(RxHelper.parseSingleResponseData())
            .flatMap { conversation ->
                conversation.status = ConversationStatusType.INBOX

                return@flatMap Completable.fromAction {
                    mDatabase.conversationDao.saveConversationList(
                        listOf(conversation)
                    )
                }
                    .compose(RxHelper.applyCompleteSchedulers())
                    .onErrorComplete()
                    .toSingleDefault(conversation)
            }
            .subscribe { conversation -> onSuccess(conversation) }
    }

    companion object {
        val TAG: String = NotificationHandler::class.java.simpleName
    }
}
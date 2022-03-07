package com.mqv.realtimechatapplication.notification

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.mqv.realtimechatapplication.data.MyDatabase
import com.mqv.realtimechatapplication.dependencies.AppDependencies
import com.mqv.realtimechatapplication.network.exception.ResourceNotFoundException
import com.mqv.realtimechatapplication.network.model.Chat
import com.mqv.realtimechatapplication.network.model.Conversation
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType
import com.mqv.realtimechatapplication.network.service.ChatService
import com.mqv.realtimechatapplication.network.service.ConversationService
import com.mqv.realtimechatapplication.notification.NotificationPayload.*
import com.mqv.realtimechatapplication.reactive.ReactiveExtension.authorizeToken
import com.mqv.realtimechatapplication.reactive.RxHelper
import com.mqv.realtimechatapplication.util.Logging
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.*
import java.util.concurrent.Executors

class NotificationHandler(
    private val mContext: Context,
    private val mDatabase: MyDatabase,
    private val mConversationService: ConversationService,
    private val mChatService: ChatService
) : NotificationEntry {
    private val mUser = FirebaseAuth.getInstance().currentUser!!
    private val mBoundExecutor = Executors.newFixedThreadPool(3)

    override fun handleNotificationPayload(payload: NotificationPayload) {
        when (payload) {
            is AcceptedFriendPayload -> handleAcceptedFriend(payload)
            is FriendRequestPayload -> handleFriendRequest(payload)
            is IncomingMessagePayload -> handleMessage(payload)
            is StatusMessagePayload -> handleStatusMessage(payload)
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

        // Only handle the notification message in the background and the websocket is not available
        if (AppDependencies.getWebSocket().isDead) {
            shouldHaveConversationInCache(conversationId).zipWith(
                notifyReceivedIncomingMessage(messageId)
            ) { conversation, message -> Pair(conversation, message) }
                .flatMap { checkCacheConversationAndReturnFresh(it, conversationId, messageId) }
                .map { mapPairToMessageNotificationMetadata(it, senderId) }
                .subscribe { metadata, _ ->
                    NotificationUtil.sendIncomingMessageNotification(
                        mContext,
                        metadata
                    )
                }
        }
    }


    // Update the message status when participants push received or seen.
    private fun handleStatusMessage(payload: StatusMessagePayload) {
        fetchIncomingMessageRemote(payload.messageId)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .concatMapCompletable { c ->
                mDatabase.chatDao.insert(c)
                    .andThen {
                        AppDependencies.getDatabaseObserver()
                            .notifyMessageUpdated(c.conversationId, c.id)
                    }
            }
            .onErrorComplete()
            .subscribe { }
    }

    /*
    * Handle new conversation added, in this scenario when the user is added into a group by the others
    * */
    private fun handleConversationGroup(payload: ConversationGroupPayload) {
        fetchConversation(payload.conversationId)
            .subscribe { conversation, _ ->
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

    private fun mapPairToMessageNotificationMetadata(
        freshData: Pair<Conversation, Chat>,
        senderId: String
    ): MessageNotificationMetadata {
        val conversation = freshData.first
        val message = freshData.second
        val sender = conversation.participants.stream()
            .filter { it.uid == senderId }
            .findFirst()
            .orElseThrow { ResourceNotFoundException() }

        return MessageNotificationMetadata(sender, conversation, message)
    }

    private fun checkCacheConversationAndReturnFresh(
        pair: Pair<Optional<Conversation>, Chat>,
        conversationId: String,
        messageId: String
    ): Single<Pair<Conversation, Chat>> {
        if (pair.first.isPresent) {
            val message = pair.second

            return mDatabase.chatDao.insert(message)
                .andThen(Single.create { emitter ->
                    val conversation = pair.first.get()

                    if (!emitter.isDisposed) {
                        emitter.onSuccess(Pair(conversation, message))
                    }
                })
        } else {
            return fetchConversation(conversationId)
                .map { conversation ->
                    val message = conversation.chats.stream()
                        .filter { it.id == messageId }
                        .findFirst()
                        .orElseThrow { ResourceNotFoundException() }

                    Pair(conversation, message)
                }
        }
    }

    private fun fetchIncomingMessageRemote(messageId: String) =
        mUser.authorizeToken().flatMap { token ->
            mChatService.fetchById(
                token,
                messageId
            ).singleOrError().compose(RxHelper.parseSingleResponseData())
        }

    private fun notifyReceivedIncomingMessage(messageId: String): Single<Chat> =
        mUser.authorizeToken()
            .flatMapObservable { token ->
                mChatService.notifyReceiveMessage(token, messageId)
            }
            .compose(RxHelper.parseResponseData())
            .singleOrError()

    // Check if the conversation is stored in the local database then return it
    private fun shouldHaveConversationInCache(conversationId: String): Single<Optional<Conversation>> {
        return mDatabase.conversationDao.fetchById(conversationId)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .flatMap { map ->
                Single.just(
                    map.keys
                        .stream()
                        .filter { c: Conversation -> c.id == conversationId }
                        .findFirst()
                )
            }
    }

    // Fetch the conversation information from remote when the local database has not stored it yet.
    private fun fetchConversation(conversationId: String): Single<Conversation> =
        mUser.authorizeToken()
            .flatMap { token -> mConversationService.findById(token, conversationId) }
            .compose(RxHelper.parseSingleResponseData())
            .flatMap { conversation ->
                Logging.debug(TAG, "Conversation fetch successfully")

                conversation.status = ConversationStatusType.INBOX
                conversation.chats.reverse()

                return@flatMap Completable.fromAction {
                    mDatabase.conversationDao.saveConversationList(
                        listOf(conversation)
                    )
                }
                    .doOnError {
                        Logging.debug(
                            TAG,
                            "Insert conversation into database failure: ${it.message}"
                        )
                    }
                    .doOnComplete {
                        Logging.debug(
                            TAG,
                            "Insert conversation into database success"
                        )
                    }
                    .compose(RxHelper.applyCompleteSchedulers())
                    .onErrorComplete()
                    .toSingleDefault(conversation)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())

    companion object {
        val TAG: String = NotificationHandler::class.java.simpleName
    }
}
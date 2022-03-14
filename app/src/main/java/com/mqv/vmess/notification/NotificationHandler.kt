package com.mqv.vmess.notification

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.mqv.vmess.data.MyDatabase
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.network.exception.ResourceNotFoundException
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.model.Conversation
import com.mqv.vmess.network.model.User
import com.mqv.vmess.network.model.type.ConversationStatusType
import com.mqv.vmess.network.service.ChatService
import com.mqv.vmess.network.service.ConversationService
import com.mqv.vmess.network.service.UserService
import com.mqv.vmess.notification.NotificationPayload.*
import com.mqv.vmess.reactive.ReactiveExtension.authorizeToken
import com.mqv.vmess.reactive.RxHelper
import com.mqv.vmess.ui.data.People
import com.mqv.vmess.util.Logging
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.*

class NotificationHandler(
    private val mContext: Context,
    private val mDatabase: MyDatabase,
    private val mConversationService: ConversationService,
    private val mChatService: ChatService,
    private val mUserService: UserService,
    private val gson: Gson
) : NotificationEntry {
    private val mUser = FirebaseAuth.getInstance().currentUser!!

    override fun handleNotificationPayload(payload: NotificationPayload) {
        when (payload) {
            is AcceptedFriendPayload -> handleAcceptedFriend(payload)
            is FriendRequestPayload -> handleFriendRequest(payload)
            is IncomingMessagePayload -> handleMessage(payload)
            is StatusMessagePayload -> handleStatusMessage(payload)
            is ConversationGroupPayload -> handleConversationGroup(payload)
            is UnfriendPayload -> handleUnfriend(payload)
            is GroupOptionChangedPayload -> handleGroupChangeOption(payload)
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

        fetchUserDetail(whoSent)
            .onErrorComplete()
            .subscribe { u ->
                NotificationUtil.sendFriendRequestNotification(mContext, u)
            }
    }

    /*
    * Received when user's friend request is accepted by the others
    * */
    private fun handleAcceptedFriend(payload: AcceptedFriendPayload) {
        val whoConfirm = payload.whoAccepted
        val conversationId = payload.conversationId

        fetchConversation(conversationId)
            .subscribe { c, _ ->
                val user = c.participants.stream()
                    .filter { u -> u.uid == whoConfirm }
                    .findFirst()
                    .orElseThrow { ResourceNotFoundException() }

                NotificationUtil.sendAcceptedFriendRequestNotification(mContext, user)

                AppDependencies.getDatabaseObserver().notifyConversationInserted(c.id)
            }
    }

    private fun handleUnfriend(payload: UnfriendPayload) {
        val whoUnfriend = payload.whoUnfriend
        val dao = mDatabase.peopleDao

        dao.getByUid(whoUnfriend)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .flatMapCompletable { people ->
                dao.delete(people)
                    .mergeWith(Completable.fromAction {
                        mDatabase.conversationDao.deleteByParticipantId(
                            whoUnfriend,
                            mUser.uid
                        )
                    })
            }
            .onErrorComplete()
            .subscribe()
    }

    /*
    * Handle the conversation change option:
    * - Change group name
    * - Change thumbnail
    * - Add new member
    * - Remove member
    * - Member leave group
    * */
    private fun handleGroupChangeOption(payload: GroupOptionChangedPayload) {
        val message = payload.message
        val option = payload.option
        val member = payload.memberJson?.run { gson.fromJson(this, User::class.java) }

        shouldHaveConversationInCache(message.conversationId)
            .flatMap { optional ->
                if (optional.isPresent) {
                    val conversation = optional.get()
                    val group = conversation.group

                    when (option) {
                        GroupOptionChangedPayload.Option.NAME -> group.name = message.content
                        GroupOptionChangedPayload.Option.ADDED_MEMBER -> member?.let { conversation.participants.add(member) }
                        GroupOptionChangedPayload.Option.LEAVE_GROUP -> member?.let { conversation.participants.remove(member) }
                        GroupOptionChangedPayload.Option.THUMBNAIL -> group.thumbnail = message.content
                        GroupOptionChangedPayload.Option.REMOVE_MEMBER -> {
                            member?.let {
                                if (it.uid == mUser.uid) {
                                    // This is mean the current user has been removed by the admin of that conversation
                                    // So delete the conversation in the cache
                                } else {
                                    conversation.participants.remove(member)
                                }
                            }
                        }
                    }

                    conversation.chats = mutableListOf(message)

                    return@flatMap Completable.fromAction {
                        mDatabase.conversationDao.saveConversationList(
                            mutableListOf(conversation)
                        )
                    }.toSingleDefault(conversation)
                } else {
                    return@flatMap fetchConversation(message.conversationId).concatMap { c ->
                        Completable.fromAction {
                            mDatabase.conversationDao.saveConversationList(
                                mutableListOf(c)
                            )
                        }.toSingleDefault(c)
                    }
                }
            }
            .subscribe { c, _ ->
                AppDependencies.getDatabaseObserver().notifyMessageInserted(c.id, message.conversationId)
                AppDependencies.getDatabaseObserver().notifyConversationUpdated(c.id)
            }

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

    /*
    * Fetch remote user and then save to local datasource when received new friend request.
    * */
    private fun fetchUserDetail(userId: String): Single<User> =
        mUser.authorizeToken()
            .flatMapObservable { token -> mUserService.fetchUserFromRemote(token, userId) }
            .compose(RxHelper.parseResponseData())
            .singleOrError()
            .flatMap { user ->
                val people = with(user) {
                    People(uid, user.biographic, displayName, photoUrl, username, accessedDate)
                }

                return@flatMap mDatabase.peopleDao.save(people).toSingleDefault(user)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())

    companion object {
        val TAG: String = NotificationHandler::class.java.simpleName
    }
}
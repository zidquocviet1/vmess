package com.mqv.vmess.notification

import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.mqv.vmess.activity.service.CallNotificationService
import com.mqv.vmess.data.MyDatabase
import com.mqv.vmess.data.model.FriendNotification
import com.mqv.vmess.data.model.FriendNotificationType
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.network.exception.ResourceNotFoundException
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.model.Conversation
import com.mqv.vmess.network.model.User
import com.mqv.vmess.network.model.type.ConversationStatusType
import com.mqv.vmess.network.model.type.MessageStatus
import com.mqv.vmess.network.service.*
import com.mqv.vmess.notification.NotificationPayload.*
import com.mqv.vmess.notification.NotificationUtil.sendIncomingMessageNotification
import com.mqv.vmess.reactive.ReactiveExtension.authorizeToken
import com.mqv.vmess.reactive.RxHelper
import com.mqv.vmess.ui.data.People
import com.mqv.vmess.util.DateTimeHelper.toLocalDateTime
import com.mqv.vmess.util.DateTimeHelper.toLong
import com.mqv.vmess.util.Logging
import com.mqv.vmess.webrtc.WebRtcCallManager
import com.mqv.vmess.webrtc.WebRtcCandidate
import com.mqv.vmess.work.LifecycleUtil
import com.mqv.vmess.work.RefreshRemotePreKeyBundleWorkWrapper
import com.mqv.vmess.work.WorkDependency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.webrtc.IceCandidate
import java.time.LocalDateTime
import java.util.*

class NotificationHandler(
    private val mContext: Context,
    private val mDatabase: MyDatabase,
    private val mConversationService: ConversationService,
    private val mChatService: ChatService,
    private val mUserService: UserService,
    private val mFriendRequestService: FriendRequestService,
    private val mRtcService: RtcService,
    private val gson: Gson
) : NotificationEntry {
    private val mUser = FirebaseAuth.getInstance().currentUser!!
    private val mValidator: NotificationValidator =
        NotificationValidatorImpl(mDatabase.conversationOptionDao, mUser, mContext)

    override fun handleNotificationPayload(payload: NotificationPayload) {
        when (payload) {
            is AcceptedFriendPayload -> handleAcceptedFriend(payload)
            is FriendRequestPayload -> handleFriendRequest(payload)
            is IncomingMessagePayload -> handleMessage(payload)
            is StatusMessagePayload -> handleStatusMessage(payload)
            is ConversationGroupPayload -> handleConversationGroup(payload)
            is UnfriendPayload -> handleUnfriend(payload)
            is GroupOptionChangedPayload -> handleGroupChangeOption(payload)
            is CancelFriendRequestPayload -> handleCancelFriendRequest(payload)
            is WebRtcMessagePayload -> handleWebRtcMessage(payload)
            is RefreshPreKeyBundlePayload -> handleRefreshPreKeyBundle(payload)
        }
    }

    /*
    * Handle the firebase notification when the server send new message
    * Message from Group or User only
    * */
    private fun handleMessage(payload: IncomingMessagePayload) {
        val message = gson.fromJson(payload.messageJson, Chat::class.java)

        shouldHaveConversationInCache(message.conversationId)
            .map { optional ->
                if (optional.isPresent) {
                    Logging.debug(
                        TAG,
                        "Conversation appear in local database, check for duration of send time and current time"
                    )
                    // If message was sent in the past for a long time, check for this if is duration from larger than 5 minute.
                    // Then load the conversation from remote and then notify received
                    return@map Pair(
                        optional.get(),
                        message.timestamp >= LocalDateTime.now().minusMinutes(5L)
                    )
                } else {
                    Logging.debug(
                        TAG,
                        "Don't have any conversation with id: ${message.conversationId} in local database"
                    )

                    // Need to fetch conversation from remote
                    return@map Pair(null, false)
                }
            }
            .flatMap { pair ->
                val shouldShowNotification = pair.second

                Logging.debug(TAG, "Should show notification: $shouldShowNotification")

                if (shouldShowNotification) {
                    return@flatMap Single.just(pair.first!!)
                } else {
                    return@flatMap fetchConversation(message.conversationId)
                }
            }
            .flatMapCompletable { conversation ->
                notifyIncomingMessageNotification(
                    conversation,
                    message
                )
            }
            .andThen(
                mDatabase.chatDao.insert(message)
                    .andThen(Completable.fromAction {
                        AppDependencies.getDatabaseObserver()
                            .notifyMessageInserted(message.conversationId, message.id)
                    })
                    .andThen(notifyReceivedIncomingMessage(message.id))
            )
            .doOnError {
                Logging.debug(
                    TAG,
                    "Send incoming message not complete because: ${it.message}"
                )
            }
            .onErrorComplete()
            .subscribe { }
    }

    // Update the message status when participants push received or seen.
    private fun handleStatusMessage(payload: StatusMessagePayload) {
        val status = payload.status
        val whoSeen = payload.whoSeen
        val messageId = payload.messageId

        fetchCacheMessage(messageId).flatMapCompletable { message ->
            Logging.debug(
                TAG,
                "The message with id = $messageId represent in the local database, new status is ${status.name}"
            )

            message.status = status

            if (status == MessageStatus.SEEN) {
                Logging.debug(TAG, "Add new user to seen message id list")

                message.seenBy.add(whoSeen.get())
            }

            return@flatMapCompletable insertMessage(message)
        }.onErrorResumeWith {
            Logging.debug(
                TAG,
                "This message don't represent in local database, make the remote to fetch message"
            )

            fetchIncomingMessageRemote(payload.messageId)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .concatMapCompletable { insertMessage(it) }
                .onErrorComplete()

        }.subscribe {
            NotificationUtil.removeNotification(
                mContext,
                payload.messageId.hashCode()
            )
        }

    }

    private fun insertMessage(message: Chat): Completable =
        mDatabase.chatDao.insert(message)
            .subscribeOn(Schedulers.io())
            .andThen {
                Logging.debug(
                    TAG,
                    "Insert message to local database and then notify message updated to observers"
                )

                AppDependencies.getDatabaseObserver()
                    .notifyMessageUpdated(message.conversationId, message.id)
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
        val notificationId = payload.notificationId

        saveFriendNotification(
            notificationId,
            senderId = whoSent,
            createdAt = payload.timestamp.toLocalDateTime(),
            FriendNotificationType.REQUEST_FRIEND
        ).concatMap { id ->
            fetchUserDetail(whoSent).zipWith(Single.just(id)) { user, notificationId ->
                return@zipWith Pair(user, notificationId)
            }
        }
            .onErrorComplete()
            .subscribe { pair ->
                NotificationUtil.sendFriendRequestNotification(mContext, pair.first, pair.second)
                AppDependencies.getDatabaseObserver().notifyRequestFriend(whoSent)
            }
    }

    /*
    * Received when user's friend request is accepted by the others
    * */
    private fun handleAcceptedFriend(payload: AcceptedFriendPayload) {
        val whoConfirm = payload.whoAccepted
        val conversationId = payload.conversationId
        val notificationId = payload.notificationId

        saveFriendNotification(
            notificationId,
            senderId = whoConfirm,
            createdAt = payload.timestamp.toLocalDateTime(),
            FriendNotificationType.ACCEPTED_FRIEND
        ).concatMap { id ->
            fetchConversation(conversationId).zipWith(Single.just(id)) { c, notificationId ->
                return@zipWith Pair(c, notificationId)
            }
        }
            .subscribe { pair, _ ->
                val user = pair.first.participants.stream()
                    .filter { u -> u.uid == whoConfirm }
                    .findFirst()
                    .orElseThrow { ResourceNotFoundException() }
                with(user) {
                    val people =
                        People(uid, biographic, displayName, photoUrl, username, true, accessedDate)

                    mDatabase.peopleDao.save(people)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .onErrorComplete()
                        .subscribe()
                }

                NotificationUtil.sendAcceptedFriendRequestNotification(mContext, user, pair.second)
                AppDependencies.getDatabaseObserver().notifyConversationInserted(pair.first.id)
                AppDependencies.getDatabaseObserver().notifyConfirmFriend(whoConfirm)
            }
    }

    private fun saveFriendNotification(
        id: Long,
        senderId: String,
        createdAt: LocalDateTime,
        type: FriendNotificationType
    ): Single<Long> {
        val item = FriendNotification(
            id = id,
            senderId = senderId,
            type = type,
            hasRead = false,
            createdAt = createdAt
        )

        return mDatabase.friendNotificationDao
            .insertAndReturn(item)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
    }

    private fun handleUnfriend(payload: UnfriendPayload) {
        val whoUnfriend = payload.whoUnfriend
        val dao = mDatabase.peopleDao
        val notificationDao = mDatabase.friendNotificationDao
        val tempNotification = FriendNotification.TEMP

        val completable = Single.zip(
            notificationDao.fetchAcceptedNotificationByUserId(whoUnfriend)
                .onErrorReturnItem(tempNotification),
            notificationDao.fetchRequestNotificationByUserId(whoUnfriend)
                .onErrorReturnItem(tempNotification)
        ) { accepted, request ->
            val result = mutableListOf<FriendNotification>()

            if (accepted != tempNotification) {
                result.add(accepted)
            }

            if (request != tempNotification) {
                result.add(request)
            }

            return@zip result
        }
            .flatMapObservable { Observable.fromIterable(it) }
            .flatMapCompletable { notificationDao.delete(it) }
            .subscribeOn(Schedulers.io())

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
            .andThen(completable)
            .onErrorComplete()
            .subscribe {
                AppDependencies.getDatabaseObserver().notifyUnfriend(whoUnfriend)
            }
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

        if ((option == GroupOptionChangedPayload.Option.REMOVE_MEMBER) && (member?.uid == mUser.uid)) {
            // This is mean the current user has been removed by the admin of that conversation
            // So delete the conversation in the cache
            shouldHaveConversationInCache(message.conversationId)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMapCompletable { optional ->
                    if (optional.isPresent) {
                        return@flatMapCompletable mDatabase.conversationDao.delete(optional.get())
                    } else {
                        return@flatMapCompletable Completable.complete()
                    }
                }
                .onErrorComplete()
                .subscribe()
        } else {
            shouldHaveConversationInCache(message.conversationId)
                .flatMap { optional ->
                    if (optional.isPresent) {
                        val conversation = optional.get()
                        val group = conversation.group

                        when (option) {
                            GroupOptionChangedPayload.Option.NAME -> group.name = message.content
                            GroupOptionChangedPayload.Option.ADDED_MEMBER -> conversation.participants.add(
                                member!!
                            )
                            GroupOptionChangedPayload.Option.LEAVE_GROUP -> conversation.participants.remove(
                                member!!
                            )
                            GroupOptionChangedPayload.Option.THUMBNAIL -> group.thumbnail =
                                message.content
                            GroupOptionChangedPayload.Option.REMOVE_MEMBER -> conversation.participants.remove(
                                member!!
                            )
                        }

                        conversation.chats = mutableListOf(message)

                        return@flatMap Completable.fromAction {
                            mDatabase.conversationDao.saveConversationList(
                                mutableListOf(conversation)
                            )
                        }.andThen(Single.just(conversation))
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
                    AppDependencies.getDatabaseObserver()
                        .notifyMessageInserted(c.id, message.id)
                    AppDependencies.getDatabaseObserver().notifyConversationUpdated(c.id)
                }
        }
    }

    private fun fetchCacheMessage(messageId: String) =
        mDatabase.chatDao.findById(messageId)
            .subscribeOn(Schedulers.io())

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
                return@flatMap mUser.authorizeToken()
                    .flatMapObservable { token -> mFriendRequestService.isFriend(token, userId) }
                    .map { isFriend ->
                        return@map with(user) {
                            People(
                                uid,
                                user.biographic,
                                displayName,
                                photoUrl,
                                username,
                                isFriend,
                                accessedDate
                            )
                        }
                    }
                    .flatMapCompletable { people ->
                        return@flatMapCompletable mDatabase.peopleDao.save(
                            people
                        )
                    }
                    .toSingleDefault(user)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())

    fun notifyIncomingMessageNotification(
        conversation: Conversation,
        message: Chat
    ): Completable =
        mValidator.shouldShowNotification(conversation, message)
            .flatMapCompletable { isShow ->
                Completable.fromAction {
                    if (isShow) {
                        val sender = conversation.participants
                            .stream()
                            .filter { u: User -> u.uid == message.senderId }
                            .findFirst()
                            .orElseThrow { ResourceNotFoundException() }
                        val metadata =
                            MessageNotificationMetadata(sender, conversation, message)
                        sendIncomingMessageNotification(
                            mContext,
                            metadata,
                            conversation.encrypted
                        )
                    }
                }
            }

    private fun handleCancelFriendRequest(payload: CancelFriendRequestPayload) {
        AppDependencies.getDatabaseObserver().notifyCancelRequest(payload.whoCancel)

        mDatabase.friendNotificationDao.fetchRequestNotificationByUserId(payload.whoCancel)
            .flatMap { mDatabase.friendNotificationDao.delete(it).toSingleDefault(it.id!!) }
            .subscribeOn(Schedulers.io())
            .subscribe { id ->
                NotificationUtil.removeNotification(mContext, id.toInt())
            }
    }

    private fun handleWebRtcMessage(payload: WebRtcMessagePayload) {
        when (payload.type) {
            WebRtcMessagePayload.WebRtcDataType.START_CALL -> {
                if (LifecycleUtil.isAppForeground() && !AppDependencies.getWebRtcCallManager().shouldShowNotification) {
                    Logging.debug(
                        TAG,
                        "The user is idle right now. Call will be ejected, send notification to eject that call to caller"
                    )
                    mUser.authorizeToken().flatMapObservable { token ->
                        mRtcService.notifyBusy(token, payload.caller)
                    }.subscribeOn(Schedulers.io())
                        .onErrorComplete()
                        .subscribe()
                } else {
                    if (payload.timestamp >= LocalDateTime.now().plusSeconds(50).toLong()) {
                        val intent = Intent(mContext, CallNotificationService::class.java).apply {
                            putExtra(CallNotificationService.EXTRA_CALLER, payload.caller)
                            putExtra(CallNotificationService.EXTRA_VIDEO, payload.isVideoCall)
                        }
                        mContext.startForegroundService(intent)
                    }
                }
            }
            WebRtcMessagePayload.WebRtcDataType.DENY_CALL -> {
                AppDependencies.getDatabaseObserver().notifyRtcDenyCall()
            }
            WebRtcMessagePayload.WebRtcDataType.IS_IN_CALL -> {
                AppDependencies.getDatabaseObserver().notifyRtcUserIsInCall()
            }
            WebRtcMessagePayload.WebRtcDataType.OFFER -> {
                AppDependencies.getDatabaseObserver().notifyRtcOffer(payload.data)
            }
            WebRtcMessagePayload.WebRtcDataType.ANSWER -> {
                AppDependencies.getDatabaseObserver().notifyRtcAnswer(payload.data)
            }
            WebRtcMessagePayload.WebRtcDataType.CANDIDATE -> {
                val webRtcCandidate = gson.fromJson(payload.data, WebRtcCandidate::class.java)
                val iceCandidate = with(webRtcCandidate) {
                    IceCandidate(id, index, sdp)
                }
                AppDependencies.getDatabaseObserver().notifyIceCandidate(iceCandidate)
            }
            else -> {
                val callId = (payload.caller + mUser.uid).hashCode()
                val reserveCallId = (mUser.uid + payload.caller).hashCode()

                if (callId == CallNotificationService.callId || reserveCallId == CallNotificationService.callId) {
                    mContext.stopService(Intent(mContext, CallNotificationService::class.java))
                }

                if ((payload.caller + mUser.uid) == WebRtcCallManager.callId || (mUser.uid + payload.caller) == WebRtcCallManager.callId) {
                    AppDependencies.getDatabaseObserver().notifyRtcSessionClose()
                }
            }
        }
    }

    private fun handleRefreshPreKeyBundle(payload: RefreshPreKeyBundlePayload) {
        val remoteAddress = payload.remoteAddress

        if (remoteAddress.isNullOrEmpty()) {
            throw IllegalArgumentException("Can't specify the user to refresh pre key bundle")
        }

        WorkDependency.enqueue(RefreshRemotePreKeyBundleWorkWrapper(mContext, remoteAddress))
    }

    companion object {
        val TAG: String = NotificationHandler::class.java.simpleName
    }
}
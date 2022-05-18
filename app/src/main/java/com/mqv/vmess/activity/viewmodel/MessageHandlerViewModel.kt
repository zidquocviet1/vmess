package com.mqv.vmess.activity.viewmodel

import android.app.Application
import android.content.Context
import android.util.Pair
import androidx.lifecycle.AndroidViewModel
import androidx.work.Data
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.mqv.vmess.R
import com.mqv.vmess.data.model.ConversationNotificationOption
import com.mqv.vmess.data.model.ConversationNotificationOption.Companion.fromConversationOption
import com.mqv.vmess.data.repository.ChatRepository
import com.mqv.vmess.data.repository.ConversationRepository
import com.mqv.vmess.data.repository.FriendRequestRepository
import com.mqv.vmess.data.repository.PeopleRepository
import com.mqv.vmess.data.result.Result
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.network.ApiResponse
import com.mqv.vmess.network.exception.PermissionDeniedException
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.model.Conversation
import com.mqv.vmess.network.model.ConversationOption
import com.mqv.vmess.network.model.User
import com.mqv.vmess.network.model.type.ConversationStatusType
import com.mqv.vmess.network.model.type.MessageStatus
import com.mqv.vmess.reactive.RxHelper
import com.mqv.vmess.reactive.RxHelper.applyObservableSchedulers
import com.mqv.vmess.reactive.RxHelper.applySingleSchedulers
import com.mqv.vmess.reactive.RxHelper.parseResponseData
import com.mqv.vmess.ui.ConversationOptionHandler.Companion.addMemberObservable
import com.mqv.vmess.ui.data.People
import com.mqv.vmess.util.Event
import com.mqv.vmess.util.FileProviderUtil.compressFileFuture
import com.mqv.vmess.util.Logging
import com.mqv.vmess.util.NetworkStatus
import com.mqv.vmess.work.PushMessageAcknowledgeWorkWrapper
import com.mqv.vmess.work.WorkDependency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.net.ConnectException
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.function.Consumer

private val TAG = MessageHandlerViewModel::class.java.simpleName

open class MessageHandlerViewModel(
    application: Application,
    open val conversationRepository: ConversationRepository,
    open val chatRepository: ChatRepository,
    open val peopleRepository: PeopleRepository,
    open val friendRequestRepository: FriendRequestRepository
) : AndroidViewModel(application) {
    private val cd = CompositeDisposable()
    protected val mUser = FirebaseAuth.getInstance().currentUser!!

    fun markAsUnread(conversation: Conversation) {
        chatRepository.findLastMessage(conversation.id)
            .map { old: Chat ->
                old.status = MessageStatus.RECEIVED
                old.seenBy
                    .remove(mUser.uid)
                old
            }
            .flatMapCompletable { chat: Chat? ->
                chatRepository.saveCached(
                    chat
                )
            }
            .subscribeOn(Schedulers.io())
            .onErrorComplete()
            .subscribe {
                AppDependencies.getDatabaseObserver().notifyConversationUpdated(conversation.id)
            }
    }

    fun seenUnreadMessageInConversation(conversationId: String) {
        chatRepository.fetchIncomingByConversation(conversationId)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .flattenAsObservable { list -> list }
            .map { incomingMessage: Chat ->
                if (!incomingMessage.seenBy.contains(mUser.uid)) {
                    incomingMessage.status = MessageStatus.SEEN
                    incomingMessage.seenBy.add(mUser.uid)

                    chatRepository.saveCached(incomingMessage)
                        .subscribeOn(Schedulers.io())
                        .subscribe()

                    return@map incomingMessage.id
                } else {
                    return@map ""
                }
            }
            .filter { id: String -> id != "" }
            .toList()
            .subscribe { list, _ ->
                sendSeenMessage(
                    getApplication<Application>().applicationContext,
                    list,
                    conversationId
                )
            }
    }

    fun seenUnreadMessageByTimestamp(conversationId: String, timestamp: LocalDateTime) {
        chatRepository.fetchIncomingByConversation(conversationId)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .flattenAsObservable { list -> list }
            .map { incomingMessage ->
                if (!incomingMessage.seenBy.contains(mUser.uid) && (incomingMessage.timestamp <= timestamp)) {
                    incomingMessage.status = MessageStatus.SEEN
                    incomingMessage.seenBy.add(mUser.uid)

                    chatRepository.saveCached(incomingMessage)
                        .subscribeOn(Schedulers.io())
                        .subscribe()

                    return@map incomingMessage.id
                } else {
                    return@map ""
                }
            }
            .filter { id -> id != "" }
            .toList()
            .subscribe { list, _ ->
                sendSeenMessage(
                    getApplication<Application>().applicationContext,
                    list,
                    conversationId
                )
            }
    }

    private fun sendSeenMessage(context: Context, ids: List<String>, conversationId: String) {
        if (ids.isNotEmpty()) {
            val data = Data.Builder()
                .putStringArray(
                    PushMessageAcknowledgeWorkWrapper.EXTRA_LIST_MESSAGE_ID,
                    ids.toTypedArray()
                )
                .putBoolean(PushMessageAcknowledgeWorkWrapper.EXTRA_MARK_AS_READ, true)
                .build()
            WorkDependency.enqueue(PushMessageAcknowledgeWorkWrapper(context, data))
            AppDependencies.getDatabaseObserver().notifyConversationUpdated(conversationId)
        } else {
            Logging.show("No need to push seen messages, because the list unread message is empty")
        }
    }

    fun setupConversationParticipants(conversation: Conversation, onResult: Consumer<List<User>>) {
        if (conversation.group != null) {
            cd.add(processListUser(chatRepository.fetchSenderIdFromChat(conversation.id), onResult))
        }
    }

    fun setupConversationParticipants(onResult: Consumer<List<User>>) {
        cd.add(processListUser(chatRepository.fetchSenderIdFromChat(), onResult))
    }

    private fun processListUser(
        singleListUser: Single<List<String>>,
        onResult: Consumer<List<User>>
    ): Disposable {
        return singleListUser.flatMapObservable { source ->
            Observable.fromIterable(
                source
            )
        }
            .map { uid ->
                Pair.create(
                    peopleRepository.isUserPresent(uid).blockingGet(),
                    uid
                )
            }
            .flatMap { pair ->
                if (pair.first) {
                    return@flatMap peopleRepository.getCachedByUid(pair.second).toObservable()
                } else {
                    return@flatMap peopleRepository.getConnectPeopleByUid(pair.second)
                        .compose(parseResponseData<People>())
                }
            }
            .flatMapSingle { people ->
                if (people.friend == null) {
                    return@flatMapSingle friendRequestRepository.isFriend(people.uid)
                        .flatMapCompletable { isFriend: Boolean? ->
                            people.friend = isFriend
                            peopleRepository.save(people)
                        }.toSingleDefault(people)
                }
                return@flatMapSingle Single.just(people)
            }
            .map { p ->
                User.Builder()
                    .setUid(p.uid)
                    .setBiographic(p.biographic)
                    .setPhotoUrl(p.photoUrl)
                    .setDisplayName(p.displayName)
                    .create()
            }
            .toList()
            .compose(applySingleSchedulers())
            .subscribe(
                { value ->
                    onResult.accept(value)
                }
            ) { t ->
                Logging.show("Can not fetch user left group because: ${t.message}")
            }
    }

    open fun muteNotification(conversationId: String, until: Long) {
        val currentMillis =
            LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().epochSecond
        val muteUntil = if (Long.MAX_VALUE == until) until else currentMillis + until
        val option = listOf(
            ConversationNotificationOption(
                null,
                conversationId,
                muteUntil,
                LocalDateTime.now()
            )
        )
        val disposable = conversationRepository.mute(conversationId, muteUntil)
            .startWith(conversationRepository.insertNotificationOption(option))
            .compose(applyObservableSchedulers())
            .compose(parseResponseData())
            .map { data: ConversationOption? ->
                listOf(
                    fromConversationOption(
                        data!!
                    )
                )
            }
            .flatMapCompletable { result ->
                conversationRepository.insertNotificationOption(
                    result
                )
            }
            .onErrorComplete()
            .subscribe()
        cd.add(disposable)
    }

    fun unMuteNotification(conversationId: String) {
        //noinspection ResultOfMethodCallIgnored
        conversationRepository.umute(conversationId)
            .startWith(conversationRepository.deleteNotificationOption(conversationId))
            .compose(applyObservableSchedulers())
            .onErrorComplete()
            .subscribe {
                Logging.debug(
                    TAG,
                    "UnMute notification successfully, conversationId: $conversationId"
                )
            }
    }

    private fun handleGroupChangeState(
        observable: Observable<ApiResponse<Conversation>>,
        onLoading: Runnable,
        onResult: Consumer<Conversation>,
        onFailure: Consumer<Throwable>,
        onErrorToast: Consumer<Event<Int>>
    ) {
        addDisposable(
            observable.startWith(Completable.fromAction { onLoading.run() })
                .compose(parseResponseData())
                .flatMapSingle { c: Conversation ->
                    c.status = ConversationStatusType.INBOX
                    onResult.accept(c)
                    conversationRepository.save(c).toSingleDefault(c.chats)
                }
                .compose(applyObservableSchedulers())
                .subscribe({ list ->
                    if (list.size == 1) {
                        val message = list[0]
                        AppDependencies.getDatabaseObserver()
                            .notifyConversationUpdated(message.conversationId)
                        AppDependencies.getDatabaseObserver()
                            .notifyMessageInserted(message.conversationId, message.id)
                    }
                }) { t ->
                    when (t) {
                        is ConnectException -> onErrorToast.accept(Event(R.string.error_connect_server_fail))
                        is FirebaseNetworkException -> onErrorToast.accept(Event(R.string.error_network_connection))
                        is PermissionDeniedException -> onErrorToast.accept(Event(R.string.msg_user_dont_allow_added))
                        else -> onErrorToast.accept(Event(R.string.error_unknown))
                    }
                    onFailure.accept(t)
                }
        )
    }

    fun changeGroupThumbnail(
        file: File,
        conversationId: String,
        onLoading: Runnable,
        onResult: Consumer<Conversation>,
        onFailure: Consumer<Throwable>,
        onErrorToast: Consumer<Event<Int>>
    ) {
        handleGroupChangeState(
            Observable.fromFuture(
                compressFileFuture(
                    getApplication<Application>().applicationContext,
                    file
                )
            ).flatMap { compress ->
                conversationRepository.changeConversationGroupThumbnail(
                    conversationId,
                    compress
                )
            },
            onLoading,
            onResult,
            onFailure,
            onErrorToast
        )
    }

    fun changeGroupName(
        groupName: String,
        conversationId: String,
        onLoading: Runnable,
        onResult: Consumer<Conversation>,
        onFailure: Consumer<Throwable>,
        onErrorToast: Consumer<Event<Int>>
    ) {
        handleGroupChangeState(
            conversationRepository.changeConversationGroupName(
                conversationId,
                groupName
            ),
            onLoading,
            onResult,
            onFailure,
            onErrorToast
        )
    }

    fun addMember(
        conversationId: String,
        memberIds: List<String>,
        onLoading: Runnable,
        onResult: Consumer<Conversation>,
        onFailure: Consumer<Throwable>,
        onErrorToast: Consumer<Event<Int>>
    ) {
        addDisposable(
            Observable.fromIterable(memberIds)
                .flatMap { memberId ->
                    addMemberObservable(
                        conversationRepository,
                        conversationId, memberId
                    )
                }
                .compose(applyObservableSchedulers())
                .subscribe(
                    { result ->
//                        oneTimeLoadingResult.postValue(
//                            Pair.create(
//                                result.status == NetworkStatus.LOADING,
//                                R.string.action_loading
//                            )
//                        )
                    }
                ) { t ->
//                    oneTimeLoadingResult.postValue(
//                        Pair.create(
//                            false,
//                            R.string.action_loading
//                        )
//                    )
                    when (t) {
                        is ConnectException -> onErrorToast.accept(Event(R.string.error_connect_server_fail))
                        is FirebaseNetworkException -> onErrorToast.accept(Event(R.string.error_network_connection))
                        is PermissionDeniedException -> onErrorToast.accept(Event(R.string.msg_user_dont_allow_added))
                        else -> onErrorToast.accept(Event(R.string.msg_permission_denied))
                    }
                }
        )
    }

    fun addDisposable(disposable: Disposable) {
        cd.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()

        cd.dispose()
    }
}
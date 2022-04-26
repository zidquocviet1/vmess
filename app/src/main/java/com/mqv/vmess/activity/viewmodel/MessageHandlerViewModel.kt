package com.mqv.vmess.activity.viewmodel

import android.app.Application
import android.content.Context
import android.util.Pair
import androidx.lifecycle.AndroidViewModel
import androidx.work.Data
import com.google.firebase.auth.FirebaseAuth
import com.mqv.vmess.data.repository.ChatRepository
import com.mqv.vmess.data.repository.FriendRequestRepository
import com.mqv.vmess.data.repository.PeopleRepository
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.model.Conversation
import com.mqv.vmess.network.model.User
import com.mqv.vmess.network.model.type.MessageStatus
import com.mqv.vmess.reactive.RxHelper.applySingleSchedulers
import com.mqv.vmess.reactive.RxHelper.parseResponseData
import com.mqv.vmess.ui.data.People
import com.mqv.vmess.util.Logging
import com.mqv.vmess.work.PushMessageAcknowledgeWorkWrapper
import com.mqv.vmess.work.WorkDependency
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.function.Consumer

open class MessageHandlerViewModel(
    application: Application,
    val chatRepository: ChatRepository,
    val peopleRepository: PeopleRepository,
    val friendRequestRepository: FriendRequestRepository
) : AndroidViewModel(application) {
    private val cd = CompositeDisposable()
    private val mUser = FirebaseAuth.getInstance().currentUser!!

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

    fun addDisposable(disposable: Disposable) {
        cd.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()

        cd.dispose()
    }
}
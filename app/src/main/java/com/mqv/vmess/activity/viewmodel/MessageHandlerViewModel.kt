package com.mqv.vmess.activity.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.work.Data
import com.google.firebase.auth.FirebaseAuth
import com.mqv.vmess.data.repository.ChatRepository
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.model.Conversation
import com.mqv.vmess.network.model.type.MessageStatus
import com.mqv.vmess.util.Logging
import com.mqv.vmess.work.PushMessageAcknowledgeWorkWrapper
import com.mqv.vmess.work.WorkDependency
import io.reactivex.rxjava3.schedulers.Schedulers

open class MessageHandlerViewModel(application: Application, val chatRepository: ChatRepository) :
    AndroidViewModel(application) {
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
}
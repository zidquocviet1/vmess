package com.mqv.vmess.activity.br

import android.content.Context
import android.content.Intent
import androidx.work.Data
import com.google.firebase.auth.FirebaseAuth
import com.mqv.vmess.data.repository.impl.ChatRepositoryImpl
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.model.type.MessageStatus
import com.mqv.vmess.notification.NotificationUtil
import com.mqv.vmess.util.Logging
import com.mqv.vmess.work.PushMessageAcknowledgeWorkWrapper
import com.mqv.vmess.work.WorkDependency
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject

@AndroidEntryPoint
class MarkReadReceiver : HiltBroadcastReceiver() {
    @Inject
    lateinit var chatRepository: ChatRepositoryImpl

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        val currentUser = FirebaseAuth.getInstance().currentUser!!

        conversationId?.let {
            NotificationUtil.removeNotification(context, it.hashCode())

            seenUnreadMessageInConversation(context, it, currentUser.uid)
        }
    }

    private fun seenUnreadMessageInConversation(context: Context, conversationId: String, userId: String) {
        chatRepository.fetchIncomingByConversation(conversationId)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .flattenAsObservable { list -> list }
            .map { incomingMessage: Chat ->
                if (!incomingMessage.seenBy.contains(userId)) {
                    incomingMessage.status = MessageStatus.SEEN
                    incomingMessage.seenBy.add(userId)

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
                    context,
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
                .putInt(PushMessageAcknowledgeWorkWrapper.EXTRA_TYPE, PushMessageAcknowledgeWorkWrapper.EXTRA_SEEN)
                .build()
            WorkDependency.enqueue(PushMessageAcknowledgeWorkWrapper(context, data))
            AppDependencies.getDatabaseObserver().notifyConversationUpdated(conversationId)
        } else {
            Logging.show("No need to push seen messages, because the list unread message is empty")
        }
    }

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversation_id"
    }
}
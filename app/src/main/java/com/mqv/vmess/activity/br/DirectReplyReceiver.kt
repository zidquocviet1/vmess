package com.mqv.vmess.activity.br

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.RemoteInput
import androidx.room.rxjava3.EmptyResultSetException
import androidx.work.Data
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.mqv.vmess.data.model.LocalPlaintextContentModel
import com.mqv.vmess.data.repository.impl.ChatRepositoryImpl
import com.mqv.vmess.data.repository.impl.ConversationRepositoryImpl
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.model.Chat.Share
import com.mqv.vmess.network.model.type.MessageStatus
import com.mqv.vmess.network.model.type.MessageType
import com.mqv.vmess.notification.MessageNotificationMetadata
import com.mqv.vmess.notification.NotificationUtil
import com.mqv.vmess.notification.toUser
import com.mqv.vmess.reactive.RxHelper.applyCompleteSchedulers
import com.mqv.vmess.ui.validator.InputValidator.getLinkFromText
import com.mqv.vmess.ui.validator.InputValidator.isLinkMessage
import com.mqv.vmess.util.Logging
import com.mqv.vmess.work.PushMessageAcknowledgeWorkWrapper
import com.mqv.vmess.work.SendMessageWorkWrapper
import com.mqv.vmess.work.WorkDependency
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class DirectReplyReceiver : HiltBroadcastReceiver() {
    @Inject
    lateinit var conversationRepository: ConversationRepositoryImpl

    @Inject
    lateinit var chatRepository: ChatRepositoryImpl

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        Logging.debug(TAG, "Receive request to handle directly reply")

        val currentUser = FirebaseAuth.getInstance().currentUser!!
        val bundle = RemoteInput.getResultsFromIntent(intent)
        val conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        val isEncrypted = intent.getBooleanExtra(EXTRA_IS_ENCRYPTED, false)
        val plainText = bundle.getCharSequence(KEY_TEXT_CONTENT).toString()
        val messageId = UUID.randomUUID().toString()

        if (conversationId.isNullOrEmpty()) {
            Logging.debug(TAG, "Conversation id is not valid, stop reply message")
            return
        }

        Logging.debug(
            TAG,
            "Reply plaintext message, data=[SenderId: ${currentUser.uid}, Content: $plainText, ConversationId: $conversationId]"
        )

        conversationRepository.isExists(conversationId)
            .flatMapCompletable { isExists ->
                val chat = Chat.builder()
                    .setId(messageId)
                    .setConversationId(conversationId)
                    .setSenderId(currentUser.uid)
                    .setType(MessageType.GENERIC)
                    .setContent(plainText)
                    .create()

                if (isLinkMessage(plainText)) {
                    val link = getLinkFromText(plainText)
                    val share = Share(link)
                    chat.share = share
                    chat.type = MessageType.SHARE
                }

                if (isExists) Completable.fromAction {
                    seenUnreadMessageInConversation(
                        context,
                        conversationId,
                        currentUser.uid
                    )
                }.andThen(
                    if (isEncrypted) {
                        val model =
                            LocalPlaintextContentModel(chat.id, conversationId, chat.content)
                        chatRepository.saveCached(chat)
                            .andThen(
                                conversationRepository.saveOutgoingEncryptedMessageContent(
                                    model
                                )
                            )
                    } else {
                        chatRepository.saveCached(chat)
                    }
                ) else Completable.error(
                    EmptyResultSetException("empty")
                )
            }
            .compose(applyCompleteSchedulers())
            .andThen(Completable.fromAction {
                Logging.debug(
                    TAG,
                    "Notify message inserted and conversation updated to all observers"
                )

                AppDependencies.getDatabaseObserver()
                    .notifyMessageInserted(conversationId, messageId)
                AppDependencies.getDatabaseObserver()
                    .notifyConversationUpdated(conversationId)
            })
            .subscribe({
                Logging.debug(TAG, "Create worker to send message using websocket or http")

                val input = Data.Builder()
                    .putBoolean(SendMessageWorkWrapper.EXTRA_IS_ENCRYPTED, isEncrypted)
                    .putString(SendMessageWorkWrapper.EXTRA_MESSAGE_ID, messageId)
                    .build()
                WorkDependency.enqueue(SendMessageWorkWrapper(context, input))
            }, { t ->
                Logging.debug(TAG, "Can't reply message because: ${t.message}")

                if (t is EmptyResultSetException) {
                    Toast.makeText(context, "Send message not complete", Toast.LENGTH_SHORT)
                        .show()
                }
            })

        notifyMessageToActiveNotification(context, conversationId, currentUser, messageId)
    }

    private fun notifyMessageToActiveNotification(
        context: Context,
        conversationId: String,
        user: FirebaseUser,
        message: String
    ) {
        conversationRepository.fetchCachedById(conversationId)
            .zipWith(chatRepository.fetchCached(message)) { conversation, chat ->
                MessageNotificationMetadata(user.toUser(), conversation, chat)
            }
            .zipWith(conversationRepository.isEncryptionConversation(conversationId)) { metadata, isEncrypted ->
                Pair(metadata, isEncrypted)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe { pair, _ ->
                val messageMetadata = pair.first
                val isEncrypted = pair.second

                NotificationUtil.sendIncomingMessageNotification(
                    context,
                    messageMetadata,
                    isEncrypted
                )
            }
    }

    private fun seenUnreadMessageInConversation(
        context: Context,
        conversationId: String,
        userId: String
    ) {
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
        private val TAG = DirectReplyReceiver::class.java.simpleName

        const val EXTRA_CONVERSATION_ID = "conversationId"
        const val EXTRA_IS_ENCRYPTED = "isEncrypted"
        const val KEY_TEXT_CONTENT = "key_content"
    }
}
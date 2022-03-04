package com.mqv.realtimechatapplication.message

import android.content.Context
import androidx.work.Data
import com.mqv.realtimechatapplication.data.dao.ChatDao
import com.mqv.realtimechatapplication.data.dao.PendingMessageDao
import com.mqv.realtimechatapplication.data.dao.SeenMessageDao
import com.mqv.realtimechatapplication.data.model.PendingMessage
import com.mqv.realtimechatapplication.data.model.SeenMessage
import com.mqv.realtimechatapplication.dependencies.AppDependencies
import com.mqv.realtimechatapplication.network.NetworkConstraint
import com.mqv.realtimechatapplication.network.model.Chat
import com.mqv.realtimechatapplication.util.Logging
import com.mqv.realtimechatapplication.work.PushMessageAcknowledgeWorkWrapper
import com.mqv.realtimechatapplication.work.SendMessageWorkWrapper
import com.mqv.realtimechatapplication.work.WorkDependency
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

/*
* The entry point class to process the message was sent fail or the message was seen fail
* Because the network poorly or have any problem with the server connection.
* */
private val TAG = MessageSenderProcessor::class.java.simpleName

class MessageSenderProcessor(
    private val context: Context,
    private val pendingMessageDao: PendingMessageDao,
    private val messageDao: ChatDao,
    private val seenMessageDao: SeenMessageDao
) {
    init {
        retrySendMessages()
    }

    private fun retrySendMessages() {
        pendingMessageDao.getAll()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .map { messages ->
                messages.stream().sorted { o1, o2 ->
                    o1.timestamp.compareTo(o2.timestamp)
                }.collect(Collectors.toList())
            }
            .toObservable()
            .concatMap { orderedMessages ->
                Observable.fromIterable(orderedMessages).delay(300, TimeUnit.MILLISECONDS)
            }
            .concatMapSingle { messageDao.findById(it.id) }
            .onErrorComplete()
            .subscribe {
                Logging.info(TAG, "Start sending failure messages: ${LocalDateTime.now()}")
                sendMessage(it)
            }
    }

    fun shouldRetrySeenMessages() {
        val listMessage = seenMessageDao.getAll()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .blockingGet()
        if (listMessage.isEmpty()) {
            Logging.debug(TAG, "Have no any failure message need to retry seen.")
        } else {
            if (NetworkConstraint.isMet(context)) {
                // Get the messages need to retry in the websocket monitor
                val retrySeenMessages = AppDependencies.getWebSocket().seenMessagesNeedToPush
                val messageIds = listMessage.stream()
                    .map { it.id }
                    .collect(Collectors.toList())

                // Remove all the list message id in the websocket monitor.
                // Because the monitor will automatically send messages when reconnected
                messageIds.removeAll(retrySeenMessages)

                Logging.debug(TAG, "Size of the seen failure message = ${messageIds.size}")

                val data = Data.Builder()
                    .putStringArray(
                        PushMessageAcknowledgeWorkWrapper.EXTRA_LIST_MESSAGE_ID,
                        messageIds.toTypedArray()
                    )
                    .putBoolean(PushMessageAcknowledgeWorkWrapper.EXTRA_MARK_AS_READ, true)
                    .build()
                // TODO: should sent seen message using HTTP even websocket in when the user in background
                WorkDependency.enqueue(PushMessageAcknowledgeWorkWrapper(context, data))
            }
        }
    }

    private fun sendMessage(message: Chat) {
        val input = Data.Builder()
            .putString(SendMessageWorkWrapper.EXTRA_MESSAGE_ID, message.id)
            .putString(SendMessageWorkWrapper.EXTRA_SENDER_ID, message.senderId)
            .putString(SendMessageWorkWrapper.EXTRA_CONTENT, message.content)
            .putString(SendMessageWorkWrapper.EXTRA_CONVERSATION_ID, message.conversationId)
            .putString(SendMessageWorkWrapper.EXTRA_MESSAGE_TYPE, message.type.name)
            .build()

        val worker = SendMessageWorkWrapper(context, input)

        WorkDependency.enqueue(worker)
    }

    @JvmOverloads
    fun insertPendingMessage(messageId: String, timestamp: Long, workId: String = "") {
        val pendingMessage = PendingMessage(messageId, timestamp, workId)

        pendingMessageDao.insert(pendingMessage)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .doOnComplete {
                Logging.info(TAG, "Insert pending message complete")
            }
            .subscribe()
    }

    fun deletePendingMessage(messageId: String): Completable {
        return pendingMessageDao.delete(messageId)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .doOnComplete {
                Logging.info(TAG, "Delete pending message complete")
            }
    }

    fun insertSeenMessage(messageId: String, timestamp: Long) {
        val seenMessage = SeenMessage(messageId, timestamp)

        seenMessageDao.insert(seenMessage)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .doOnComplete {
                Logging.info(TAG, "Insert seen message complete")
            }
            .subscribe()
    }

    fun deleteSeenMessage(messageId: String): Completable {
        return seenMessageDao.delete(messageId)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .doOnComplete {
                Logging.info(TAG, "Delete seen message complete")
            }
    }
}
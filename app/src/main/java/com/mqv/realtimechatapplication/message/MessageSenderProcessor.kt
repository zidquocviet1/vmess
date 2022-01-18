package com.mqv.realtimechatapplication.message

import android.content.Context
import androidx.work.Data
import com.mqv.realtimechatapplication.data.dao.ChatDao
import com.mqv.realtimechatapplication.data.dao.PendingMessageDao
import com.mqv.realtimechatapplication.data.model.PendingMessage
import com.mqv.realtimechatapplication.network.model.Chat
import com.mqv.realtimechatapplication.util.Logging
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
    private val messageDao: ChatDao
) {
    init {
        retrySendMessages()
        retryPostSeenMessages()
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

    private fun retryPostSeenMessages() {

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
}
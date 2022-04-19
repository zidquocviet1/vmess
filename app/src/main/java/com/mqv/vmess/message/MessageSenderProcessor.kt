package com.mqv.vmess.message

import android.content.Context
import androidx.work.Data
import com.google.firebase.auth.FirebaseAuth
import com.mqv.vmess.data.dao.ChatDao
import com.mqv.vmess.data.dao.PendingMessageDao
import com.mqv.vmess.data.dao.SeenMessageDao
import com.mqv.vmess.data.model.PendingMessage
import com.mqv.vmess.data.model.SeenMessage
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.network.NetworkConstraint
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.network.service.ChatService
import com.mqv.vmess.reactive.ReactiveExtension.authorizeToken
import com.mqv.vmess.reactive.RxHelper
import com.mqv.vmess.util.Logging
import com.mqv.vmess.work.SendMessageWorkWrapper
import com.mqv.vmess.work.WorkDependency
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
    private val seenMessageDao: SeenMessageDao,
    private val messageService: ChatService
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

                val mUser = FirebaseAuth.getInstance().currentUser

                mUser?.let {
                    mUser.authorizeToken()
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .flatMap { token ->
                            Observable.fromIterable(messageIds)
                                .flatMap { id -> messageDao.findById(id).toObservable() }
                                .flatMap { chat -> messageService.seenMessage(token, chat) }
                                .compose(RxHelper.parseResponseData())
                                .singleOrError()
                        }
                        .onErrorComplete()
                        .subscribe()
                }
            }
        }
    }

    private fun sendMessage(message: Chat) {
        val input = Data.Builder()
            .putString(SendMessageWorkWrapper.EXTRA_MESSAGE_ID, message.id)
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
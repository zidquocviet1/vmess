package com.mqv.realtimechatapplication.network.websocket

import com.mqv.realtimechatapplication.dependencies.AppDependencies
import com.mqv.realtimechatapplication.util.Logging
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/*
* The monitor is responsible for sending heartbeat messages to prevent timeouts.
* */
private val TAG = WebSocketHeartbeatMonitor::class.java.simpleName
private const val PING_PONG_SENT_TIME_INTERVAL = 50L
private const val MAX_MESSAGE_ALLOWED_MISSING = 3L

class WebSocketHeartbeatMonitor(private val timer: Timer) : HeartbeatMonitor {
    private val pendingMessage = HashSet<WebSocketRequestMessage>()
    private val executor = Executors.newSingleThreadExecutor()
    private var webSocket: WebSocketClient? = null
    private var sender: PingPongSender? = null
    private var isPingPongNecessary = false
    private var lastTimeReceivePong = 0L

    fun monitor(webSocket: WebSocketClient) {
        executor.execute {
            this.webSocket = webSocket

            webSocket.webSocketState
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .distinctUntilChanged()
                .subscribe { onStateChange(it) }
        }
    }

    // Check the state, when it connected create PingPongSender to send ping messages
    private fun onStateChange(state: WebSocketConnectionState) {
        executor.execute {
            isPingPongNecessary = state == WebSocketConnectionState.CONNECTED

            if (isPingPongNecessary) {
                retrySendingErrorMessage()
            }

            if (sender == null && isPingPongNecessary) {
                sender = PingPongSender()
                sender!!.start()
            } else if (sender != null && !isPingPongNecessary) {
                sender!!.shutdown()
                sender = null
            }
        }
    }

    override fun onKeepAliveResponse(sentTime: Long) {
        executor.execute {
            lastTimeReceivePong = sentTime
        }
    }

    override fun onMessageError(request: WebSocketRequestMessage) {
        executor.execute {
            pendingMessage.add(request)
            AppDependencies.getIncomingMessageProcessor().onMessageSendTimeout(request)
        }
    }

    override fun onUserPresence(request: WebSocketRequestMessage) {
        executor.execute {
            // If request return ID = -1L, it's mean the user is offline otherwise online
            if (request.id == -1L) {
                webSocket?.postPresenceValue(request.from, false)
            } else {
                webSocket?.postPresenceValue(request.from, true)
            }
        }
    }

    private fun retrySendingErrorMessage() {
        executor.execute {
            val iterator = pendingMessage.iterator()

            while (iterator.hasNext()) {
                val request = iterator.next()
                Logging.debug(TAG, "Retry sending message id = ${request.id}")

                val response = webSocket!!.sendRequest(request)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .onErrorComplete()
                    .blockingGet()
                
                iterator.remove()

                AppDependencies.getIncomingMessageProcessor().process(response)
            }
        }
    }

    inner class PingPongSender : Thread() {
        @Volatile
        private var shouldSend = true

        override fun run() {
            lastTimeReceivePong = System.currentTimeMillis()

            while (shouldSend && isPingPongNecessary) {
                // Sleep for 50 seconds and then check if should send ping or not
                timer.sleep(TimeUnit.SECONDS.toMillis(PING_PONG_SENT_TIME_INTERVAL))

                if (shouldSend && isPingPongNecessary) {
                    val maxAllowedTime = TimeUnit.SECONDS.toMillis(
                        PING_PONG_SENT_TIME_INTERVAL
                    ) * MAX_MESSAGE_ALLOWED_MISSING
                    val sinceTime = System.currentTimeMillis() - maxAllowedTime

                    if (lastTimeReceivePong < sinceTime) {
                        webSocket?.disconnect()
                    } else {
                        webSocket?.sendPingMessage()
                    }
                }
            }
        }

        fun shutdown() {
            shouldSend = false
        }
    }
}
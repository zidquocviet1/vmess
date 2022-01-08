package com.mqv.realtimechatapplication.network.websocket

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
    private val executor = Executors.newSingleThreadExecutor()
    private var webSocket: WebSocketClient? = null
    private var sender: PingPongSender? = null
    private var isPingPongNecessary = false
    private var lastTimeReceivePong = 0L

    fun monitor(webSocket: WebSocketClient) {
        this.webSocket = webSocket

        webSocket.webSocketState
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .distinctUntilChanged()
            .subscribe { onStateChange(it) }
    }

    // Check the state, when it connected create PingPongSender to send ping messages
    private fun onStateChange(state: WebSocketConnectionState) {
        isPingPongNecessary = state == WebSocketConnectionState.CONNECTED

        if (sender == null && isPingPongNecessary) {
            sender = PingPongSender()
            sender!!.start()
        } else if (sender != null && !isPingPongNecessary) {
            sender!!.shutdown()
            sender = null;
        }
    }

    override fun onKeepAliveResponse(sentTime: Long) {
        lastTimeReceivePong = sentTime
    }

    override fun onMessageError() {
        TODO("Not yet implemented")
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
                    val sinceTime = System.currentTimeMillis() - (TimeUnit.SECONDS.toMillis(
                        PING_PONG_SENT_TIME_INTERVAL) * MAX_MESSAGE_ALLOWED_MISSING)

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
package com.mqv.vmess.network.websocket

class WebSocketAlarmTimer : Timer {
    override fun sleep(millis: Long) {
        Thread.sleep(millis)
    }
}
package com.mqv.realtimechatapplication.network.websocket

/*
* Simple timer only using Thread for sleeping
* */
class WebSocketSimpleTimer : Timer {
    override fun sleep(millis: Long) {
        Thread.sleep(millis)
    }
}
package com.mqv.vmess.notification

interface NotificationEntry {
    /*
    * The entry point to handle all notification of this app
    * */
    fun handleNotificationPayload(payload: NotificationPayload)
}
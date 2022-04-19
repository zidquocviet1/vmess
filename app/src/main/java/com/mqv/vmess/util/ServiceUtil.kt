package com.mqv.vmess.util

import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.telephony.TelephonyManager
import android.view.inputmethod.InputMethodManager

object ServiceUtil {
    @JvmStatic
    fun getInputMethodManager(context: Context): InputMethodManager =
        context.getSystemService(InputMethodManager::class.java)

    @JvmStatic
    fun getClipboardManager(context: Context): ClipboardManager =
        context.getSystemService(ClipboardManager::class.java)

    @JvmStatic
    fun getNotificationManager(context: Context): NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    @JvmStatic
    fun getTelephonyManager(context: Context): TelephonyManager =
        context.getSystemService(TelephonyManager::class.java)
}
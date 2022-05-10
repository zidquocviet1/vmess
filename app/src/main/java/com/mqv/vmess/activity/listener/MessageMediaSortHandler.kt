package com.mqv.vmess.activity.listener

import com.mqv.vmess.activity.preferences.MessageMediaSort

interface MessageMediaSortHandler {
    fun handleSortLatest()
    fun handleSortOldest()
    fun handleSortLargest()
    fun handleSortSmallest()
    fun onSortSelected(type: MessageMediaSort)
}
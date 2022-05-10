package com.mqv.vmess.activity.listener

import com.mqv.vmess.ui.data.MessageMedia
import kotlin.streams.toList

abstract class MessageMediaSortHelper<T : MessageMedia> {
    abstract fun getMediaList(): List<T>

    open fun sortByLatest(): List<T> =
        getMediaList().stream()
            .sorted { o1, o2 -> o2.timestamp.compareTo(o1.timestamp) }
            .toList()

    open fun sortByOldest(): List<T> =
        getMediaList().stream()
            .sorted { o1, o2 -> o1.timestamp.compareTo(o2.timestamp) }
            .toList()

    open fun sortByLargest(): List<T> =
        getMediaList().stream()
            .sorted { o1, o2 -> o2.size.compareTo(o1.size) }
            .toList()

    open fun sortBySmallest(): List<T> =
        getMediaList().stream()
            .sorted { o1, o2 -> o1.size.compareTo(o2.size) }
            .toList()
}
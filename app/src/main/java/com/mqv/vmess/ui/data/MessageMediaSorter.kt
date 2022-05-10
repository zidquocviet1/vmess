package com.mqv.vmess.ui.data

import com.mqv.vmess.activity.listener.MessageMediaSortHelper
import java.lang.IllegalStateException

class MessageMediaSorter<T : MessageMedia>(
    private val mediaList: List<T>,
    private val sortOnlyTime: Boolean = false
) : MessageMediaSortHelper<T>() {
    override fun getMediaList(): List<T> = mediaList

    override fun sortByLargest(): List<T> {
        if (sortOnlyTime) {
            throw IllegalStateException()
        }
        return super.sortByLargest()
    }

    override fun sortBySmallest(): List<T> {
        if (sortOnlyTime) {
            throw IllegalStateException()
        }
        return super.sortBySmallest()
    }
}
package com.mqv.vmess.ui.components

import android.view.View
import com.mqv.vmess.network.model.Chat

interface ImageLongClickListener {
    fun onLongClick(v: View, indexOfMedia: Int): Boolean
}
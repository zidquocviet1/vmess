package com.mqv.vmess.ui.components

import android.view.View
import com.mqv.vmess.network.model.Chat

interface ImageClickListener {
    fun onClick(v: View, photo: Chat.Photo)
}
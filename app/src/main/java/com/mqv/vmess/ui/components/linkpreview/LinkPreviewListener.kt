package com.mqv.vmess.ui.components.linkpreview

import com.mqv.vmess.network.model.Chat

interface LinkPreviewListener {
    fun onBindLinkPreview(messageId: String): LinkPreviewMetadata?
    fun onLoadLinkPreview(messageId: String, url: String)
    fun onOpenLink(url: String)
    fun onLinkPreviewLongClick(message: Chat): Boolean
}
package com.mqv.vmess.ui.adapter

import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.mqv.vmess.R
import com.mqv.vmess.databinding.ItemMediaLinkBinding
import com.mqv.vmess.ui.components.linkpreview.LinkPreviewMetadata
import com.mqv.vmess.ui.data.LinkMedia
import com.mqv.vmess.util.Picture

class MessageLinkAdapter :
    BaseAdapter<LinkMedia, ItemMediaLinkBinding>(object : DiffUtil.ItemCallback<LinkMedia>() {
        override fun areItemsTheSame(oldItem: LinkMedia, newItem: LinkMedia): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: LinkMedia, newItem: LinkMedia): Boolean =
            oldItem == newItem

    }) {
    override fun getViewRes(): Int = R.layout.item_media_link

    override fun getView(view: View): ItemMediaLinkBinding =
        ItemMediaLinkBinding.bind(view)

    override fun bindItem(item: LinkMedia, binding: ViewBinding) {
        with(binding as ItemMediaLinkBinding) {
            Picture.loadOutsideImage(root.context, item.thumbnail).into(imageLinkThumbnail)

            textTitle.text = item.title.ifEmpty { "Untitled" }
            textDescription.text = item.description.ifEmpty { "No content" }
            textLink.text = LinkPreviewMetadata.resolveHttpUrl(item.url)
        }
    }
}
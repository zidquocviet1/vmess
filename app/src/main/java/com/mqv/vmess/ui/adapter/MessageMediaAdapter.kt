package com.mqv.vmess.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.mqv.vmess.R
import com.mqv.vmess.databinding.ItemConversationMediaSelectionBinding
import com.mqv.vmess.ui.data.RemoteMedia
import com.mqv.vmess.util.Picture

class MessageMediaAdapter(
    private val realWidth: Int,
    val column: Int,
    private val spacing: Int,
) : BaseAdapter<RemoteMedia, ItemConversationMediaSelectionBinding>(object :
    DiffUtil.ItemCallback<RemoteMedia>() {
    override fun areItemsTheSame(oldItem: RemoteMedia, newItem: RemoteMedia): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: RemoteMedia, newItem: RemoteMedia): Boolean =
        oldItem == newItem
}) {
    private var mItemClickListener: ChildViewClickListener? = null

    override fun getViewRes(): Int = R.layout.item_conversation_media_selection

    override fun getView(view: View): ItemConversationMediaSelectionBinding =
        ItemConversationMediaSelectionBinding.bind(view)

    override fun bindItem(item: RemoteMedia, binding: ViewBinding) {
        with(binding as ItemConversationMediaSelectionBinding) {
            val thumbnailWidth: Int = (realWidth - (column - 1) * spacing) / column
            val context = root.context

            root.layoutParams.width = thumbnailWidth
            root.layoutParams.height = thumbnailWidth
            imageThumbnail.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            imageThumbnail.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT

            radioSelect.visibility = View.GONE
            textVideoDuration.visibility = View.GONE
            buttonPlay.visibility = if (item.type == "video") View.VISIBLE else View.GONE

            Picture.loadUserAvatar(context, item.path).centerCrop().into(imageThumbnail)
        }
    }

    override fun afterCreateViewHolder(binding: ViewBinding) {
        super.afterCreateViewHolder(binding)

        val itemBinding = binding as ItemConversationMediaSelectionBinding

        with(itemBinding) {
            imageThumbnail.setOnClickListener { _ ->
                mItemClickListener?.invoke(mRetriever?.invoke(root) ?: -1)
            }
        }
    }

    fun registerItemClick(listener: ChildViewClickListener) {
        mItemClickListener = listener
    }
}
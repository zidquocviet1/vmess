package com.mqv.vmess.ui.adapter

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.mqv.vmess.R
import com.mqv.vmess.databinding.ItemRecentSearchBinding
import com.mqv.vmess.ui.data.RecentSearch

class RecentSearchAdapter : BaseAdapter<RecentSearch, ItemRecentSearchBinding>(
    object : DiffUtil.ItemCallback<RecentSearch>() {
        override fun areItemsTheSame(oldItem: RecentSearch, newItem: RecentSearch) =
            oldItem.uid == newItem.uid

        override fun areContentsTheSame(oldItem: RecentSearch, newItem: RecentSearch) =
            oldItem == newItem
    }
) {
    interface OnSearchClickListener {
        fun onOpenConversation(childView: View)
        fun onRemoveRecentSearch(childView: View)
    }

    private var mCallback: OnSearchClickListener? = null

    override fun getViewRes() = R.layout.item_recent_search

    override fun getView(view: View) = ItemRecentSearchBinding.bind(view)

    override fun bindItem(item: RecentSearch, binding: ViewBinding) {
        with(binding as ItemRecentSearchBinding) {
            conversationThumbnail.setActiveStatus(item.isOnline)
            conversationThumbnail.setThumbnail(mutableListOf(item.photoUrl))
            conversationThumbnail.setSingleThumbnailSize(120, 120)
            textDisplayName.text = item.displayName
        }
    }

    override fun bindItem(item: RecentSearch, binding: ViewBinding, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            when (payloads[0]) {
                is Bundle -> {
                    val itemBinding = binding as ItemRecentSearchBinding
                    val bundle = payloads[0] as Bundle

                    with(itemBinding) {
                        if (bundle.getBoolean(ONLINE_PAYLOAD, false)) {
                            conversationThumbnail.setActiveStatus(item.isOnline)
                        }
                    }
                }
                else -> {}
            }
        }
    }

    override fun afterCreateViewHolder(binding: ViewBinding) {
        super.afterCreateViewHolder(binding)

        with (binding as ItemRecentSearchBinding) {
            remove.setOnClickListener { _ ->
                mCallback?.onRemoveRecentSearch(root)
            }
            conversationThumbnail.setOnClickListener { _ ->
                mCallback?.onOpenConversation(root)
            }
            textDisplayName.setOnClickListener { _ ->
                mCallback?.onOpenConversation(root)
            }
        }
    }

    fun registerOnSearchListener(listener: OnSearchClickListener) {
        mCallback = listener
    }
}
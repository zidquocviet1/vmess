package com.mqv.vmess.ui.adapter

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.mqv.vmess.R
import com.mqv.vmess.databinding.ItemUserSelectionBinding
import com.mqv.vmess.ui.data.UserSelection
import java.util.*
import java.util.function.BiConsumer

const val ONLINE_PAYLOAD = "online"
const val SELECTED_PAYLOAD = "selected"

class UserSelectionAdapter(
    private val mContext: Context,
    private val mMultiSelect: Boolean,
    private val mSingleSend: Boolean
) :
    BaseAdapter<UserSelection, ItemUserSelectionBinding>(object :
        DiffUtil.ItemCallback<UserSelection>() {
        override fun areItemsTheSame(oldItem: UserSelection, newItem: UserSelection): Boolean =
            oldItem.uid == newItem.uid

        override fun areContentsTheSame(oldItem: UserSelection, newItem: UserSelection): Boolean =
            (oldItem.uid == newItem.uid) &&
                    (Objects.equals(oldItem.photoUrl, newItem.photoUrl)) &&
                    (oldItem.displayName == newItem.displayName) &&
                    (oldItem.isOnline == newItem.isOnline) &&
                    (oldItem.isSelected == newItem.isSelected)

        override fun getChangePayload(oldItem: UserSelection, newItem: UserSelection): Any {
            return Bundle().apply {
                if (oldItem.isOnline != newItem.isOnline) {
                    putBoolean(ONLINE_PAYLOAD, true)
                } else if (oldItem.isSelected != newItem.isSelected) {
                    putBoolean(SELECTED_PAYLOAD, true)
                }
            }
        }
    }) {

    private var mSendMessageListener: BiConsumer<View, Button>? = null

    override fun getViewRes() = R.layout.item_user_selection

    override fun getView(view: View) = ItemUserSelectionBinding.bind(view)

    override fun bindItem(item: UserSelection, binding: ViewBinding) {
        val itemBinding = binding as ItemUserSelectionBinding

        with(itemBinding) {
            if (item.isConversation) {
                includedImageAvatar.setThumbnail(item.conversationMetadata!!.conversationThumbnail)
            } else {
                includedImageAvatar.setThumbnail(listOf(item.photoUrl))
            }
            includedImageAvatar.setActiveStatus(item.isOnline)
            radioSelect.visibility = if (mMultiSelect) View.VISIBLE else View.INVISIBLE
            buttonSendMessage.visibility = if (mSingleSend) View.VISIBLE else View.INVISIBLE
            textDisplayName.text = if (item.isConversation) item.conversationMetadata?.conversationName else item.displayName
            radioSelect.isChecked = item.isSelected
        }
    }

    override fun bindItem(item: UserSelection, binding: ViewBinding, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            when (payloads[0]) {
                is Bundle -> {
                    val itemBinding = binding as ItemUserSelectionBinding
                    val bundle = payloads[0] as Bundle

                    with(itemBinding) {
                        if (bundle.getBoolean(ONLINE_PAYLOAD, false)) {
                            includedImageAvatar.setActiveStatus(item.isOnline)
                        }
                        if (bundle.getBoolean(SELECTED_PAYLOAD, false)) {
                            radioSelect.isChecked = item.isSelected
                        }
                    }
                }
                else -> {}
            }
        }
    }

    override fun afterCreateViewHolder(binding: ViewBinding) {
        super.afterCreateViewHolder(binding)

        with(binding as ItemUserSelectionBinding) {
            buttonSendMessage.setOnClickListener { v ->
                mSendMessageListener?.accept(binding.root, v as Button)
            }
        }
    }

    fun registerSendMessageClickListener(callback: BiConsumer<View, Button>?) {
        mSendMessageListener = callback
    }
}
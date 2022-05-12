package com.mqv.vmess.ui.adapter

import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.mqv.vmess.R
import com.mqv.vmess.databinding.ItemPhoneContactBinding
import com.mqv.vmess.ui.data.PhoneContact
import com.mqv.vmess.util.Picture

class PhoneContactAdapter : BaseAdapter<PhoneContact, ItemPhoneContactBinding>(object :
    DiffUtil.ItemCallback<PhoneContact>() {
    override fun areItemsTheSame(oldItem: PhoneContact, newItem: PhoneContact): Boolean =
        oldItem.uid == newItem.uid

    override fun areContentsTheSame(oldItem: PhoneContact, newItem: PhoneContact): Boolean =
        oldItem == newItem
}) {
    private var mAddFriendListener: ChildViewClickListener? = null

    override fun getViewRes(): Int =
        R.layout.item_phone_contact

    override fun getView(view: View): ItemPhoneContactBinding =
        ItemPhoneContactBinding.bind(view)

    override fun bindItem(item: PhoneContact, binding: ViewBinding) {
        with(binding as ItemPhoneContactBinding) {
            Picture.loadUserAvatar(root.context, item.photoUrl).into(thumbnail)

            textDisplayName.text = item.displayName
            textContactNameAndPhoneNumber.text = root.context.getString(
                R.string.label_contact_name_and_phone_number,
                item.contactName,
                item.contactNumber
            )

            bindButtonInfo(item, binding)
        }
    }

    override fun bindItem(item: PhoneContact, binding: ViewBinding, payloads: MutableList<Any>) {
        with(binding as ItemPhoneContactBinding) {
            if (payloads.contains(PAYLOAD_LOADING)) {
                loading.show()
                buttonAddFriend.isEnabled = false
                buttonAddFriend.text = ""
            } else if (payloads.contains(PAYLOAD_STOP_LOADING)) {
                loading.hide()
                bindButtonInfo(item, binding)
            }
        }
    }

    override fun afterCreateViewHolder(binding: ViewBinding) {
        super.afterCreateViewHolder(binding)

        with(binding as ItemPhoneContactBinding) {
            buttonAddFriend.setOnClickListener {
                mAddFriendListener?.invoke(mRetriever?.invoke(root) ?: -1)
            }
        }
    }

    fun registerOnButtonClickListener(listener: ChildViewClickListener) {
        mAddFriendListener = listener
    }

    private fun bindButtonInfo(item: PhoneContact, binding: ItemPhoneContactBinding) {
        with(binding) {
            if (item.isPendingFriendRequest) {
                buttonAddFriend.text = root.context.getString(R.string.action_cancel)
                buttonAddFriend.isEnabled = true
                buttonAddFriend.setTextColor(root.context.getColor(R.color.purple_500))
            } else {
                if (item.isFriend) {
                    buttonAddFriend.text = root.context.getString(R.string.title_friend)
                    buttonAddFriend.isEnabled = false
                    buttonAddFriend.setTextColor(root.context.getColor(R.color.text_color))
                } else {
                    buttonAddFriend.text = root.context.getString(R.string.action_add_friend)
                    buttonAddFriend.isEnabled = true
                    buttonAddFriend.setTextColor(root.context.getColor(R.color.purple_500))
                }
            }
        }
    }

    companion object {
        const val PAYLOAD_LOADING = "loading"
        const val PAYLOAD_STOP_LOADING = "stop_loading"
    }
}
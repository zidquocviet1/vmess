package com.mqv.vmess.ui.adapter

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.mqv.vmess.R
import com.mqv.vmess.databinding.ItemActivePeopleBinding
import com.mqv.vmess.ui.data.People
import com.mqv.vmess.util.Picture

class ActivePeopleAdapter(private val mContext: Context) :
    BaseAdapter<People, ItemActivePeopleBinding>(object : DiffUtil.ItemCallback<People>() {
        override fun areItemsTheSame(oldItem: People, newItem: People) = oldItem.uid == newItem.uid

        override fun areContentsTheSame(oldItem: People, newItem: People) = oldItem == newItem
    }) {
    override fun getViewRes() = R.layout.item_active_people

    override fun getView(view: View) = ItemActivePeopleBinding.bind(view)

    override fun bindItem(item: People, binding: ViewBinding) {
        val itemBinding = binding as ItemActivePeopleBinding

        with(itemBinding) {
            includedImageAvatar.imageActive.visibility = View.VISIBLE
            textDisplayName.text = item.displayName

            Picture.loadUserAvatar(mContext, item.photoUrl).into(includedImageAvatar.imageAvatar)
        }
    }
}
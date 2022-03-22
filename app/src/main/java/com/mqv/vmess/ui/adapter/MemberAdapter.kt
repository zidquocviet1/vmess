package com.mqv.vmess.ui.adapter

import android.content.Context
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.viewbinding.ViewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.mqv.vmess.R
import com.mqv.vmess.databinding.ItemMemberBinding
import com.mqv.vmess.network.model.Conversation
import com.mqv.vmess.ui.data.Member
import com.mqv.vmess.util.Picture

class MemberAdapter(
    private val mContext: Context,
    private val mItemClick: OnMenuItemClick,
    private val mConversation: Conversation,
    private val mUser: FirebaseUser = FirebaseAuth.getInstance().currentUser!!
) : BaseAdapter<Member, ItemMemberBinding>(object : DiffUtil.ItemCallback<Member>() {
    override fun areItemsTheSame(oldItem: Member, newItem: Member): Boolean {
        return false
    }

    override fun areContentsTheSame(oldItem: Member, newItem: Member): Boolean {
        return false
    }
}) {
    interface OnMenuItemClick {
        fun onItemClick(item: Member, menuItem: MenuItem): Boolean
    }

    override fun getViewRes() = R.layout.item_member

    override fun getView(view: View) = ItemMemberBinding.bind(view)

    override fun bindItem(item: Member, binding: ViewBinding) {
        val itemBinding = binding as ItemMemberBinding

        with(itemBinding) {
            Picture.loadUserAvatar(mContext, item.photoUrl).into(includedImageAvatar.imageAvatar)
            textDisplayName.text = item.displayName
            textRole.text = if (item.isAdmin) mContext.getString(R.string.title_admin) else ""
            textRole.visibility = if (item.isAdmin) View.VISIBLE else View.GONE

            buttonHorizontalMore.setOnClickListener { v ->
                val menu = PopupMenu(mContext, v)

                menu.setOnMenuItemClickListener { menuItem -> mItemClick.onItemClick(item, menuItem) }
                menu.inflate(R.menu.menu_group_member)
                menu.show()

                if (!isCurrentUserAdmin()) {
                    menu.menu.removeItem(R.id.menu_make_as_admin)
                    menu.menu.removeItem(R.id.menu_remove_member)
                }

                if (item.uid == mUser.uid) {
                    menu.menu.removeItem(R.id.menu_remove_member)
                    menu.menu.removeItem(R.id.menu_view_profile)
                    menu.menu.removeItem(R.id.menu_block)
                } else {
                    menu.menu.removeItem(R.id.menu_leave_group)
                }
            }
        }
    }

    private fun isCurrentUserAdmin(): Boolean {
        return mConversation.group.adminId == mUser.uid
    }
}
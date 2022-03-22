package com.mqv.vmess.activity

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.AndroidViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.mqv.vmess.R
import com.mqv.vmess.databinding.ActivityGroupMemberBinding
import com.mqv.vmess.network.model.Conversation
import com.mqv.vmess.ui.adapter.MemberAdapter
import com.mqv.vmess.ui.data.Member
import com.mqv.vmess.util.AlertDialogUtil
import dagger.hilt.android.AndroidEntryPoint
import java.util.stream.Collectors

@AndroidEntryPoint
class GroupMemberActivity : ToolbarActivity<AndroidViewModel, ActivityGroupMemberBinding>(),
    MemberAdapter.OnMenuItemClick {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()

        updateActionBarTitle(R.string.label_group_member)

        val conversation = intent.getParcelableExtra<Conversation>("conversation")!!

        setupRecyclerView(conversation)
    }

    override fun binding() {
        mBinding = ActivityGroupMemberBinding.inflate(layoutInflater)
    }

    override fun getViewModelClass(): Class<AndroidViewModel>? = null

    override fun setupObserver() {
    }

    override fun onItemClick(item: Member, menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_remove_member -> {
                AlertDialogUtil.show(
                    this,
                    R.string.title_remove_member,
                    R.string.msg_remove_group_member,
                    R.string.action_yes,
                    R.string.action_cancel
                ) { dialog, _ ->
                    dialog.dismiss()
                    setResult(RESULT_OK, Intent().apply {
                        putExtra(EXTRA_TYPE, TYPE_REMOVE)
                        putExtra(EXTRA_MEMBER_ID, item.uid)
                    })
                    finish()
                }
                true
            }
            R.id.menu_leave_group -> {
                AlertDialogUtil.show(
                    this,
                    R.string.title_leave_group,
                    R.string.msg_leave_group,
                    R.string.action_yes,
                    R.string.action_cancel
                ) { dialog, _ ->
                    dialog.dismiss()
                    setResult(RESULT_OK, Intent().apply { putExtra(EXTRA_TYPE, TYPE_LEAVE) })
                    finish()
                }
                true
            }
            else -> false
        }
    }

    private fun setupRecyclerView(conversation: Conversation) {
        val mAdapter = MemberAdapter(this, this, conversation)

        mBinding.recyclerViewMember.adapter = mAdapter
        mBinding.recyclerViewMember.setHasFixedSize(true)
        mBinding.recyclerViewMember.layoutManager = LinearLayoutManager(this)

        mAdapter.submitList(
            conversation.participants.stream().map { user ->
                Member(
                    uid = user.uid,
                    photoUrl = user.photoUrl,
                    displayName = user.displayName,
                    isAdmin = user.uid == conversation.group.adminId,
                    nickname = null
                )
            }.collect(Collectors.toList())
        )
    }

    companion object {
        const val EXTRA_TYPE = "type"
        const val EXTRA_MEMBER_ID = "member_id"
        const val TYPE_REMOVE = 1
        const val TYPE_LEAVE = 2
        const val TYPE_ADMIN = 3
        const val TYPE_NICKNAME = 4
        const val TYPE_BLOCK = 5
    }
}
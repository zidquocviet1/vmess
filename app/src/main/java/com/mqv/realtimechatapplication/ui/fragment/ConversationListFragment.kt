package com.mqv.realtimechatapplication.ui.fragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mqv.realtimechatapplication.R
import com.mqv.realtimechatapplication.activity.AddGroupConversationActivity
import com.mqv.realtimechatapplication.activity.ConversationActivity
import com.mqv.realtimechatapplication.activity.MainActivity
import com.mqv.realtimechatapplication.activity.listener.ConversationListChanged
import com.mqv.realtimechatapplication.activity.preferences.PreferenceArchivedConversationActivity
import com.mqv.realtimechatapplication.activity.viewmodel.ConversationListViewModel
import com.mqv.realtimechatapplication.manager.LoggedInUserManager
import com.mqv.realtimechatapplication.network.model.Conversation
import com.mqv.realtimechatapplication.network.model.User
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType
import com.mqv.realtimechatapplication.ui.adapter.ConversationListAdapter
import com.mqv.realtimechatapplication.ui.data.UserSelection
import com.mqv.realtimechatapplication.util.MyActivityForResult
import java.util.*
import java.util.function.BiConsumer
import java.util.stream.Collectors

/*
* Base class for all the Fragment related to conversation list
* */
abstract class ConversationListFragment<V : ConversationListViewModel, VB : ViewBinding> :
    BaseSwipeFragment<V, VB>(),
    ConversationListChanged,
    ConversationDialogFragment.ConversationOptionListener {

    internal open lateinit var mAdapter: ConversationListAdapter
    internal open lateinit var mConversations: MutableList<Conversation>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeRecyclerview()
        mAdapter.registerOnDataSizeChanged { onDataSizeChanged(it) }
    }

    abstract fun initializeRecyclerview()
    abstract fun postToRecyclerview(runnable: Runnable)
    abstract fun onDataSizeChanged(isEmpty: Boolean)
    override fun onRefresh() {}

    override fun onDelete(conversation: Conversation?) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.msg_delete_conversation_title)
            .setMessage(R.string.msg_delete_conversation_message)
            .setPositiveButton(R.string.action_delete) { dialog, _ ->
                dialog.dismiss()
                mViewModel.delete(conversation)
                removeConversationUI(conversation!!)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .setBackground(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.background_alert_dialog_corner_radius
                )
            )
            .create()
            .show()
    }

    override fun onMuteNotification(conversation: Conversation?) {
        mViewModel.muteNotification(conversation)
    }

    override fun onCreateGroup(conversation: Conversation?, whoCreateWith: User) {
        val intent = Intent(requireContext(), AddGroupConversationActivity::class.java).apply {
            putExtra(EXTRA_USER, whoCreateWith)
        }
        getLauncherByActivity()?.launch(intent) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { intent ->
                    val userSelectedList =
                        intent.getParcelableArrayListExtra<UserSelection>(EXTRA_GROUP_PARTICIPANTS)

                    userSelectedList?.let {
                        val userParticipants = it.stream()
                            .map { us ->
                                with(us) {
                                    return@with User.Builder()
                                        .setUid(uid)
                                        .setDisplayName(displayName)
                                        .setPhotoUrl(photoUrl)
                                        .create()
                                }
                            }
                            .collect(Collectors.toList())

                        mViewModel.createGroup(LoggedInUserManager.getInstance().loggedInUser, userParticipants)
                    }
                }
            }
        }
    }

    override fun onLeaveGroup(conversation: Conversation?) {
        mViewModel.leaveGroup(conversation)
    }

    override fun onAddMember(conversation: Conversation?) {
        mViewModel.addMember(conversation)
    }

    override fun onMarkUnread(conversation: Conversation?) {
        mViewModel.markAsUnread(conversation)
    }

    override fun onIgnore(conversation: Conversation?) {
        mViewModel.ignore(conversation)
    }

    override fun onUnArchive(conversation: Conversation?) {
        mViewModel.changeConversationStatusType(conversation, ConversationStatusType.INBOX)
        removeConversationUI(conversation!!)
    }

    override fun onArchive(conversation: Conversation?) {
        mViewModel.changeConversationStatusType(conversation, ConversationStatusType.ARCHIVED)
        removeConversationUI(conversation!!)
    }

    override fun removeConversationUI(conversation: Conversation) {
        mConversations.remove(conversation)
        mAdapter.submitList(ArrayList(mConversations))
    }

    override fun bindPresenceConversation(onlineUsersId: List<String>) {
        if (onlineUsersId.isEmpty()) {
            mAdapter.notifyItemRangeChanged(
                0,
                mConversations.size,
                ConversationListAdapter.PRESENCE_OFFLINE_PAYLOAD
            )
        } else {
            val onlineUsers: List<User?> = onlineUsersId.stream()
                .map { id: String? -> User(id) }
                .collect(Collectors.toList())

            mConversations.stream()
                .collect(
                    Collectors.toMap(
                        { c -> mConversations.indexOf(c) },
                        { obj -> obj.participants })
                )
                .forEach { (index, users) ->
                    val hasAny = !Collections.disjoint(users, HashSet(onlineUsers))
                    val payload =
                        if (hasAny) ConversationListAdapter.PRESENCE_ONLINE_PAYLOAD else ConversationListAdapter.PRESENCE_OFFLINE_PAYLOAD
                    postToRecyclerview {
                        mAdapter.notifyItemChanged(
                            index,
                            payload
                        )
                    }
                }
        }
    }

    protected fun onConversationClick(): BiConsumer<Int, Boolean> = BiConsumer { pos, isLongClick ->
        val conversation = mAdapter.currentList[pos]

        if (isLongClick) {
            openConversationOptionDialog(conversation)
        } else {
            openConversation(conversation)
        }
    }

    private fun openConversationOptionDialog(conversation: Conversation?) {
        val dialog = ConversationDialogFragment.newInstance(this, conversation)
        dialog.show(parentFragmentManager, null)
    }

    private fun openConversation(conversation: Conversation?) {
        val conversationIntent =
            Intent(requireContext(), ConversationActivity::class.java).apply {
                putExtra(ConversationActivity.EXTRA_CONVERSATION, conversation)
            }

        getLauncherByActivity()?.launch(conversationIntent) { result: ActivityResult? ->
            onConversationOpenResult(result)
        }
    }

    // If new class extends this, it is need to be cast the Activity owner
    private fun getLauncherByActivity(): MyActivityForResult<Intent, ActivityResult>? {
        return when (requireActivity()) {
            is MainActivity -> (requireActivity() as MainActivity).activityResultLauncher
            is PreferenceArchivedConversationActivity -> (requireActivity() as PreferenceArchivedConversationActivity).activityResultLauncher
            else -> null
        }
    }

    protected open fun onConversationOpenResult(result: ActivityResult?) {
    }

    companion object {
        const val EXTRA_USER = "user"
        const val EXTRA_GROUP_PARTICIPANTS = "group_participants"
    }
}
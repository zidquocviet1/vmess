package com.mqv.vmess.ui.fragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mqv.vmess.R
import com.mqv.vmess.activity.AddGroupConversationActivity
import com.mqv.vmess.activity.ConversationActivity
import com.mqv.vmess.activity.MainActivity
import com.mqv.vmess.activity.listener.ConversationListChanged
import com.mqv.vmess.activity.preferences.PreferenceArchivedConversationActivity
import com.mqv.vmess.activity.viewmodel.ConversationListViewModel
import com.mqv.vmess.manager.LoggedInUserManager
import com.mqv.vmess.network.model.Conversation
import com.mqv.vmess.network.model.User
import com.mqv.vmess.network.model.type.ConversationStatusType
import com.mqv.vmess.network.model.type.ConversationType
import com.mqv.vmess.ui.ConversationOptionHandler
import com.mqv.vmess.ui.adapter.ConversationListAdapter
import com.mqv.vmess.ui.adapter.payload.ConversationNotificationPayload
import com.mqv.vmess.ui.adapter.payload.ConversationNotificationType
import com.mqv.vmess.ui.adapter.payload.ConversationPresencePayload
import com.mqv.vmess.ui.adapter.payload.ConversationPresenceType
import com.mqv.vmess.ui.data.UserSelection
import com.mqv.vmess.util.AlertDialogUtil
import com.mqv.vmess.util.DateTimeHelper.expire
import com.mqv.vmess.util.DateTimeHelper.toLong
import com.mqv.vmess.util.Logging
import com.mqv.vmess.util.MyActivityForResult
import java.time.LocalDateTime
import java.util.*
import java.util.function.BiConsumer
import java.util.stream.Collectors
import kotlin.streams.toList

/*
* Base class for all the Fragment related to conversation list
* */
abstract class ConversationListFragment<V : ConversationListViewModel, VB : ViewBinding> :
    BaseSwipeFragment<V, VB>(),
    ConversationListChanged,
    ConversationDialogFragment.ConversationOptionListener {

    private lateinit var mConversationHandler: ConversationOptionHandler
    private lateinit var mNonExpireNotificationOption: List<String>

    internal open lateinit var mAdapter: ConversationListAdapter
    internal open lateinit var mConversations: MutableList<Conversation?>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeRecyclerview()
        mAdapter.registerOnDataSizeChanged { onDataSizeChanged(it) }
        mConversationHandler = ConversationOptionHandler(requireContext())
    }

    abstract fun initializeRecyclerview()
    abstract fun postToRecyclerview(runnable: Runnable)
    abstract fun onDataSizeChanged(isEmpty: Boolean)
    override fun onRefresh() {}

    override fun setupObserver() {
        mViewModel.presenceUserListObserverDistinct.observe(this) { bindPresenceConversation() }
        mViewModel.conversationNotificationOption.observe(this) { bindNotificationOption() }
    }

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
        AlertDialogUtil.showMuteNotificationSelectionDialog(requireContext()) { _, which ->
            when (which) {
                0 -> mViewModel.muteNotification(conversation, 1 * 60 * 60) // 1 hour
                1 -> mViewModel.muteNotification(conversation, 8 * 60 * 60) // 8 hours
                2 -> mViewModel.muteNotification(conversation, 1 * 24 * 60 * 60) // 1 day
                3 -> mViewModel.muteNotification(conversation, 7 * 24 * 60 * 60) // 7 days
                else -> mViewModel.muteNotification(conversation, Long.MAX_VALUE) // always
            }
        }
    }

    override fun onUnMuteNotification(conversation: Conversation?) {
        mViewModel.unMuteNotification(conversation)
    }

    override fun onCreateGroup(conversation: Conversation?, whoCreateWith: User) {
        val intent = Intent(requireContext(), AddGroupConversationActivity::class.java).apply {
            putExtra(EXTRA_USER, whoCreateWith)
        }
        getLauncherByActivity()?.launch(intent) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { intent ->
                    val userSelectedList =
                        intent.getParcelableArrayListExtra<UserSelection>(
                            AddGroupConversationActivity.EXTRA_GROUP_PARTICIPANTS
                        )

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

                        mViewModel.createGroup(
                            LoggedInUserManager.getInstance().loggedInUser,
                            userParticipants
                        )
                    }
                }
            }
        }
    }

    override fun onLeaveGroup(conversation: Conversation?) {
        AlertDialogUtil.show(
            requireContext(),
            R.string.title_leave_group,
            R.string.msg_leave_group,
            R.string.action_yes,
            R.string.action_cancel
        ) { dialog, _ ->
            dialog.dismiss()
            mViewModel.leaveGroup(conversation)
        }
    }

    override fun onAddMember(conversation: Conversation?) {
        mConversationHandler.addMember(getLauncherByActivity()!!, conversation!!) { memberIds ->
            mViewModel.addMember(conversation.id, memberIds)
        }
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
        submitCurrentData()
    }

    override fun bindPresenceConversation() {
        val onlineUsersId = mViewModel.presenceUserList
        if (onlineUsersId.isEmpty()) {
            mAdapter.notifyItemRangeChanged(
                0,
                mConversations.size,
                ConversationPresencePayload(ConversationPresenceType.OFFLINE, LocalDateTime.now().toLong())
            )
        } else {
            val onlineUsers: List<User?> = onlineUsersId.stream()
                .map { id: String? -> User(id) }
                .collect(Collectors.toList())

            mConversations.stream()
                .collect(
                    Collectors.toMap(
                        { c -> mConversations.indexOf(c) },
                        { obj -> obj?.participants })
                )
                .forEach { (index, users) ->
                    val hasAny = !Collections.disjoint(users, HashSet(onlineUsers))
                    val payload =
                        if (hasAny) {
                            ConversationPresencePayload(ConversationPresenceType.ONLINE, LocalDateTime.now().toLong())
                        } else {
                            ConversationPresencePayload(ConversationPresenceType.OFFLINE, LocalDateTime.now().toLong())
                        }
                    postToRecyclerview {
                        mAdapter.notifyItemChanged(
                            index,
                            payload
                        )
                    }
                }
        }
    }

    override fun bindNotificationOption() {
        mViewModel.conversationNotificationOption?.value?.let { option ->
            mNonExpireNotificationOption = option.stream()
                .filter { !it.until.expire() }
                .map { it.conversationId }
                .toList()

            mConversations.stream()
                .forEach { conversation ->
                    postToRecyclerview {
                        val payload = if (mNonExpireNotificationOption.contains(conversation?.id)) {
                            ConversationNotificationPayload(
                                ConversationNotificationType.OFF,
                                LocalDateTime.now().toLong()
                            )
                        } else {
                            ConversationNotificationPayload(
                                ConversationNotificationType.ON,
                                LocalDateTime.now().toLong()
                            )
                        }
                        mAdapter.notifyItemChanged(
                            mConversations.indexOf(conversation),
                            payload
                        )
                    }
                }
        }
    }

    override fun addLoadingUI(onAdded: Runnable) {
        Logging.debug(
            TAG,
            "Add temp conversation with id: ${-1} in charge of loading indicator view"
        )

        mConversations.add(
            Conversation(
                "-1",
                mutableListOf(),
                ConversationType.NORMAL,
                ConversationStatusType.INBOX,
                LocalDateTime.now()
            )
        )
        mAdapter.submitList(ArrayList(mConversations), onAdded)
    }

    override fun removeLoadingUI() {
        Logging.debug(TAG, "Remove temp conversation was added, and then submit list")

        mConversations.removeLast()
        submitCurrentData()
    }

    override fun onMoreConversation(conversation: List<Conversation>) {
        Logging.debug(
            TAG,
            "Get new conversation list size = ${conversation.size}, prepend to current list and then submit and update to view model"
        )

        mConversations.addAll(conversation)
        mViewModel.updateCurrentList(mConversations)
        mViewModel.saveConversation(conversation)
    }

    private fun submitCurrentData() {
        mAdapter.submitList(ArrayList(mConversations))
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
        val dialog = ConversationDialogFragment.newInstance(
            this,
            conversation,
            mNonExpireNotificationOption.stream().anyMatch { id -> id == conversation?.id })
        dialog.show(parentFragmentManager, null)
    }

    private fun openConversation(conversation: Conversation?) {
        val conversationIntent =
            Intent(requireContext(), ConversationActivity::class.java).apply {
                putExtra(ConversationActivity.EXTRA_CONVERSATION_ID, conversation?.id)
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

    override fun onConnectionStateChanged() {
        onRefresh()
    }

    protected fun registerLoadMore() {
        mViewModel.loadMore()
    }

    protected fun submitAndBinding() {
        mAdapter.submitList(ArrayList(mConversations)) {
            bindNotificationOption()
            bindPresenceConversation()
        }
    }

    companion object {
        const val EXTRA_USER = "user"
        private val TAG: String = ConversationListFragment::class.java.simpleName
    }
}
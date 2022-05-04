package com.mqv.vmess.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.annotation.StringRes
import androidx.core.view.get
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.appbar.MaterialToolbar
import com.mqv.vmess.R
import com.mqv.vmess.activity.viewmodel.ConversationDetailViewModel
import com.mqv.vmess.databinding.ActivityConversationDetailBinding
import com.mqv.vmess.network.model.User
import com.mqv.vmess.network.model.type.ConversationType
import com.mqv.vmess.ui.data.AlertDialogData
import com.mqv.vmess.ui.data.Type
import com.mqv.vmess.ui.data.UserSelection
import com.mqv.vmess.ui.fragment.ConversationDetailFragmentDirections
import com.mqv.vmess.ui.fragment.InputDialogData
import com.mqv.vmess.ui.fragment.VMessAlertDialogFragment
import com.mqv.vmess.ui.fragment.preference.ConversationPreferenceFragment
import com.mqv.vmess.util.AlertDialogUtil
import com.mqv.vmess.util.Logging
import com.mqv.vmess.util.NetworkStatus
import dagger.hilt.android.AndroidEntryPoint

typealias InflateMenuCallback = (isGroup: Boolean) -> Unit

@AndroidEntryPoint
class ConversationDetailActivity :
    ToolbarActivity<ConversationDetailViewModel, ActivityConversationDetailBinding>(),
    NavController.OnDestinationChangedListener,
    ConversationPreferenceFragment.ConversationPreferenceListener {

    private var mInflatedMenu: Boolean = false
    private var mInflateMenuCallback: InflateMenuCallback? = null
    private lateinit var mNavController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()

        updateActionBarTitle(R.string.dummy_empty_string)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        mNavController = navHostFragment.navController
        mNavController.addOnDestinationChangedListener(this)
    }

    override fun binding() {
        mBinding = ActivityConversationDetailBinding.inflate(layoutInflater)
    }

    override fun getViewModelClass(): Class<ConversationDetailViewModel> =
        ConversationDetailViewModel::class.java

    override fun setupObserver() {
        mViewModel.conversationDetail.observe(this) { detail ->
            if (!mInflatedMenu) {
                mInflateMenuCallback?.invoke(detail.metadata.type == ConversationType.GROUP)
            }
        }
        mViewModel.singleToast.observe(this) { event ->
            event?.getContentIfNotHandled()?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
        mViewModel.requestState.observe(this) { event ->
            event?.getContentIfNotHandled()?.let { result ->
                when (result.status) {
                    NetworkStatus.LOADING -> AlertDialogUtil.startLoadingDialog(
                        this,
                        layoutInflater,
                        R.string.action_loading
                    )
                    NetworkStatus.SUCCESS -> AlertDialogUtil.finishLoadingDialog()
                    else -> AlertDialogUtil.finishLoadingDialog()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        mInflateMenuCallback = { isGroup ->
            mInflatedMenu = true
            menuInflater.inflate(
                if (isGroup) R.menu.menu_conversation_detail_group else R.menu.menu_conversation_detail_personal,
                menu
            )
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_change_name -> {
                mNavController.navigate(
                    ConversationDetailFragmentDirections.actionInputDialogFragment(
                        InputDialogData(
                            R.string.title_change_group_name,
                            R.string.prompt_enter_your_group_name
                        ),
                        mViewModel.conversationDetail.value!!.metadata.conversationName
                    )
                )
            }
            R.id.menu_change_thumbnail -> {
                mNavController.navigate(
                    ConversationDetailFragmentDirections.actionConversationDetailFragmentToHandleThumbnailDialogFragment()
                )
            }
            R.id.menu_create_group -> {
                val metadata = mViewModel.conversationDetail.value!!.metadata
                val participants = metadata.conversationParticipants

                onCreateGroup(participants.filter { user -> user.uid != metadata.currentUserId }[0])
            }
            R.id.menu_delete_conversation -> {
                VMessAlertDialogFragment.newInstance(
                    AlertDialogData(
                        title = R.string.msg_delete_conversation_title,
                        message = R.string.msg_delete_conversation_message,
                        positiveButton = R.string.action_delete,
                        negativeButton = R.string.action_cancel,
                        type = Type.DELETE
                    )
                ).setOnPositiveClickListener {
                    Logging.debug(TAG, "Request delete conversation")

                    setResult(RESULT_OK)
                    startActivity(
                        Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra(MainActivity.EXTRA_CONVERSATION, mViewModel.conversation)
                            action = MainActivity.ACTION_DELETE_CONVERSATION
                        }
                    )
                }.show(supportFragmentManager, null)
            }
            R.id.menu_leave_group -> {
                VMessAlertDialogFragment.newInstance(
                    AlertDialogData(
                        title = R.string.title_leave_group,
                        message = R.string.msg_leave_group,
                        positiveButton = R.string.action_yes,
                        negativeButton = R.string.action_cancel,
                        type = Type.LEAVE_GROUP
                    )
                ).setOnPositiveClickListener {
                    Logging.debug(TAG, "Request leave group")

                    setResult(RESULT_OK)
                    startActivity(
                        Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra(MainActivity.EXTRA_CONVERSATION, mViewModel.conversation)
                            action = MainActivity.ACTION_LEAVE_GROUP
                        }
                    )
                }.show(supportFragmentManager, null)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        when (destination.id) {
            R.id.notificationPreferenceFragment -> changeActionBar(
                R.string.title_disappearing_messages,
                false
            )
            R.id.colorAndWallpaperFragment -> changeActionBar(
                R.string.title_chat_color_and_wallpaper,
                false
            )
            R.id.mediaAndFileFragment -> changeActionBar(
                R.string.title_media,
                false
            )
            R.id.notificationAndSoundFragment -> changeActionBar(
                R.string.title_preference_item_notification_and_sounds,
                false
            )
            R.id.conversationDetailFragment -> changeActionBar(
                R.string.dummy_empty_string,
                true
            )
        }
    }

    override fun onChatAndWallPaper() {
        mNavController.navigate(R.id.colorAndWallpaperFragment)
    }

    override fun onDisappearingMessages() {
        mNavController.navigate(R.id.notificationPreferenceFragment)
    }

    override fun onNotificationAndSound() {
        mNavController.navigate(R.id.notificationAndSoundFragment)
    }

    override fun onViewMediaAndFile() {
        mNavController.navigate(R.id.mediaAndFileFragment)
    }

    override fun onCreateGroup(user: User) {
        activityResultLauncher.launch(
            Intent(this, AddGroupConversationActivity::class.java).apply {
                putExtra(AddGroupConversationActivity.EXTRA_ADD_MEMBER, false)
                putExtra(AddGroupConversationActivity.EXTRA_USER, user)
            }
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { intent ->
                    val userSelection = intent.getParcelableArrayListExtra<UserSelection>(
                        AddGroupConversationActivity.EXTRA_GROUP_PARTICIPANTS
                    )

                    Logging.debug(TAG, "Request create group with ${userSelection?.size}")

                    setResult(RESULT_OK)
                    startActivity(
                        Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra(
                                AddGroupConversationActivity.EXTRA_GROUP_PARTICIPANTS,
                                userSelection
                            )
                            action = MainActivity.ACTION_CREATE_GROUP
                        }
                    )
                }
            }
        }
    }

    override fun onSeeGroupMember() {
        activityResultLauncher.launch(
            Intent(this, GroupMemberActivity::class.java).apply {
                putExtra("conversation", mViewModel.conversation)
            }
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                if (data != null) {
                    when (data.getIntExtra(GroupMemberActivity.EXTRA_TYPE, -1)) {
                        GroupMemberActivity.TYPE_REMOVE -> {
                            setResult(
                                RESULT_OK,
                                Intent(this, ConversationActivity::class.java).apply {
                                    action = ConversationActivity.ACTION_REMOVE_GROUP_MEMBER
                                    putExtra(
                                        ConversationActivity.EXTRA_REMOVE_MEMBER_ID,
                                        data.getStringExtra(GroupMemberActivity.EXTRA_MEMBER_ID)
                                    )
                                })
                            finish()
                        }
                        GroupMemberActivity.TYPE_LEAVE -> {
                            setResult(
                                RESULT_OK,
                                Intent(this, ConversationActivity::class.java).apply {
                                    action = ConversationActivity.ACTION_LEAVE_GROUP
                                })
                            finish()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onIgnoreMessage() {
    }

    override fun onBlock() {
    }

    override fun onReport() {
    }

    private fun changeActionBar(@StringRes title: Int, shouldShowMenu: Boolean) {
        updateActionBarTitle(title)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val menu = toolbar.menu

        for (i in 0 until menu.size()) {
            menu[i].isVisible = shouldShowMenu
        }
    }

    companion object {
        private val TAG: String = ConversationDetailActivity::class.java.simpleName

        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val KEY_DELETE_CONVERSATION = "delete_conversation"
        const val KEY_LEAVE_GROUP = "leave_group"
    }
}
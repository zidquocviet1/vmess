package com.mqv.vmess.ui.fragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.mqv.vmess.R
import com.mqv.vmess.activity.AddGroupConversationActivity
import com.mqv.vmess.activity.ConversationActivity
import com.mqv.vmess.activity.PreviewEditPhotoActivity
import com.mqv.vmess.activity.viewmodel.ConversationDetailViewModel
import com.mqv.vmess.databinding.FragmentConversationDetailBinding
import com.mqv.vmess.network.model.type.ConversationType
import com.mqv.vmess.ui.ConversationOptionHandler
import com.mqv.vmess.ui.data.RoundedIconButton
import com.mqv.vmess.ui.data.ConversationDetail
import com.mqv.vmess.ui.data.UserSelection
import com.mqv.vmess.ui.fragment.preference.ConversationPreferenceFragment
import com.mqv.vmess.util.AlertDialogUtil
import com.mqv.vmess.util.DateTimeHelper.toLong
import com.mqv.vmess.util.FileProviderUtil
import com.mqv.vmess.util.Logging
import com.mqv.vmess.util.NavigationUtil.getNavigationResult
import com.mqv.vmess.util.NetworkStatus
import com.mqv.vmess.util.views.ViewUtil
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.time.LocalDateTime
import java.util.stream.Collectors

@AndroidEntryPoint
class ConversationDetailFragment :
    BaseFragment<ConversationDetailViewModel, FragmentConversationDetailBinding>() {
    private var isMuteNotification: Boolean = false

    override fun binding(inflater: LayoutInflater, container: ViewGroup?) {
        mBinding = FragmentConversationDetailBinding.inflate(inflater, container, false)
    }

    override fun getViewModelClass(): Class<ConversationDetailViewModel> =
        ConversationDetailViewModel::class.java

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        registerClickEvent()
    }

    override fun setupObserver() {
        mViewModel.conversationDetail.observe(viewLifecycleOwner, ::showConversationDetail)
        mViewModel.singleToast.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
        mViewModel.requestState.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let { result ->
                when (result.status) {
                    NetworkStatus.LOADING -> AlertDialogUtil.startLoadingDialog(
                        requireContext(),
                        layoutInflater,
                        R.string.action_loading
                    )
                    NetworkStatus.SUCCESS -> AlertDialogUtil.finishLoadingDialog()
                    else -> AlertDialogUtil.finishLoadingDialog()
                }
            }
        }

        getNavigationResult<Long>(
            R.id.conversationDetailFragment,
            KEY_NOTIFICATION_UNTIL
        ) { until ->
            mViewModel.muteNotification(until)
        }

        getNavigationResult<String>(R.id.conversationDetailFragment, KEY_GROUP_NAME) { groupName ->
            Logging.debug(TAG, "Receive new group name request: $groupName")

            mViewModel.changeGroupName(groupName)
        }

        getNavigationResult<Uri>(R.id.conversationDetailFragment, KEY_TAKE_PHOTO) { uri ->
            Logging.debug(TAG, "Get new file to change thumbnail, file uri = $uri")

            mTakePictureLauncher.launch(uri) { isSuccess ->
                if (isSuccess) {
                    val imageThumbnail =
                        FileProviderUtil.getImageThumbnailFromUri(
                            requireContext().contentResolver,
                            uri
                        )
                    val intent =
                        Intent(requireContext(), PreviewEditPhotoActivity::class.java).apply {
                            putExtra(
                                PreviewEditPhotoActivity.EXTRA_CHANGE_PHOTO,
                                PreviewEditPhotoActivity.EXTRA_GROUP_THUMBNAIL
                            )
                            putExtra(PreviewEditPhotoActivity.EXTRA_IMAGE_THUMBNAIL, imageThumbnail)
                        }
                    mActivityLauncher.launch(intent) { result: ActivityResult ->
                        if (result.resultCode == RESULT_OK) {
                            val data = result.data
                            if (data != null) {
                                val filePath =
                                    data.getStringExtra(PreviewEditPhotoActivity.EXTRA_FILE_PATH_RESULT)
                                filePath?.let {
                                    val file = File(it)

                                    Logging.debug(
                                        TAG,
                                        "File for change thumbnail, file size = ${file.length()}"
                                    )

                                    mViewModel.changeGroupThumbnail(file)
                                }
                            }
                        }
                    }
                } else {
                    requireContext().contentResolver.delete(uri, null, null)
                }
            }
        }

        getNavigationResult<Boolean>(R.id.conversationDetailFragment, KEY_CHOOSE_PHOTO) { isOpen ->
            if (isOpen) {
                mGetContentLauncher.launch("image/*") { uri ->
                    if (uri != null) {
                        val path = FileProviderUtil.getPath(requireContext(), uri)
                        if (path != "") {
                            val file = File(path)

                            Logging.debug(
                                TAG,
                                "File for change thumbnail, file size = ${file.length()}"
                            )

                            mViewModel.changeGroupThumbnail(file)
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Can't open the file, check later",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun showConversationDetail(detail: ConversationDetail?) {
        detail?.let {
            val metadata = detail.metadata
            val notificationOption = detail.notificationOption
            val isMuteNotification = notificationOption.until >= LocalDateTime.now().toLong()

            with(mBinding) {
                conversationBanner.setMetadata(metadata)
                conversationBanner.setSingleThumbnailSize(
                    ViewUtil.getLargeUserAvatarPixel(resources),
                    ViewUtil.getLargeUserAvatarPixel(resources)
                )

                setupButtonList(
                    isGroup = metadata.type == ConversationType.GROUP,
                    isMuteNotification = isMuteNotification
                )
            }

            // Create navigation graph programmatically because start argument is needed
            val navController = Navigation.findNavController(
                requireActivity(),
                R.id.fragment_conversation_preference
            )

            if (navController.currentDestination == null) {
                val navGraph =
                    navController.navInflater.inflate(R.navigation.conversation_preference)

                navController.setGraph(
                    navGraph, bundleOf(
                        ConversationPreferenceFragment.ARG_CONVERSATION to it
                    )
                )
            } else {
                navController.currentBackStackEntry?.savedStateHandle?.set(
                    KEY_MUTE_NOTIFICATION,
                    isMuteNotification
                )
            }
        }
    }

    private fun setupButtonList(isGroup: Boolean, isMuteNotification: Boolean) {
        this.isMuteNotification = isMuteNotification
        with(mBinding) {
            if (isGroup) {
                conversationBanner.setOnThumbnailClickListener {
                    findNavController().navigate(R.id.handleThumbnailDialogFragment)
                }
            }
            buttonPhone.setButton(mListButton[KEY_BUTTON_AUDIO])
            buttonVideo.setButton(mListButton[KEY_BUTTON_VIDEO])
            buttonAddMember.setButton(mListButton[KEY_BUTTON_ADD_MEMBER])
            buttonSearch.setButton(mListButton[KEY_BUTTON_SEARCH])
            if (isMuteNotification) {
                mBinding.buttonNotification.setButton(mListButton[KEY_BUTTON_NOTIFICATION])
            } else {
                mBinding.buttonNotification.setButton(mListButton[KEY_BUTTON_MUTE_NOTIFICATION])
            }

            buttonAddMember.visibility = if (isGroup) View.VISIBLE else View.GONE
        }
    }

    private fun registerClickEvent() {
        with(mBinding) {
            buttonPhone.setOnButtonClickListener { }
            buttonVideo.setOnButtonClickListener { }
            buttonNotification.setOnButtonClickListener {
                if (isMuteNotification) {
                    mViewModel.unMuteNotification()
                } else {
                    findNavController().navigate(R.id.muteNotificationDialogFragment)
                }
            }
            buttonAddMember.setOnButtonClickListener {
                mActivityLauncher.launch(
                    Intent(requireContext(), AddGroupConversationActivity::class.java).apply {
                        putStringArrayListExtra(
                            ConversationOptionHandler.EXTRA_GROUP_MEMBER_ID,
                            mViewModel.conversation.participants
                                .stream()
                                .map { user -> user.uid }
                                .collect(Collectors.toCollection { ArrayList<String>() })
                        )
                        putExtra(AddGroupConversationActivity.EXTRA_ADD_MEMBER, true)
                    }
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        result.data?.let { intent ->
                            val members = intent.getParcelableArrayListExtra<UserSelection>(
                                AddGroupConversationActivity.EXTRA_GROUP_PARTICIPANTS
                            )

                            Logging.debug(
                                TAG,
                                "Request add ${members?.size} new members to this conversation"
                            )

                            requireActivity().setResult(
                                RESULT_OK,
                                Intent(requireContext(), ConversationActivity::class.java).apply {
                                    action = ConversationActivity.ACTION_ADD_MEMBER
                                    putExtra(ConversationActivity.EXTRA_MEMBER_TO_ADD, members)
                                })
                            requireActivity().finish()
                        }
                    }
                }
            }
            buttonSearch.setOnButtonClickListener {
                requireActivity().setResult(
                    RESULT_OK,
                    Intent(requireContext(), ConversationActivity::class.java).apply {
                        action = ConversationActivity.ACTION_SEARCH_MESSAGE
                    })
                requireActivity().finish()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            ConversationDetailFragment()

        private val TAG: String = ConversationDetailFragment::class.java.simpleName

        const val KEY_NOTIFICATION_UNTIL = "notification_until"
        const val KEY_MUTE_NOTIFICATION = "mute_notification"
        const val KEY_TAKE_PHOTO = "take_photo"
        const val KEY_CHOOSE_PHOTO = "choose_photo"
        const val KEY_GROUP_NAME = "group_name"

        private const val KEY_BUTTON_AUDIO = "button_audio"
        private const val KEY_BUTTON_VIDEO = "button_video"
        private const val KEY_BUTTON_MUTE_NOTIFICATION = "button_mute_notification"
        private const val KEY_BUTTON_NOTIFICATION = "button_notification"
        private const val KEY_BUTTON_ADD_MEMBER = "button_add_member"
        private const val KEY_BUTTON_SEARCH = "button_search"

        private val mListButton = mutableMapOf(
            KEY_BUTTON_AUDIO to RoundedIconButton(R.string.label_audio, R.drawable.ic_phone),
            KEY_BUTTON_VIDEO to RoundedIconButton(
                R.string.label_video,
                R.drawable.ic_round_videocam
            ),
            KEY_BUTTON_MUTE_NOTIFICATION to RoundedIconButton(
                R.string.label_conversation_mute_notifications_shortly,
                R.drawable.ic_round_notifications
            ),
            KEY_BUTTON_NOTIFICATION to RoundedIconButton(
                R.string.label_conversation_unmute_notifications_shortly,
                R.drawable.ic_round_notifications_off
            ),
            KEY_BUTTON_ADD_MEMBER to RoundedIconButton(
                R.string.action_add,
                R.drawable.ic_round_group_add_member
            ),
            KEY_BUTTON_SEARCH to RoundedIconButton(
                R.string.action_search_message,
                R.drawable.ic_search
            )
        )
    }
}
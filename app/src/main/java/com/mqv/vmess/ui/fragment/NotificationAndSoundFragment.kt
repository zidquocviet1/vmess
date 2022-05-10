package com.mqv.vmess.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.mqv.vmess.R
import com.mqv.vmess.activity.viewmodel.ConversationDetailViewModel
import com.mqv.vmess.databinding.FragmentNotificationAndSoundBinding
import com.mqv.vmess.util.DateTimeHelper.toLong
import com.mqv.vmess.util.NavigationUtil.getNavigationResult
import java.time.LocalDateTime

class NotificationAndSoundFragment :
    BaseFragment<ConversationDetailViewModel, FragmentNotificationAndSoundBinding>() {
    private var isTurnOnNotification: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(mBinding) {
            layoutSwitch.setOnClickListener {
                if (isTurnOnNotification) {
                    findNavController().navigate(R.id.muteNotificationDialogFragment)
                } else {
                    mViewModel.unMuteNotification()
                }
            }
            layoutCustomize.setOnClickListener { openAppNotificationSettings() }
        }
    }

    override fun binding(inflater: LayoutInflater, container: ViewGroup?) {
        mBinding = FragmentNotificationAndSoundBinding.inflate(inflater, container, false)
    }

    override fun getViewModelClass(): Class<ConversationDetailViewModel> =
        ConversationDetailViewModel::class.java

    override fun setupObserver() {
        mViewModel.conversationDetail.observe(viewLifecycleOwner) { detail ->
            isTurnOnNotification = detail.notificationOption.until < LocalDateTime.now().toLong()
            mBinding.switchOn.isChecked = isTurnOnNotification
        }
        getNavigationResult<Long>(
            R.id.notificationAndSoundFragment,
            ConversationDetailFragment.KEY_NOTIFICATION_UNTIL
        ) { until ->
            mViewModel.muteNotification(until)
        }
    }

    private fun openAppNotificationSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
        }

        requireContext().startActivity(intent)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            NotificationAndSoundFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}
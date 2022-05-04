package com.mqv.vmess.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.mqv.vmess.activity.viewmodel.ConversationDetailViewModel
import com.mqv.vmess.databinding.FragmentNotificationPreferenceBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationPreferenceFragment :
    BaseFragment<ConversationDetailViewModel, FragmentNotificationPreferenceBinding>() {
    override fun binding(inflater: LayoutInflater, container: ViewGroup?) {
        mBinding = FragmentNotificationPreferenceBinding.inflate(inflater, container, false)
    }

    override fun getViewModelClass(): Class<ConversationDetailViewModel> = ConversationDetailViewModel::class.java

    override fun setupObserver() {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.buttonSave.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}
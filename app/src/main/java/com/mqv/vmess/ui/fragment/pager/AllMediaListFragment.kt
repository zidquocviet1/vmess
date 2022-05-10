package com.mqv.vmess.ui.fragment.pager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mqv.vmess.activity.ConversationDetailActivity
import com.mqv.vmess.activity.viewmodel.ConversationDetailViewModel
import com.mqv.vmess.databinding.FragmentAllListMediaBinding
import com.mqv.vmess.ui.fragment.BaseFragment
import com.mqv.vmess.util.NavigationUtil.setNavigationResult

class AllMediaListFragment : BaseFragment<ConversationDetailViewModel, FragmentAllListMediaBinding>() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        setNavigationResult(ConversationDetailActivity.KEY_SUBTITLE, "1024 media messages")
    }

    override fun binding(inflater: LayoutInflater, container: ViewGroup?) {
        mBinding = FragmentAllListMediaBinding.inflate(inflater, container, false)
    }

    override fun getViewModelClass(): Class<ConversationDetailViewModel> = ConversationDetailViewModel::class.java

    override fun setupObserver() {
    }
}
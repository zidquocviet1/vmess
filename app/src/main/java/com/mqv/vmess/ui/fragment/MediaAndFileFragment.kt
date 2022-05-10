package com.mqv.vmess.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import com.mqv.vmess.R
import com.mqv.vmess.activity.viewmodel.ConversationDetailViewModel
import com.mqv.vmess.databinding.FragmentMediaAndFileBinding
import com.mqv.vmess.ui.adapter.MediaAndFileStateAdapter

class MediaAndFileFragment :
    BaseFragment<ConversationDetailViewModel, FragmentMediaAndFileBinding>() {
    private lateinit var mTabMediator: TabLayoutMediator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.viewPager.adapter =
            MediaAndFileStateAdapter(requireActivity().supportFragmentManager, lifecycle)

        mTabMediator = TabLayoutMediator(mBinding.tabLayout, mBinding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.title_media)
                1 -> getString(R.string.title_links)
                else -> getString(R.string.title_all)
            }
        }
        mTabMediator.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        mTabMediator.detach()
    }

    override fun binding(inflater: LayoutInflater, container: ViewGroup?) {
        mBinding = FragmentMediaAndFileBinding.inflate(inflater, container, false)
    }

    override fun getViewModelClass(): Class<ConversationDetailViewModel> =
        ConversationDetailViewModel::class.java

    override fun setupObserver() {
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            MediaAndFileFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}
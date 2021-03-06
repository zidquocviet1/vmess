package com.mqv.vmess.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mqv.vmess.activity.viewmodel.ConversationListArchivedViewModel
import com.mqv.vmess.databinding.FragmentConversationListArchivedBinding
import com.mqv.vmess.ui.adapter.ConversationListAdapter

class ConversationListArchivedFragment :
    ConversationListFragment<ConversationListArchivedViewModel, FragmentConversationListArchivedBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun binding(inflater: LayoutInflater, container: ViewGroup?) {
        mBinding = FragmentConversationListArchivedBinding.inflate(inflater, container, false)
    }

    override fun getViewModelClass(): Class<ConversationListArchivedViewModel> =
        ConversationListArchivedViewModel::class.java

    override fun setupObserver() {
        super.setupObserver()

        mViewModel.listObserver.observe(viewLifecycleOwner) { list ->
            mConversations = list
            submitAndBinding()
        }
    }

    override fun initializeRecyclerview() {
        mConversations = ArrayList()
        mAdapter = ConversationListAdapter(requireContext(), getPlaintextCallback())
        mAdapter.registerOnConversationClick(onConversationClick())

        mBinding.recyclerViewArchivedChats.adapter = mAdapter
        mBinding.recyclerViewArchivedChats.setHasFixedSize(true)
        mBinding.recyclerViewArchivedChats.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun postToRecyclerview(runnable: Runnable) {
        mBinding.recyclerViewArchivedChats.post(runnable)
    }

    override fun getSwipeLayout(): SwipeRefreshLayout? = null

    override fun onDataSizeChanged(isEmpty: Boolean) {
        if (isEmpty) {
            mBinding.recyclerViewArchivedChats.visibility = View.GONE
            mBinding.imageNoData.visibility = View.VISIBLE
            mBinding.textNoData.visibility = View.VISIBLE
        } else {
            mBinding.recyclerViewArchivedChats.visibility = View.VISIBLE
            mBinding.imageNoData.visibility = View.GONE
            mBinding.textNoData.visibility = View.GONE
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            ConversationListArchivedFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}
package com.mqv.vmess.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.mqv.vmess.activity.viewmodel.UserSelectionListViewModel
import com.mqv.vmess.databinding.FragmentSuggestionFriendListBinding
import com.mqv.vmess.ui.adapter.BaseAdapter
import com.mqv.vmess.ui.adapter.UserSelectionAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates

@AndroidEntryPoint
class SuggestionFriendListFragment :
    BaseFragment<UserSelectionListViewModel, FragmentSuggestionFriendListBinding>() {

    private lateinit var mAdapter: UserSelectionAdapter
    private var mMultiSelect by Delegates.notNull<Boolean>()
    private var mSingleSend by Delegates.notNull<Boolean>()
    private var mIncludeGroup by Delegates.notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mMultiSelect = it.getBoolean(ARG_MULTI_SELECT, false)
            mSingleSend = it.getBoolean(ARG_SINGLE_SEND, false)
            mIncludeGroup = it.getBoolean(ARG_INCLUDE_GROUP, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAdapter = UserSelectionAdapter(requireContext(), mMultiSelect, mSingleSend)

        with(mBinding) {
            recyclerFriendSuggestion.adapter = mAdapter
            recyclerFriendSuggestion.layoutManager = LinearLayoutManager(requireContext())
            recyclerFriendSuggestion.setHasFixedSize(true)
            textClear.visibility = if (mMultiSelect) View.VISIBLE else View.INVISIBLE
            textClear.setOnClickListener { mViewModel.clearAllSelect() }
        }

        mAdapter.registerEventHandler(object : BaseAdapter.ItemEventHandler {
            override fun onItemClick(position: Int) {
                if (!mSingleSend) {
                    mViewModel.notifyUserSelect(mAdapter.currentList[position])
                }
            }
        })

        mAdapter.registerSendMessageClickListener { view, button ->
            val position = mBinding.recyclerFriendSuggestion.getChildAdapterPosition(view)
            val selection = mAdapter.currentList[position]

            button.isEnabled = false

            mViewModel.sendMessage(requireContext(), selection)
        }
    }

    override fun binding(inflater: LayoutInflater, container: ViewGroup?) {
        mBinding = FragmentSuggestionFriendListBinding.inflate(inflater, container, false)
    }

    override fun getViewModelClass(): Class<UserSelectionListViewModel> =
        UserSelectionListViewModel::class.java

    override fun setupObserver() {
        mViewModel.userSuggestionList.observe(viewLifecycleOwner) { userSuggestionList ->
            mAdapter.submitList(ArrayList(userSuggestionList))
        }

        mViewModel.userRecentSearchList.observe(viewLifecycleOwner) { userRecentSearchList ->
            mBinding.textRecentSearch.visibility =
                if (userRecentSearchList.isEmpty()) View.GONE else View.VISIBLE
            mBinding.recyclerFriendRecentSearch.visibility =
                if (userRecentSearchList.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    companion object {
        const val ARG_MULTI_SELECT = "multi_select"
        const val ARG_SINGLE_SEND = "single_send"
        const val ARG_INCLUDE_GROUP = "include_group"
        const val ARG_MESSAGE_TO_SEND = "message"

        @JvmStatic
        @JvmOverloads
        fun newInstance(
            multiSelect: Boolean = false,
            singleSend: Boolean = false,
            includeGroup: Boolean = false
        ) =
            SuggestionFriendListFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_MULTI_SELECT, multiSelect)
                    putBoolean(ARG_SINGLE_SEND, singleSend)
                    putBoolean(ARG_INCLUDE_GROUP, includeGroup)
                }
            }
    }
}
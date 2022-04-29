package com.mqv.vmess.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.mqv.vmess.activity.SearchConversationActivity
import com.mqv.vmess.activity.viewmodel.UserSelectionListViewModel
import com.mqv.vmess.databinding.FragmentSuggestionFriendListBinding
import com.mqv.vmess.ui.adapter.BaseAdapter
import com.mqv.vmess.ui.adapter.ONLINE_PAYLOAD
import com.mqv.vmess.ui.adapter.RecentSearchAdapter
import com.mqv.vmess.ui.adapter.UserSelectionAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SuggestionFriendListFragment :
    BaseFragment<UserSelectionListViewModel, FragmentSuggestionFriendListBinding>(),
    RecentSearchAdapter.OnSearchClickListener {

    private lateinit var mAdapter: UserSelectionAdapter
    private lateinit var mRecentSearchAdapter: RecentSearchAdapter

    private var mMultiSelect = false
    private var mSingleSend = false
    private var mIncludeGroup = false
    private var mShouldHideSearchBar = false
    private var mSearchHandler: SearchHandler? = null

    interface SearchHandler {
        fun onOpenConversation(userId: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is SearchConversationActivity) {
            mSearchHandler = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mMultiSelect = it.getBoolean(ARG_MULTI_SELECT, false)
            mSingleSend = it.getBoolean(ARG_SINGLE_SEND, false)
            mIncludeGroup = it.getBoolean(ARG_INCLUDE_GROUP, false)
            mShouldHideSearchBar = it.getBoolean(ARG_HIDE_SEARCH_BAR, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mRecentSearchAdapter = RecentSearchAdapter()
        mAdapter = UserSelectionAdapter(requireContext(), mMultiSelect, mSingleSend)

        with(mBinding) {
            recyclerFriendSuggestion.adapter = mAdapter
            recyclerFriendSuggestion.layoutManager = LinearLayoutManager(requireContext())
            textClear.visibility = if (mMultiSelect) View.VISIBLE else View.INVISIBLE
            textClear.setOnClickListener { mViewModel.clearAllSelect() }
            includedSearchBar.root.visibility =
                if (mShouldHideSearchBar) View.GONE else View.VISIBLE

            recyclerFriendRecentSearch.adapter = mRecentSearchAdapter
            recyclerFriendRecentSearch.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        mAdapter.registerEventHandler(object : BaseAdapter.ItemEventHandler {
            override fun onItemClick(position: Int) {
                if (!mSingleSend) {
                    mViewModel.notifyUserSelect(mAdapter.currentList[position])
                }
                mSearchHandler?.onOpenConversation(mAdapter.currentList[position].uid)
            }
        })

        mAdapter.registerSendMessageClickListener { sendView, button ->
            val position = mBinding.recyclerFriendSuggestion.getChildAdapterPosition(sendView)
            val selection = mAdapter.currentList[position]

            button.isEnabled = false

            mViewModel.sendMessage(requireContext(), selection)
        }

        mRecentSearchAdapter.registerOnSearchListener(this)
    }

    override fun binding(inflater: LayoutInflater, container: ViewGroup?) {
        mBinding = FragmentSuggestionFriendListBinding.inflate(inflater, container, false)
    }

    override fun getViewModelClass(): Class<UserSelectionListViewModel> =
        UserSelectionListViewModel::class.java

    override fun setupObserver() {
        mViewModel.userSuggestionList.observe(viewLifecycleOwner) { userSuggestionList ->
            mAdapter.submitList(ArrayList(userSuggestionList))
            mAdapter.notifyItemRangeChanged(0, mAdapter.itemCount, Bundle().apply {
                putBoolean(ONLINE_PAYLOAD, true)
            })
        }

        mViewModel.userRecentSearchList.observe(viewLifecycleOwner) { userRecentSearchList ->
            if (mSearchHandler == null) {
                mBinding.textRecentSearch.visibility = View.GONE
                mBinding.recyclerFriendRecentSearch.visibility = View.GONE
                return@observe
            }

            mRecentSearchAdapter.submitList(ArrayList(userRecentSearchList))
            mRecentSearchAdapter.notifyItemRangeChanged(
                0,
                mRecentSearchAdapter.itemCount,
                Bundle().apply {
                    putBoolean(ONLINE_PAYLOAD, true)
                })

            mBinding.textRecentSearch.visibility =
                if (userRecentSearchList.isEmpty()) View.GONE else View.VISIBLE
            mBinding.recyclerFriendRecentSearch.visibility =
                if (userRecentSearchList.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    override fun onOpenConversation(childView: View) {
        retrievePositionFromView(childView).let {
            if (it != -1) {
                mSearchHandler?.onOpenConversation(mRecentSearchAdapter.currentList[it].uid)
            }
        }
    }

    override fun onRemoveRecentSearch(childView: View) {
        retrievePositionFromView(childView).let {
            if (it != -1) {
                mViewModel.removeRecentSearch(mRecentSearchAdapter.currentList[it].toRecentSearchPeople())
            }
        }
    }

    private fun retrievePositionFromView(view: View) =
        mBinding.recyclerFriendRecentSearch.getChildAdapterPosition(view)

    fun insertRecentSearchPeople(userId: String) {
        mViewModel.insertRecentSearch(userId)
    }

    companion object {
        const val ARG_MULTI_SELECT = "multi_select"
        const val ARG_SINGLE_SEND = "single_send"
        const val ARG_INCLUDE_GROUP = "include_group"
        const val ARG_MESSAGE_TO_SEND = "message"
        const val ARG_HIDE_SEARCH_BAR = "hide_search_bar"

        @JvmStatic
        @JvmOverloads
        fun newInstance(
            multiSelect: Boolean = false,
            singleSend: Boolean = false,
            includeGroup: Boolean = false,
            hideSearchBar: Boolean = false
        ) =
            SuggestionFriendListFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_MULTI_SELECT, multiSelect)
                    putBoolean(ARG_SINGLE_SEND, singleSend)
                    putBoolean(ARG_INCLUDE_GROUP, includeGroup)
                    putBoolean(ARG_HIDE_SEARCH_BAR, hideSearchBar)
                }
            }
    }
}
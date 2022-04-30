package com.mqv.vmess.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.mqv.vmess.R
import com.mqv.vmess.activity.AddConversationActivity
import com.mqv.vmess.activity.SearchConversationActivity
import com.mqv.vmess.databinding.FragmentSearchResultBinding
import com.mqv.vmess.ui.adapter.BaseAdapter
import com.mqv.vmess.ui.adapter.UserSelectionAdapter
import com.mqv.vmess.ui.data.UserSelection
import com.mqv.vmess.ui.fragment.viewmodel.SearchViewModel
import com.mqv.vmess.util.Logging
import com.mqv.vmess.util.NetworkStatus
import kotlin.streams.toList

class SearchResultFragment : BaseFragment<SearchViewModel, FragmentSearchResultBinding>(),
    BaseAdapter.ItemEventHandler {
    private lateinit var mSearchName: String
    private lateinit var mAdapter: UserSelectionAdapter

    private var mCallback: SuggestionFriendListFragment.SearchHandler? = null

    override fun binding(inflater: LayoutInflater, container: ViewGroup?) {
        mBinding = FragmentSearchResultBinding.inflate(inflater, container, false)
    }

    override fun getViewModelClass(): Class<SearchViewModel> = SearchViewModel::class.java

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is SearchConversationActivity || context is AddConversationActivity) {
            mCallback = context as SuggestionFriendListFragment.SearchHandler
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mSearchName = it.getString(EXTRA_NAME, "")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(mBinding) {
            mAdapter =
                UserSelectionAdapter(requireContext(), mMultiSelect = false, mSingleSend = false)
            mAdapter.registerEventHandler(this@SearchResultFragment)
            recyclerSearchUser.adapter = mAdapter
            recyclerSearchUser.layoutManager = LinearLayoutManager(requireContext())
        }

        Logging.debug(TAG, "Receive search request with name = $mSearchName")

        mViewModel.requestSearchName(mSearchName, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mViewModel.resetSearchResult()
    }

    override fun setupObserver() {
        mViewModel.searchResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                when (result.status) {
                    NetworkStatus.LOADING -> {
                        mBinding.progressBarLoading.show()
                        mBinding.recyclerSearchUser.visibility = View.GONE
                        mBinding.imageSearchError.visibility = View.GONE
                        mBinding.textError.visibility = View.GONE
                    }
                    NetworkStatus.SUCCESS -> {
                        mBinding.progressBarLoading.hide()
                        mBinding.recyclerSearchUser.visibility = View.VISIBLE
                        mBinding.imageSearchError.visibility = View.GONE
                        mBinding.textError.visibility = View.GONE

                        mAdapter.submitList(result.success.stream().map { people ->
                            with(people) {
                                UserSelection(
                                    uid,
                                    photoUrl,
                                    displayName,
                                    isOnline = false,
                                    isSelected = false
                                )
                            }
                        }.toList())
                    }
                    else -> {
                        mBinding.progressBarLoading.hide()
                        mBinding.recyclerSearchUser.visibility = View.GONE
                        mBinding.imageSearchError.visibility = View.VISIBLE
                        mBinding.textError.visibility = View.VISIBLE
                        mBinding.textError.text =
                            getString(R.string.error_no_result_found_for, mSearchName)
                    }
                }
            }
        }
    }

    override fun onItemClick(position: Int) {
        super.onItemClick(position)

        mCallback?.onOpenConversation(mAdapter.currentList[position].uid)
    }

    fun postNewNameRequest(name: String) {
        mSearchName = name
        mViewModel.requestSearchName(name, false)
    }

    companion object {
        val TAG: String = SearchResultFragment::class.java.simpleName
        const val EXTRA_NAME = "name"

        @JvmStatic
        fun newInstance(name: String) =
            SearchResultFragment().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_NAME, name)
                }
            }
    }
}
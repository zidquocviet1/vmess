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

const val ARG_MULTI_SELECT = "multi_select"

@AndroidEntryPoint
class SuggestionFriendListFragment :
    BaseFragment<UserSelectionListViewModel, FragmentSuggestionFriendListBinding>() {

    private lateinit var mAdapter: UserSelectionAdapter
    private var mMultiSelect by Delegates.notNull<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mMultiSelect = it.getBoolean(ARG_MULTI_SELECT, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAdapter = UserSelectionAdapter(requireContext(), mMultiSelect)

        with(mBinding) {
            recyclerFriendSuggestion.adapter = mAdapter
            recyclerFriendSuggestion.layoutManager = LinearLayoutManager(requireContext())
            recyclerFriendSuggestion.setHasFixedSize(true)
            textClear.visibility = if (mMultiSelect) View.VISIBLE else View.INVISIBLE
            textClear.setOnClickListener { mViewModel.clearAllSelect() }
        }

        mAdapter.registerEventHandler(object : BaseAdapter.ItemEventHandler {
            override fun onItemClick(position: Int) {
                mViewModel.notifyUserSelect(mAdapter.currentList[position])
            }
        })
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
    }

    companion object {
        @JvmStatic
        fun newInstance(multiSelect: Boolean) =
            SuggestionFriendListFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_MULTI_SELECT, multiSelect)
                }
            }
    }
}
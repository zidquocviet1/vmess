package com.mqv.vmess.ui.fragment.pager

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mqv.vmess.R
import com.mqv.vmess.activity.ConversationDetailActivity
import com.mqv.vmess.activity.preferences.MessageMediaSort
import com.mqv.vmess.activity.viewmodel.ConversationDetailViewModel
import com.mqv.vmess.databinding.FragmentLinkListBinding
import com.mqv.vmess.ui.adapter.BaseAdapter
import com.mqv.vmess.ui.adapter.MessageLinkAdapter
import com.mqv.vmess.ui.components.linkpreview.LinkPreviewMetadata
import com.mqv.vmess.ui.data.LinkMedia
import com.mqv.vmess.ui.data.MessageMediaSorter
import com.mqv.vmess.ui.fragment.BaseFragment
import com.mqv.vmess.util.NavigationUtil.setNavigationResult
import com.mqv.vmess.util.NetworkStatus
import kotlin.streams.toList

class LinkListFragment : BaseFragment<ConversationDetailViewModel, FragmentLinkListBinding>() {
    private lateinit var mAdapter: MessageLinkAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mAdapter = MessageLinkAdapter()
        mAdapter.registerEventHandler(object : BaseAdapter.ItemEventHandler {
            override fun onItemClick(position: Int) {
                openLink(mAdapter.currentList[position].url)
            }

            override fun onListItemSizeChanged(size: Int) {
                mBinding.textNoMedia.visibility = if (size == 0) View.VISIBLE else View.GONE
                mBinding.recyclerMedia.visibility = if (size == 0) View.GONE else View.VISIBLE
            }
        })

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(mBinding) {
            recyclerMedia.adapter = mAdapter
            recyclerMedia.layoutManager = LinearLayoutManager(requireContext())
            recyclerMedia.addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    LinearLayout.VERTICAL
                )
            )
        }

        super.onViewCreated(view, savedInstanceState)

        val shareMessages = mViewModel.conversation.chats.stream()
            .filter { chat -> !chat.isUnsent && chat.share != null }
            .toList()
        mViewModel.loadBunchOfLinkPreview(shareMessages)
    }

    override fun onResume() {
        super.onResume()

        setNavigationResult(
            ConversationDetailActivity.KEY_SUBTITLE,
            if (mAdapter.currentList.isEmpty()) getString(R.string.label_zero_media_link_number)
            else requireContext().resources.getQuantityString(
                R.plurals.label_media_links_number,
                mAdapter.currentList.size,
                mAdapter.currentList.size
            )
        )
    }

    override fun binding(inflater: LayoutInflater, container: ViewGroup?) {
        mBinding = FragmentLinkListBinding.inflate(inflater, container, false)
    }

    override fun getViewModelClass(): Class<ConversationDetailViewModel> =
        ConversationDetailViewModel::class.java

    override fun setupObserver() {
        mViewModel.linkMediaResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                when (result.status) {
                    NetworkStatus.LOADING -> {
                        with(mBinding) {
                            progressBarLoading.show()
                            textNoMedia.visibility = View.GONE
                            recyclerMedia.visibility = View.GONE
                        }
                    }
                    NetworkStatus.SUCCESS -> {
                        with(mBinding) {
                            progressBarLoading.hide()
                            recyclerMedia.visibility = View.VISIBLE

                            mViewModel.mediaSortType.value?.let { type ->
                                sortByType(result.success, type)
                            }
                        }
                    }
                    else -> {
                        with(mBinding) {
                            progressBarLoading.hide()
                            recyclerMedia.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }

        mViewModel.mediaSortType.observe(viewLifecycleOwner) { type ->
            sortByType(mAdapter.currentList, type)
        }
    }

    private fun sortByType(linkMedia: List<LinkMedia>, type: MessageMediaSort) {
        val sorter = MessageMediaSorter(linkMedia)
        when (type) {
            MessageMediaSort.LATEST -> mAdapter.submitList(sorter.sortByLatest())
            MessageMediaSort.OLDEST -> mAdapter.submitList(sorter.sortByOldest())
            MessageMediaSort.LARGEST -> mAdapter.submitList(sorter.sortByLargest())
            else -> mAdapter.submitList(sorter.sortBySmallest())
        }
    }

    private fun openLink(unResolveUrl: String) {
        requireContext().startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(LinkPreviewMetadata.resolveHttpUrl(unResolveUrl))
            )
        )
    }
}
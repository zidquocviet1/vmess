package com.mqv.vmess.ui.fragment.pager

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.mqv.vmess.R
import com.mqv.vmess.activity.ConversationDetailActivity
import com.mqv.vmess.activity.SelectPhotoActivity
import com.mqv.vmess.activity.VideoPlayerActivity
import com.mqv.vmess.activity.preferences.MessageMediaSort
import com.mqv.vmess.activity.viewmodel.ConversationDetailViewModel
import com.mqv.vmess.databinding.FragmentMediaListBinding
import com.mqv.vmess.ui.adapter.BaseAdapter
import com.mqv.vmess.ui.adapter.MessageMediaAdapter
import com.mqv.vmess.ui.data.MessageMediaSorter
import com.mqv.vmess.ui.data.RemoteMedia
import com.mqv.vmess.ui.fragment.BaseFragment
import com.mqv.vmess.ui.fragment.MediaAndFileFragmentDirections
import com.mqv.vmess.util.DateTimeHelper.toLong
import com.mqv.vmess.util.NavigationUtil.setNavigationResult
import kotlin.streams.toList

private const val NUM_SPACING = 9 // 9px
private const val NUM_PORTRAIT_COL = 5
private const val NUM_LANDSCAPE_COL = 9
private const val PHOTO = "photo"
private const val VIDEO = "video"

class MediaListFragment : BaseFragment<ConversationDetailViewModel, FragmentMediaListBinding>() {
    private lateinit var mAdapter: MessageMediaAdapter

    @SuppressLint("UnsafeOptInUsageError")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        /*
        * Get actual device width and height. Also known as Dimension
        * */
        val dimension = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getRealMetrics(dimension)
        val actualDeviceWidth = dimension.widthPixels
        val spanCount =
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) NUM_PORTRAIT_COL else NUM_LANDSCAPE_COL

        mAdapter = MessageMediaAdapter(actualDeviceWidth, spanCount, NUM_SPACING)
        mAdapter.registerEventHandler(object : BaseAdapter.ItemEventHandler {
            override fun onListItemSizeChanged(size: Int) {
                mBinding.textNoMedia.visibility = if (size == 0) View.VISIBLE else View.GONE
                mBinding.recyclerMedia.visibility = if (size == 0) View.GONE else View.VISIBLE
            }
        })
        mAdapter.registerItemClick { pos ->
            with(mAdapter.currentList[pos]) {
                if (type == PHOTO) {
                    findNavController().navigate(
                        MediaAndFileFragmentDirections.actionPreviewImageDialogFragment(
                            mAdapter.currentList[pos].path
                        )
                    )
                } else {
                    startActivity(Intent(
                        this@MediaListFragment.requireContext(),
                        VideoPlayerActivity::class.java
                    ).apply {
                        putExtra(
                            VideoPlayerActivity.EXTRA_START_INDEX,
                            0
                        )
                        putStringArrayListExtra(
                            VideoPlayerActivity.EXTRA_URI,
                            arrayListOf(path)
                        )
                    })
                }
            }
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(mBinding) {
            recyclerMedia.adapter = mAdapter
            recyclerMedia.layoutManager = GridLayoutManager(requireContext(), mAdapter.column)
            recyclerMedia.addItemDecoration(
                SelectPhotoActivity.GridSpacingItemDecoration(
                    mAdapter.column,
                    NUM_SPACING,
                    false
                )
            )
        }

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        val totalPhoto = mAdapter.currentList.stream().filter { it.type == PHOTO }
            .count().toInt()
        val totalVideo = mAdapter.currentList.stream().filter { it.type == VIDEO }
            .count().toInt()
        val displayDetail = "${
            if (totalPhoto != 0) requireContext().resources.getQuantityString(
                R.plurals.label_media_photo_number,
                totalPhoto,
                totalPhoto
            ) else getString(R.string.label_zero_media_photo_number)
        }, ${
            if (totalVideo != 0) requireContext().resources.getQuantityString(
                R.plurals.label_media_video_number,
                totalVideo,
                totalVideo
            ) else getString(R.string.label_zero_media_video_number)
        }"

        setNavigationResult(
            ConversationDetailActivity.KEY_SUBTITLE,
            displayDetail
        )
    }

    override fun binding(inflater: LayoutInflater, container: ViewGroup?) {
        mBinding = FragmentMediaListBinding.inflate(inflater, container, false)
    }

    override fun getViewModelClass(): Class<ConversationDetailViewModel> =
        ConversationDetailViewModel::class.java

    override fun setupObserver() {
        val media = mViewModel.conversation.chats.stream()
            .filter { chat -> !chat.isUnsent }
            .map { chat ->
                val photo = chat.photos?.stream()?.map {
                    with(it) {
                        RemoteMedia(
                            chat.id.hashCode() + uri.hashCode(),
                            uri,
                            PHOTO,
                            chat.timestamp.toLong(),
                            0
                        )
                    }
                }?.toList() ?: listOf()

                val video = chat.videos?.stream()?.map {
                    with(it) {
                        RemoteMedia(
                            chat.id.hashCode() + uri.hashCode(),
                            uri,
                            VIDEO,
                            chat.timestamp.toLong(),
                            0
                        )
                    }
                }?.toList() ?: listOf()

                val media = mutableListOf<RemoteMedia>()

                media.addAll(photo)
                media.addAll(video)
                media
            }
            .flatMap { it.stream() }
            .toList()

        mViewModel.mediaSortType.observe(viewLifecycleOwner) { type ->
            val sorter = MessageMediaSorter(media)
            when (type) {
                MessageMediaSort.LATEST -> mAdapter.submitList(sorter.sortByLatest())
                MessageMediaSort.OLDEST -> mAdapter.submitList(sorter.sortByOldest())
                MessageMediaSort.LARGEST -> mAdapter.submitList(sorter.sortByLargest())
                else -> mAdapter.submitList(sorter.sortBySmallest())
            }
        }
    }
}
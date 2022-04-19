package com.mqv.vmess.ui.components.conversation

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.mqv.vmess.R
import com.mqv.vmess.databinding.CustomConversationMultiMediaFooterBinding
import com.mqv.vmess.network.model.type.MessageMediaUploadType
import com.mqv.vmess.ui.adapter.ConversationMediaSelectionAdapter
import com.mqv.vmess.ui.data.Media
import com.mqv.vmess.util.StorageUtil
import java.util.stream.Collectors

class ConversationMultiMediaFooter @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val mBinding = CustomConversationMultiMediaFooterBinding.bind(
        inflate(
            context,
            R.layout.custom_conversation_multi_media_footer,
            this
        )
    )
    private val mAdapter: ConversationMediaSelectionAdapter
    private var mCallback: Callback? = null

    interface Callback {
        fun onMediaClick(totalSelected: Int)
        fun onGalleryClick()
        fun onFileClick()
        fun onRequestPermissionStorage()
    }

    init {
        mAdapter = ConversationMediaSelectionAdapter(context, mBinding.recyclerViewPhotoVideo)
        mBinding.recyclerViewPhotoVideo.adapter = mAdapter
        mBinding.recyclerViewPhotoVideo.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        mAdapter.setOnMediaClickListener { totalSelected ->
            mCallback?.onMediaClick(totalSelected)
        }
        mBinding.buttonGallery.setOnClickListener { _ ->
            mCallback?.onGalleryClick()
        }
        mBinding.buttonFile.setOnClickListener { _ ->
            mCallback?.onFileClick()
        }
        visibility = GONE
    }

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }

    fun isShowing() = (visibility == View.VISIBLE)

    fun setCallback(callback: Callback) {
        mCallback = callback
    }

    fun setMediaList(media: List<Media>) {
        if (StorageUtil.canReadDataFromMediaStore(context)) {
            mAdapter.submitList(ArrayList(media))

            mBinding.textPermission.visibility = View.GONE
            mBinding.buttonPermissionGiveAccess.visibility = View.GONE
        } else {
            mBinding.textPermission.visibility = View.VISIBLE
            mBinding.buttonPermissionGiveAccess.visibility = View.VISIBLE

            mBinding.buttonPermissionGiveAccess.setOnClickListener { _ ->
                mCallback?.onRequestPermissionStorage()
            }
        }
    }

    fun getSelectedListMediaAsPath(): Map<MessageMediaUploadType, List<String>> {
        return mAdapter.currentList.stream()
            .filter { media -> media.isSelected }
            .collect(Collectors.toList())
            .groupBy { media -> media.mimeType }
            .mapKeys {
                when (it.key) {
                    "image/jpeg" -> MessageMediaUploadType.PHOTO
                    "image/png" -> MessageMediaUploadType.PHOTO
                    "video/mp4" -> MessageMediaUploadType.VIDEO
                    "video/mp3" -> MessageMediaUploadType.VIDEO
                    "video/wav" -> MessageMediaUploadType.VIDEO
                    else -> MessageMediaUploadType.FILE
                }
            }
            .mapValues {
                it.value.map { media -> media.path }
            }
    }

    fun resetAllSelectMedia() {
        mAdapter.currentList
            .stream()
            .filter { media -> media.isSelected }
            .mapToInt { selected -> mAdapter.currentList.indexOf(selected) }
            .forEach { position -> mAdapter.changeItemSelectState(position, false) }
        mCallback?.onMediaClick(0)
    }
}
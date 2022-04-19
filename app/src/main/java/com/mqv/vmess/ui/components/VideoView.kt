package com.mqv.vmess.ui.components

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.mqv.vmess.R
import com.mqv.vmess.databinding.CustomVideoViewBinding
import com.mqv.vmess.network.model.Chat

typealias PlayClickListener = (Chat.Video) -> Unit

class VideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val mBinding = CustomVideoViewBinding.bind(inflate(context, R.layout.custom_video_view, this))
    private var mCallback: PlayClickListener? = null
    private var mVideo: Chat.Video? = null

    init {
        mBinding.buttonPlay.setOnClickListener { _ ->
            mCallback?.invoke(mVideo!!)
        }
    }

    fun setVideoResource(video: Chat.Video, indexOfMedia: Int) {
        mVideo = video

        // If thumbnail is not set this is mean the video is not completed yet
        if (video.thumbnail == null) {
            Glide.with(context)
                .load(video.uri)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(mBinding.imageVideoThumbnail.getThumbnailContainer())
        } else {
            mBinding.imageVideoThumbnail.setImageResource(Chat.Photo(video.thumbnail), indexOfMedia)
        }
    }

    fun setOnPlayListener(listener: PlayClickListener?) {
        mCallback = listener
    }

    fun setOnLongClickListener(callback: ImageLongClickListener?) {
        mBinding.imageVideoThumbnail.setImageLongClickListener(callback)
    }

    fun setMessage(chat: Chat) {
        mBinding.imageVideoThumbnail.setMessage(chat)
    }

    fun shouldShowPlayButton(isShow: Boolean) {
        mBinding.buttonPlay.visibility = if (isShow) VISIBLE else GONE
    }
}
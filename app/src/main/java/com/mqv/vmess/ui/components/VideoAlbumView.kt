package com.mqv.vmess.ui.components

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.mqv.vmess.R
import com.mqv.vmess.databinding.CustomVideoAlbumViewBinding
import com.mqv.vmess.network.model.Chat
import kotlin.math.min

class VideoAlbumView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val mBinding =
        CustomVideoAlbumViewBinding.bind(inflate(context, R.layout.custom_video_album_view, this))
    private val mMaxVideoView = 5
    private var mCurrentVideo = 0
    private var mChat: Chat? = null
    private var mPlayCallback: PlayClickListener? = null
    private var mLongVideoClick: ImageLongClickListener? = null

    fun setVideos(isReceived: Boolean, videos: List<Chat.Video>) {
        val size = min(videos.size, mMaxVideoView)

        if (size != mCurrentVideo) {
            mCurrentVideo = size
            inflateVideoView(size)
        }

        showVideoView(isReceived, videos)
    }

    fun setMessage(chat: Chat) {
        mChat = chat
    }

    fun setOnPlayListener(listener: PlayClickListener) {
        mPlayCallback = listener
    }

    fun setOnLongAlbumVideoClickListener(callback: ImageLongClickListener) {
        mLongVideoClick = callback
    }

    private fun inflateVideoView(size: Int) {
        val container = mBinding.albumCellContainer

        container.removeAllViews()

        when (size) {
            2 -> inflate(context, R.layout.custom_album_video_2, container)
            3 -> inflate(context, R.layout.custom_album_video_3, container)
            4 -> inflate(context, R.layout.custom_album_video_4, container)
            else -> inflate(context, R.layout.custom_album_video_many, container)
        }
    }

    private fun showVideoView(isReceived: Boolean, videos: List<Chat.Video>) {
        setVideo(0, videos[0], R.id.video_1, true)
        setVideo(1, videos[1], R.id.video_2, true)

        if (videos.size >= 3) {
            setVideo(2, videos[2], R.id.video_3, true)
        }

        if (videos.size >= 4) {
            setVideo(3, videos[3], R.id.video_4, true)
        }

        if (videos.size >= 5) {
            setVideo(4, videos[4], R.id.video_more, false)

            findViewById<TextView>(R.id.text_more)?.apply {
                text = context.getString(R.string.label_text_more_number, videos.size - 4)
            }
        }

        if (isReceived && videos.size == 3) {
            findViewById<VideoView>(R.id.video_3)?.apply {
                updateLayoutParams<ConstraintLayout.LayoutParams> {
                    startToStart = R.id.video_1
                    topToBottom = R.id.video_1
                    endToEnd = ConstraintLayout.LayoutParams.UNSET
                }
            }
        }
    }

    private fun setVideo(index: Int, video: Chat.Video, id: Int, showPlayButton: Boolean) {
        findViewById<VideoView>(id)?.apply {
            setVideoResource(video, index)
            setMessage(mChat!!)
            setOnPlayListener(mPlayCallback)
            setOnLongClickListener(mLongVideoClick)
            shouldShowPlayButton(showPlayButton)
        }
    }
}
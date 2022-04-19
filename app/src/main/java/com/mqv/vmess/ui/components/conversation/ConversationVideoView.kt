package com.mqv.vmess.ui.components.conversation

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.mqv.vmess.R
import com.mqv.vmess.databinding.CustomConversationVideoViewBinding
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.ui.components.ImageLongClickListener
import java.util.concurrent.Callable
import java.util.function.Consumer

class ConversationVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val mBinding = CustomConversationVideoViewBinding.bind(inflate(context, R.layout.custom_conversation_video_view, this))

    fun setVideoResource(isReceived: Boolean, chat: Chat) {
        mBinding.video.setMessage(chat)
        mBinding.videoAlbum.setMessage(chat)

        val videos = chat.videos

        if (videos.size > 1) {
            mBinding.video.visibility = View.GONE
            mBinding.videoAlbum.visibility = View.VISIBLE

            mBinding.videoAlbum.setVideos(isReceived, videos)
        } else {
            mBinding.video.visibility = View.VISIBLE
            mBinding.videoAlbum.visibility = View.GONE

            mBinding.video.setVideoResource(videos[0], 0)
        }
    }

    fun setOnPlayListener(callable: Consumer<Chat.Video>) {
        mBinding.video.setOnPlayListener {
            callable.accept(it)
        }

        mBinding.videoAlbum.setOnPlayListener {
            callable.accept(it)
        }
    }

    fun setOnThumbnailLongClickListener(callback: ImageLongClickListener) {
        mBinding.video.setOnLongClickListener(callback)
        mBinding.videoAlbum.setOnLongAlbumVideoClickListener(callback)
    }
}
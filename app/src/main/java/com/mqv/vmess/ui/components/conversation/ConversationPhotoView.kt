package com.mqv.vmess.ui.components.conversation

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.mqv.vmess.R
import com.mqv.vmess.databinding.CustomConversationPhotoViewBinding
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.ui.components.ImageClickListener
import com.mqv.vmess.ui.components.ImageLongClickListener

class ConversationPhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val mBinding: CustomConversationPhotoViewBinding =
        CustomConversationPhotoViewBinding.bind(
            inflate(
                context,
                R.layout.custom_conversation_photo_view,
                this
            )
        )

    fun setImageResource(isReceive: Boolean, chat: Chat) {
        val photos = chat.photos
        if (photos.size == 1) {
            val photo = photos[0]

            mBinding.thumbnail.setImageResource(photo, 0)

            mBinding.thumbnail.visibility = View.VISIBLE
            mBinding.album.visibility = View.GONE

            touchDelegate = mBinding.thumbnail.touchDelegate
        } else {
            mBinding.album.setPhotos(isReceive, photos)

            mBinding.thumbnail.visibility = View.GONE
            mBinding.album.visibility = View.VISIBLE

            touchDelegate = mBinding.album.touchDelegate
        }
        mBinding.album.setMessage(chat)
        mBinding.thumbnail.setMessage(chat)
    }

    fun setOnThumbnailClickListener(l: ImageClickListener?) {
        mBinding.thumbnail.setImageClickListener(l)
        mBinding.album.setOnThumbnailClickListener(l)
    }

    fun setOnThumbnailLongClickListener(l: ImageLongClickListener?) {
        mBinding.thumbnail.setImageLongClickListener(l)
        mBinding.album.setOnLongThumbnailClickListener(l)
    }
}
package com.mqv.vmess.ui.components

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.mqv.vmess.R
import com.mqv.vmess.databinding.CustomPhotoAlbumViewBinding
import com.mqv.vmess.network.model.Chat
import kotlin.math.min

class AlbumView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val maxDisplayPhotoSize = 5
    private val mBinding: CustomPhotoAlbumViewBinding =
        CustomPhotoAlbumViewBinding.bind(inflate(context, R.layout.custom_photo_album_view, this))
    private var mListPhoto: List<Chat.Photo>? = null
    private var mCurrentChat: Chat? = null
    private var mCurrentPhotos: Int = 0
    private var mThumbnailClickListener: ImageClickListener? = null
    private var mThumbnailLongClickListener: ImageLongClickListener? = null

    fun setPhotos(isReceive: Boolean, photos: List<Chat.Photo>) {
        mListPhoto = photos

        val size = min(photos.size, maxDisplayPhotoSize)

        if (size != mCurrentPhotos) {
            inflateAlbumLayout(size)
            mCurrentPhotos = size
        }

        showPhotosView(isReceive, photos)
    }

    fun setMessage(chat: Chat?) {
        mCurrentChat = chat
    }

    fun setOnThumbnailClickListener(l: ImageClickListener?) {
        mThumbnailClickListener = l
    }

    fun setOnLongThumbnailClickListener(l: ImageLongClickListener?) {
        mThumbnailLongClickListener = l
    }

    private fun inflateAlbumLayout(size: Int) {
        mBinding.albumCellContainer.removeAllViews()

        when (size) {
            2 -> inflate(context, R.layout.custom_album_thumbnail_2, mBinding.albumCellContainer)
            3 -> inflate(context, R.layout.custom_album_thumbnail_3, mBinding.albumCellContainer)
            4 -> inflate(context, R.layout.custom_album_thumbnail_4, mBinding.albumCellContainer)
            else -> inflate(
                context,
                R.layout.custom_album_thumbnail_many,
                mBinding.albumCellContainer
            )
        }
    }

    private fun showPhotosView(isReceive: Boolean, photos: List<Chat.Photo>) {
        setPhoto(photos[0], R.id.thumbnail_1)
        setPhoto(photos[1], R.id.thumbnail_2)

        if (photos.size >= 3) {
            setPhoto(photos[2], R.id.thumbnail_3)
        }

        if (photos.size >= 4) {
            setPhoto(photos[3], R.id.thumbnail_4)
        }

        if (photos.size >= 5) {
            setPhoto(photos[4], R.id.thumbnail_more)

            findViewById<TextView>(R.id.text_more)?.apply {
                text = context.getString(R.string.label_text_more_number, photos.size - 4)
            }
        }

        if (isReceive && photos.size == 3) {
            findViewById<PhotoView>(R.id.thumbnail_3)?.apply {
                updateLayoutParams<ConstraintLayout.LayoutParams> {
                    startToStart = R.id.thumbnail_1
                    topToBottom = R.id.thumbnail_1
                    endToEnd = ConstraintLayout.LayoutParams.UNSET
                }
            }
        }
    }

    private fun setPhoto(photo: Chat.Photo, id: Int) {
        findViewById<PhotoView>(id)?.apply {
            setImageResource(photo, mListPhoto!!.indexOf(photo))
            setImageClickListener(mThumbnailClickListener)
            setImageLongClickListener(mThumbnailLongClickListener)
            setMessage(mCurrentChat)
        }
    }
}
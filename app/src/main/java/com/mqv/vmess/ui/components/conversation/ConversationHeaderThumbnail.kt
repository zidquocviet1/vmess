package com.mqv.vmess.ui.components.conversation

import android.content.Context
import android.content.res.Resources.getSystem
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.mqv.vmess.R
import com.mqv.vmess.databinding.ItemConversationHeaderThumbnailBinding
import com.mqv.vmess.util.Picture

class ConversationHeaderThumbnail @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val mBinding = ItemConversationHeaderThumbnailBinding.bind(inflate(context, R.layout.item_conversation_header_thumbnail, this))
    private val Int.dp: Int get() = (this / getSystem().displayMetrics.density).toInt()
    private var mIsMultiple: Boolean = false

    fun setThumbnail(thumbnails: List<String?>) {
        if (thumbnails.size > 1) {
            mIsMultiple = true

            setMultipleThumbnail(thumbnails)
        } else {
            mIsMultiple = false

            setSingleThumbnail(thumbnails[0])
        }
    }

    fun setActiveStatus(isActive: Boolean) {
        if (mIsMultiple) {
            mBinding.imageActive.updateLayoutParams<ConstraintLayout.LayoutParams> {
                bottomToBottom = R.id.layout_multiple_thumbnail
                endToEnd = R.id.layout_multiple_thumbnail
                marginEnd = 2.dp
            }
        } else {
            mBinding.imageActive.updateLayoutParams<ConstraintLayout.LayoutParams> {
                bottomToBottom = R.id.image_single_avatar
                endToEnd = R.id.image_single_avatar
            }
        }
        mBinding.imageActive.visibility = if (isActive) View.VISIBLE else View.GONE
    }

    fun showHeader(isShow: Boolean, isActive: Boolean) {
        if (isShow) {
            setActiveStatus(isActive)

            mBinding.imageSingleAvatar.visibility = if (mIsMultiple) INVISIBLE else VISIBLE
            mBinding.layoutMultipleThumbnail.visibility = if (mIsMultiple) VISIBLE else INVISIBLE
        } else {
            mBinding.imageActive.visibility = INVISIBLE
            mBinding.imageSingleAvatar.visibility = INVISIBLE
            mBinding.layoutMultipleThumbnail.visibility = INVISIBLE
        }
    }

    fun setSingleThumbnailSize(width: Int, height: Int) {
        mBinding.imageSingleAvatar.layoutParams = ViewGroup.LayoutParams(width, height)
        mBinding.imageSingleAvatar.requestLayout()
    }

    private fun setSingleThumbnail(photoUrl: String?) {
        mBinding.imageSingleAvatar.visibility = VISIBLE
        mBinding.layoutMultipleThumbnail.visibility = View.GONE

        loadUserImage(photoUrl, mBinding.imageSingleAvatar)
    }

    private fun setMultipleThumbnail(photoUrls: List<String?>) {
        mBinding.imageSingleAvatar.visibility = View.GONE
        mBinding.layoutMultipleThumbnail.visibility = VISIBLE

        loadUserImage(photoUrls[0], mBinding.imageMultipleAvatar1)
        loadUserImage(photoUrls[1], mBinding.imageMultipleAvatar2)
    }

    private fun loadUserImage(url: String?, imageView: ImageView) {
        Picture.loadUserAvatar(context, url).into(imageView)
    }
}
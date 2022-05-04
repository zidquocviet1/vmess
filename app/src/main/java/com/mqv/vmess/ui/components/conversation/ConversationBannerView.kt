package com.mqv.vmess.ui.components.conversation

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.mqv.vmess.R
import com.mqv.vmess.databinding.ItemConversationBannerBinding
import com.mqv.vmess.network.model.User
import com.mqv.vmess.ui.data.ConversationMetadata
import com.mqv.vmess.util.Picture
import com.mqv.vmess.util.views.ViewUtil.dp
import com.mqv.vmess.util.views.ViewUtil.px

class ConversationBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleArray: Int = 0
) : FrameLayout(context, attrs, defStyleArray) {
    private val mBinding = ItemConversationBannerBinding.bind(
        inflate(
            context,
            R.layout.item_conversation_banner,
            this
        )
    )
    private var mRunnable: Runnable? = null
    private var mIsSingle: Boolean = false

    init {
        mBinding.layoutGroupThumbnail.setOnClickListener {
            mRunnable?.run()
        }
    }

    fun setMetadata(metadata: ConversationMetadata) {
        setThumbnail(metadata.conversationParticipants, metadata.conversationThumbnail)
        setName(metadata.conversationName)
    }

    fun setOnThumbnailClickListener(runnable: Runnable) {
        mRunnable = runnable
    }

    fun setSingleThumbnailSize(width: Int, height: Int) {
        if (mIsSingle) {
            mBinding.layoutAvatar3.layoutParams = LinearLayout.LayoutParams(width, height)
            mBinding.layoutAvatar3.requestLayout()
        }
    }

    private fun setName(name: String) {
        mBinding.textGroupName.text = name
    }

    private fun setThumbnail(participants: List<User>, thumbnails: List<String?>) {
        if (thumbnails.isEmpty())
            throw IllegalArgumentException()

        if (thumbnails.size > 1) {
            mIsSingle = false

            setMultipleThumbnail(participants, thumbnails)
        } else {
            mIsSingle = true

            setSingleThumbnail(thumbnails[0])
        }
    }

    private fun setSingleThumbnail(url: String?) {
        Picture.loadUserAvatar(context, url).into(mBinding.imageAvatar3)

        mBinding.layoutAvatar3.setPadding(0, 0, 0, 0)
        mBinding.layoutAvatar3.isClickable = false
        mBinding.layoutAvatar2.visibility = GONE
        mBinding.layoutAvatar1.visibility = GONE
        mBinding.textMoreNumber.visibility = GONE
    }

    private fun setMultipleThumbnail(participants: List<User>, thumbnails: List<String?>) {
        val imageSize = getImageSizeForMultipleThumbnail()

        mBinding.layoutAvatar3.setPadding(3.px, 3.px, 3.px, 3.px)
        mBinding.layoutAvatar3.layoutParams = LinearLayout.LayoutParams(imageSize, imageSize)
        mBinding.layoutAvatar3.requestLayout()

        Picture.loadUserAvatar(context, thumbnails[0])
            .into(mBinding.imageAvatar3)
        Picture.loadUserAvatar(context, thumbnails[1])
            .into(mBinding.imageAvatar2)

        mBinding.layoutAvatar2.visibility = VISIBLE

        when (participants.size) {
            3 -> mBinding.layoutAvatar1.visibility = GONE
            4 -> {
                Picture.loadUserAvatar(context, thumbnails[2]).into(mBinding.imageAvatar1)
                mBinding.layoutAvatar1.visibility = VISIBLE
                mBinding.textMoreNumber.visibility = GONE
            }
            else -> {
                mBinding.layoutAvatar1.visibility = VISIBLE
                mBinding.imageAvatar1.visibility = VISIBLE
                mBinding.imageAvatar1.setImageDrawable(Picture.getErrorAvatarLoaded(context))
                mBinding.textMoreNumber.visibility = VISIBLE
                mBinding.textMoreNumber.text = context.getString(
                    R.string.label_text_more_number,
                    participants.size - 3
                )
            }
        }
    }

    private fun getImageSizeForMultipleThumbnail(): Int =
        context.resources.getDimensionPixelSize(R.dimen.image_conversation)
}
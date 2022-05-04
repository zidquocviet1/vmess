package com.mqv.vmess.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import com.mqv.vmess.R
import com.mqv.vmess.databinding.CustomLinkPreviewBinding
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.ui.components.linkpreview.LinkPreviewMetadata
import com.mqv.vmess.ui.data.ConversationMessageItem
import com.mqv.vmess.util.Picture

class LinkPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleArray: Int = 0
) : FrameLayout(context, attrs, defStyleArray) {
    private val mBinding =
        CustomLinkPreviewBinding.bind(inflate(context, R.layout.custom_link_preview, this))

    fun setLinkPreview(isReceived: Boolean, message: Chat, metadata: LinkPreviewMetadata) {
        mBinding.textContent.text = message.content

        if (isReceived) {
            mBinding.divider.visibility = View.VISIBLE
            mBinding.messageBackground.alpha = .7f
            mBinding.messageBackground.backgroundTintList = ColorStateList.valueOf(getColor(R.color.base_background_color))
            mBinding.textContent.setTextColor(getColor(R.color.black))
        } else {
            mBinding.divider.visibility = View.GONE
            mBinding.messageBackground.backgroundTintList = ConversationMessageItem.sChatColor
            mBinding.textContent.setTextColor(getColor(R.color.white))
        }

        if (metadata.isLoadComplete) {
            setLoadingComplete()
            showDetail(metadata)
        } else {
            setLoadingPreview()
        }

        if (metadata.isNoPreview) {
            setNoPreview()
        }
    }

    private fun setLoadingPreview() {
        mBinding.progressBarLoading.visibility = View.VISIBLE

        mBinding.linkpreviewThumbnail.visibility = View.INVISIBLE
        mBinding.linkpreviewTitle.visibility = View.INVISIBLE
        mBinding.linkpreviewDescription.visibility = View.INVISIBLE
        mBinding.linkpreviewSite.visibility = View.INVISIBLE
    }

    private fun setLoadingComplete() {
        mBinding.progressBarLoading.visibility = View.GONE

        mBinding.linkpreviewThumbnail.visibility = View.VISIBLE
        mBinding.linkpreviewTitle.visibility = View.VISIBLE
        mBinding.linkpreviewDescription.visibility = View.VISIBLE
        mBinding.linkpreviewSite.visibility = View.VISIBLE
    }

    private fun setNoPreview() {
        mBinding.progressBarLoading.visibility = View.GONE
        mBinding.linkpreviewTitle.visibility = View.GONE
        mBinding.linkpreviewDescription.visibility = View.GONE
        mBinding.linkpreviewSite.visibility = View.GONE

        showNoPreview()
    }

    private fun showDetail(metadata: LinkPreviewMetadata) {
        mBinding.linkpreviewTitle.text = metadata.title
        mBinding.linkpreviewDescription.text = metadata.description
        mBinding.linkpreviewSite.text = metadata.siteName

        if (metadata.title.isEmpty()) {
            mBinding.linkpreviewTitle.visibility = View.GONE
        }

        if (metadata.description.isEmpty()) {
            mBinding.linkpreviewDescription.visibility = View.GONE
        }

        if (metadata.siteName.isEmpty()) {
            mBinding.linkpreviewSite.visibility = View.GONE
        }

        if (metadata.imageUrl.isEmpty()) {
            showNoPreview()
        } else {
            Picture.loadUserAvatar(context, metadata.imageUrl).centerCrop().into(mBinding.linkpreviewThumbnail)
        }
    }

    private fun showNoPreview() {
        mBinding.linkpreviewThumbnail.visibility = View.VISIBLE
        mBinding.linkpreviewThumbnail.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_round_no_photography))
        mBinding.linkpreviewThumbnail.scaleType = ImageView.ScaleType.CENTER
    }

    private fun getColor(colorRes: Int): Int {
        return ContextCompat.getColor(context, colorRes)
    }
}
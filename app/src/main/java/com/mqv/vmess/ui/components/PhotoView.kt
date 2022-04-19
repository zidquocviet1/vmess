package com.mqv.vmess.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.AttrRes
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.mqv.vmess.R
import com.mqv.vmess.databinding.CustomPhotoViewBinding
import com.mqv.vmess.dependencies.AppDependencies
import com.mqv.vmess.di.GlideRequest
import com.mqv.vmess.network.model.Chat
import com.mqv.vmess.util.FileProviderUtil
import com.mqv.vmess.util.Picture
import java.io.File

class PhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleArray: Int = 0
) : FrameLayout(context, attrs, defStyleArray) {

    private val mBinding: CustomPhotoViewBinding =
        CustomPhotoViewBinding.bind(inflate(context, R.layout.custom_photo_view, this))
    private var mPhoto: Chat.Photo? = null
    private var mChat: Chat? = null
    private var mIndexOfMedia: Int = -1

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
    }

    fun getThumbnailContainer(): ImageView = mBinding.thumbnail

    fun setImageResource(photo: Chat.Photo, indexOfMedia: Int) {
        mPhoto = photo
        mIndexOfMedia = indexOfMedia

        val glideRequest: GlideRequest<Drawable> =
            if (FileProviderUtil.isLocalFile(photo.uri)) {
                Picture.loadLocalFile(context, File(photo.uri))
            } else {
                Picture.loadSecureResource(
                    AppDependencies.getAppPreferences().userAuthToken.orElse(""),
                    context,
                    photo.uri
                ).apply(RequestOptions.fitCenterTransform())
            }

        glideRequest.transition(DrawableTransitionOptions.withCrossFade())
            .into(mBinding.thumbnail)
    }

    fun setImageClickListener(l: ImageClickListener?) {
        mBinding.thumbnail.setOnClickListener { v ->
            l?.let {
                mPhoto?.let { photo ->
                    v.tag = mChat
                    it.onClick(v, photo)
                }
            }
        }
    }

    fun setImageLongClickListener(l: ImageLongClickListener?) {
        mBinding.thumbnail.setOnLongClickListener { v ->
            mPhoto?.run {
                v.tag = mChat
                l?.onLongClick(v, mIndexOfMedia)
                return@setOnLongClickListener true
            }
            return@setOnLongClickListener false
        }
    }

    fun setMessage(chat: Chat?) {
        mChat = chat
    }
}

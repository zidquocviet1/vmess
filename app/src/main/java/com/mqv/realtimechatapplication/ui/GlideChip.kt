package com.mqv.realtimechatapplication.ui

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.chip.Chip
import com.mqv.realtimechatapplication.R
import com.mqv.realtimechatapplication.util.Picture

class GlideChip(context: Context) : Chip(context) {
    init {
        chipStartPadding = 10f
        chipEndPadding = 10f
        chipIconSize = context.resources.getDimension(R.dimen.image_user_avatar_extra_small)
        minHeight = 40
        isCloseIconVisible = true
    }

    fun setIconUrl(photoUrl: String?): GlideChip {
        Picture.loadUserAvatar(context, photoUrl)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    chipIcon = if (model != null) {
                        Picture.createRoundedDrawable(
                            context,
                            Picture.getErrorAvatarLoaded(context),
                            Picture.DEFAULT_IMAGE_WIDTH,
                            Picture.DEFAULT_IMAGE_HEIGHT
                        )
                    } else {
                        // Fallback
                        Picture.getDefaultUserAvatar(context)
                    }
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    chipIcon = resource
                    return true
                }
            }).preload()
        return this
    }
}
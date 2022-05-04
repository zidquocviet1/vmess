package com.mqv.vmess.util.views

import android.content.res.Resources
import android.content.res.Resources.getSystem
import android.view.View
import com.mqv.vmess.R

object ViewUtil {
    val Int.dp: Int get() = (this / getSystem().displayMetrics.density).toInt()

    val Int.px: Int get() = (this * getSystem().displayMetrics.density).toInt()

    fun View.updateSize(width: Int, height: Int) {
        this.layoutParams.width = width
        this.layoutParams.height = height
        this.requestLayout()
    }

    @JvmStatic
    fun getLargeUserAvatarPixel(resources: Resources): Int =
        resources.getDimensionPixelSize(R.dimen.image_user_avatar_large)

    @JvmStatic
    fun getMediumUserAvatarPixel(resources: Resources): Int =
        resources.getDimensionPixelSize(R.dimen.image_user_avatar_medium)

    @JvmStatic
    fun getSmallUserAvatarPixel(resources: Resources): Int =
        resources.getDimensionPixelSize(R.dimen.image_user_avatar_small)

    @JvmStatic
    fun getExtraSmallUserAvatarPixel(resources: Resources): Int =
        resources.getDimensionPixelSize(R.dimen.image_user_avatar_extra_small)
}
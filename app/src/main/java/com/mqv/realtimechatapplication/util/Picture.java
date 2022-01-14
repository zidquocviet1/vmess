package com.mqv.realtimechatapplication.util;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.di.GlideApp;
import com.mqv.realtimechatapplication.di.GlideRequest;

import javax.annotation.Nullable;

/*
Util class to load picture with Glide and added some default drawable
*/
public class Picture {
    private static final int    DEFAULT_LOAD_FAILED = R.color.base_background_color;

    // Remove the host name of the photo URL
    private static String reformatUrl(@Nullable String photoUrl) {
        return photoUrl == null ? null : photoUrl.replace("localhost", Const.BASE_IP);
    }

    public static GlideRequest<Drawable> loadUserAvatar(Fragment fragment, @Nullable String url) {
        Context context = fragment.requireContext();

        return GlideApp.with(context)
                       .load(reformatUrl(url))
                       .error(getErrorAvatarLoaded(context))
                       .fallback(getDefaultUserAvatar(context))
                       .circleCrop()
                       .diskCacheStrategy(DiskCacheStrategy.ALL);
    }

    public static GlideRequest<Drawable> loadUserAvatar(Context context, @Nullable String url) {
        return GlideApp.with(context)
                       .load(reformatUrl(url))
                       .error(getErrorAvatarLoaded(context))
                       .fallback(getDefaultUserAvatar(context))
//                       .transition(DrawableTransitionOptions.withCrossFade())
                       .circleCrop()
                       .diskCacheStrategy(DiskCacheStrategy.ALL);
    }

    public static GlideRequest<Drawable> loadUserAvatarWithPlaceHolder(Context context, @Nullable String url) {
        return GlideApp.with(context)
                       .load(reformatUrl(url))
                       .error(getErrorAvatarLoaded(context))
                       .fallback(getDefaultUserAvatar(context))
                       .circleCrop()
                       .placeholder(getDefaultCirclePlaceHolder(context))
                       .diskCacheStrategy(DiskCacheStrategy.ALL);
    }

    private static CircularProgressDrawable getDefaultCirclePlaceHolder(Context context) {
        var placeHolder = new CircularProgressDrawable(context);
        placeHolder.setStrokeWidth(5f);
        placeHolder.setCenterRadius(30f);
        placeHolder.start();

        return placeHolder;
    }

    public static Drawable getErrorAvatarLoaded(Context context) {
        return new ColorDrawable(ContextCompat.getColor(context, DEFAULT_LOAD_FAILED));
    }

    public static Drawable getDefaultUserAvatar(Context context) {
        return ContextCompat.getDrawable(context, R.drawable.ic_default_user_avatar);
    }
}

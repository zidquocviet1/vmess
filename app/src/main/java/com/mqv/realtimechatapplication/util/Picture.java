package com.mqv.realtimechatapplication.util;

import android.content.Context;

import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.di.GlideApp;
import com.mqv.realtimechatapplication.di.GlideRequest;

/*
Util class to load picture with Glide and added some default drawable
*/
public class Picture {
    public static GlideRequest defaultUser(Context context, String url) {
        return GlideApp.with(context)
                .load(url)
                .error(R.drawable.ic_account_undefined)
                .fallback(R.drawable.ic_round_account)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL);
    }

    public static GlideRequest defaultUserWithPlaceHolder(Context context, String url) {
        return GlideApp.with(context)
                .load(url)
                .error(R.drawable.ic_account_undefined)
                .fallback(R.drawable.ic_round_account)
                .circleCrop()
                .placeholder(getDefaultCirclePlaceHolder(context))
                .diskCacheStrategy(DiskCacheStrategy.ALL);
    }

    public static CircularProgressDrawable getDefaultCirclePlaceHolder(Context context) {
        var placeHolder = new CircularProgressDrawable(context);
        placeHolder.setStrokeWidth(5f);
        placeHolder.setCenterRadius(30f);
        placeHolder.start();

        return placeHolder;
    }
}

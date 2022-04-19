package com.mqv.vmess.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.mqv.vmess.R;
import com.mqv.vmess.dependencies.AppDependencies;
import com.mqv.vmess.di.GlideApp;
import com.mqv.vmess.di.GlideRequest;

import java.io.File;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

/*
Util class to load picture with Glide and added some default drawable
*/
public class Picture {
    private static final int    DEFAULT_LOAD_FAILED = R.color.base_background_color;
    public static final int DEFAULT_IMAGE_HEIGHT = 88;
    public static final int DEFAULT_IMAGE_WIDTH = 88;

    // Remove the host name of the photo URL
    public static GlideRequest<Drawable> loadUserAvatar(Fragment fragment, @Nullable String url) {
        Context context = fragment.requireContext();

        if (url == null) {
            return GlideApp.with(context)
                           .load((String) null)
                           .error(getErrorAvatarLoaded(context))
                           .fallback(getDefaultUserAvatar(context))
                           .circleCrop()
                           .diskCacheStrategy(DiskCacheStrategy.ALL);
        } else {
            return loadSecureResource(AppDependencies.getAppPreferences().getUserAuthToken().orElse(""), context, url);
        }
    }

    public static String reformatUrl(@Nullable String photoUrl) {
        return photoUrl == null ? null : photoUrl.replace("localhost", Const.BASE_IP);
    }

    public static GlideRequest<Drawable> loadUserAvatar(Context context, @Nullable String url) {
        if (url == null) {
            return GlideApp.with(context)
                           .load((String) null)
                           .error(getErrorAvatarLoaded(context))
                           .fallback(getDefaultUserAvatar(context))
//                              .transition(DrawableTransitionOptions.withCrossFade())
                           .circleCrop()
                           .diskCacheStrategy(DiskCacheStrategy.ALL);
        } else {
            return loadSecureResource(AppDependencies.getAppPreferences().getUserAuthToken().orElse(""), context, url);
        }
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

    public static GlideRequest<Drawable> loadSecureResource(String token, Context context, @NonNull String url) {
        GlideUrl glideUrl = new GlideUrl(reformatUrl(url),
                                         new LazyHeaders.Builder()
                                                        .addHeader(Const.AUTHORIZATION, Const.PREFIX_TOKEN + token)
                                                        .build());

        return GlideApp.with(context)
                       .load(glideUrl)
                       .error(getErrorAvatarLoaded(context))
                       .fallback(getDefaultUserAvatar(context))
                       .circleCrop()
                       .placeholder(getErrorAvatarLoaded(context))
                       .diskCacheStrategy(DiskCacheStrategy.ALL);
    }

    public static GlideRequest<Drawable> loadLocalFile(Context context, @NonNull File file) {
        return GlideApp.with(context)
                       .load(file)
                       .error(getErrorAvatarLoaded(context))
                       .fallback(getDefaultUserAvatar(context))
                       .placeholder(getErrorAvatarLoaded(context))
                       .diskCacheStrategy(DiskCacheStrategy.ALL);
    }

    @WorkerThread
    public static Bitmap loadUserAvatarIntoBitmap(Context context, @Nullable  String url) {
        try {
            return GlideApp.with(context)
                           .asBitmap()
                           .load(reformatUrl(url))
                           .error(getErrorAvatarLoaded(context))
                           .fallback(getDefaultUserAvatar(context))
                           .circleCrop()
                           .diskCacheStrategy(DiskCacheStrategy.ALL)
                           .submit()
                           .get();
        } catch (ExecutionException | InterruptedException e) {
            return null;
        }
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

    public static Bitmap createBitmapFromDrawable(Drawable d, int width, int height) {
        Bitmap destBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(destBitmap);

        d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        d.draw(canvas);

        return destBitmap;
    }

    public static Drawable createRoundedDrawable(Context context, Drawable resource, int width, int height) {
        Bitmap destBitmap = createBitmapFromDrawable(resource, width, height);
        RoundedBitmapDrawable rbd = RoundedBitmapDrawableFactory.create(context.getResources(), destBitmap);
        rbd.setCornerRadius(Math.max(destBitmap.getHeight(), destBitmap.getWidth()) / 2.0f);
        return rbd;
    }
}

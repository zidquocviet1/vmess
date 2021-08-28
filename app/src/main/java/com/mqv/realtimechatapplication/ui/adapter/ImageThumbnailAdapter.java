package com.mqv.realtimechatapplication.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.ItemProfileImageThumbnailBinding;
import com.mqv.realtimechatapplication.ui.data.ImageThumbnail;

public class ImageThumbnailAdapter extends
        ListAdapter<ImageThumbnail, ImageThumbnailAdapter.ImageThumbnailViewHolder> {
    private final Context mContext;
    public static final int IMAGE_WIDTH = 120*2;
    public static final int IMAGE_HEIGHT = 120*2;

    public ImageThumbnailAdapter(Context context) {
        super(new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull ImageThumbnail oldItem, @NonNull ImageThumbnail newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull ImageThumbnail oldItem, @NonNull ImageThumbnail newItem) {
                return oldItem.getId().equals(newItem.getId()) &&
                        oldItem.getDisplayName().equals(newItem.getDisplayName()) &&
                        oldItem.getSize().equals(newItem.getSize()) &&
                        oldItem.getTimestamp().equals(newItem.getTimestamp()) &&
                        oldItem.getContentUri().equals(newItem.getContentUri());
            }
        });
        mContext = context;
    }

    @NonNull
    @Override
    public ImageThumbnailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var view = LayoutInflater.from(mContext).inflate(R.layout.item_profile_image_thumbnail, parent, false);
        return new ImageThumbnailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageThumbnailViewHolder holder, int position) {
        var item = getItem(position);
        holder.bind(item, mContext);
    }

    public static class ImageThumbnailViewHolder extends RecyclerView.ViewHolder {
        ItemProfileImageThumbnailBinding mBinding;

        public ImageThumbnailViewHolder(@NonNull View itemView) {
            super(itemView);
            mBinding = ItemProfileImageThumbnailBinding.bind(itemView);
        }

        public void bind(ImageThumbnail item, Context context) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                try {
////                    var thumbnail = context.getContentResolver().loadThumbnail(
////                            item.getContentUri(),
////                            new Size(480, 480),
////                            null);
//
//                    Glide.with(context)
//                            .asBitmap()
//                            .override(IMAGE_WIDTH, IMAGE_HEIGHT)
//                            .load(thumbnail)
//                            .centerCrop()
//                            .transition(BitmapTransitionOptions.withCrossFade())
//                            .error(AppCompatResources.getDrawable(context, R.drawable.ic_round_account))
//                            .into(mBinding.imageThumbnail);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }

            if (item.getThumbnail() != null){
                mBinding.imageThumbnail.setImageBitmap(item.getThumbnail());
            }else{
                mBinding.imageThumbnail.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_image_not_supported));
            }
        }
    }
}

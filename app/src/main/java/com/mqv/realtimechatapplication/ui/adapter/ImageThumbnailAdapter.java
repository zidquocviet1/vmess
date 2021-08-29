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
import com.mqv.realtimechatapplication.util.Logging;

import java.util.function.Consumer;

public class ImageThumbnailAdapter extends
        ListAdapter<ImageThumbnail, ImageThumbnailAdapter.ImageThumbnailViewHolder> {
    private final Context mContext;
    private final int mRealWidth, mColumn, mSpacing;
    private Consumer<ImageThumbnail> callback;

    public ImageThumbnailAdapter(Context context, int realWidth, int column, int spacing) {
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
        mRealWidth = realWidth;
        mColumn = column;
        mSpacing = spacing;
    }

    public void setOnItemClick(Consumer<ImageThumbnail> callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public ImageThumbnailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var thumbnailWidth = (mRealWidth - (mColumn - 1) * mSpacing) / mColumn;
        var thumbnailHeight = thumbnailWidth;

        var view = LayoutInflater.from(mContext).inflate(R.layout.item_profile_image_thumbnail, parent, false);
        var viewHolder = new ImageThumbnailViewHolder(view);

        viewHolder.mBinding.imageThumbnail.getLayoutParams().width = thumbnailWidth;
        viewHolder.mBinding.imageThumbnail.getLayoutParams().height = thumbnailHeight;
        return new ImageThumbnailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageThumbnailViewHolder holder, int position) {
        var item = getItem(position);
        holder.bind(item, mContext);
        holder.itemView.setOnClickListener(v -> {
            if (callback != null){
                callback.accept(item);
            }
        });
    }

    public static class ImageThumbnailViewHolder extends RecyclerView.ViewHolder {
        public ItemProfileImageThumbnailBinding mBinding;

        public ImageThumbnailViewHolder(@NonNull View itemView) {
            super(itemView);
            mBinding = ItemProfileImageThumbnailBinding.bind(itemView);
        }

        public void bind(ImageThumbnail item, Context context) {
            if (item.getThumbnail() != null) {
                mBinding.imageThumbnail.setImageBitmap(item.getThumbnail());
            } else {
                mBinding.imageThumbnail.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_image_not_supported));
            }
        }
    }
}

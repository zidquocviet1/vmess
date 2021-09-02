package com.mqv.realtimechatapplication.ui.adapter;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.ItemCameraPreviewBinding;
import com.mqv.realtimechatapplication.databinding.ItemProfileImageThumbnailBinding;
import com.mqv.realtimechatapplication.ui.data.ImageThumbnail;

import java.util.function.Consumer;

public class ImageThumbnailAdapter extends
        ListAdapter<ImageThumbnail, RecyclerView.ViewHolder> {
    private final Context mContext;
    private final int mRealWidth, mColumn, mSpacing;
    private Consumer<ImageThumbnail> thumbnailConsumer;
    private Consumer<Void> cameraConsumer;

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

    public void setOnThumbnailClick(Consumer<ImageThumbnail> callback) {
        this.thumbnailConsumer = callback;
    }

    public void setOnCameraPreviewClick(Consumer<Void> cameraConsumer) {
        this.cameraConsumer = cameraConsumer;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var thumbnailWidth = (mRealWidth - (mColumn - 1) * mSpacing) / mColumn;
        var thumbnailHeight = thumbnailWidth;

        if (viewType == 0) {
            var view = LayoutInflater.from(mContext).inflate(R.layout.item_camera_preview, parent, false);
            var viewHolder = new CameraPreviewViewHolder(view);

            viewHolder.mBinding.layoutMain.getLayoutParams().width = thumbnailWidth;
            viewHolder.mBinding.layoutMain.getLayoutParams().height = thumbnailHeight;
            return viewHolder;
        } else {
            var view = LayoutInflater.from(mContext).inflate(R.layout.item_profile_image_thumbnail, parent, false);
            var viewHolder = new ImageThumbnailViewHolder(view);

            viewHolder.mBinding.imageThumbnail.getLayoutParams().width = thumbnailWidth;
            viewHolder.mBinding.imageThumbnail.getLayoutParams().height = thumbnailHeight;
            return viewHolder;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == 0) {
            var cameraHolder = (CameraPreviewViewHolder) holder;
            if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY))
                return;

            cameraHolder.bindCameraPreview(mContext);
            cameraHolder.itemView.setOnClickListener(v -> {
                if (cameraConsumer != null) cameraConsumer.accept(null);
            });
        } else {
            var item = getItem(position);
            var thumbnailHolder = (ImageThumbnailViewHolder) holder;

            thumbnailHolder.bind(item, mContext);
            thumbnailHolder.itemView.setOnClickListener(v -> {
                if (thumbnailConsumer != null) thumbnailConsumer.accept(item);
            });
        }
    }

    static class ImageThumbnailViewHolder extends RecyclerView.ViewHolder {
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

    static class CameraPreviewViewHolder extends RecyclerView.ViewHolder{
        public ItemCameraPreviewBinding mBinding;

        public CameraPreviewViewHolder(@NonNull View itemView) {
            super(itemView);
            mBinding = ItemCameraPreviewBinding.bind(itemView);
        }

        public void bindCameraPreview(Context context) {
            mBinding.imageAddPhoto.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_add_a_photo));
        }
    }
}

package com.mqv.realtimechatapplication.activity;

import static com.mqv.realtimechatapplication.activity.EditProfileActivity.EXTRA_CHANGE_PHOTO;
import static com.mqv.realtimechatapplication.activity.EditProfileActivity.EXTRA_IMAGE_THUMBNAIL;

import android.content.ContentUris;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.AndroidViewModel;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.ActivitySelectPhotoBinding;
import com.mqv.realtimechatapplication.ui.adapter.ImageThumbnailAdapter;
import com.mqv.realtimechatapplication.ui.data.ImageThumbnail;
import com.mqv.realtimechatapplication.util.Logging;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SelectPhotoActivity extends ToolbarActivity<AndroidViewModel, ActivitySelectPhotoBinding> {
    private static final int NUM_THUMBNAIL_PORTRAIT_COLUMN = 3;
    private static final int NUM_THUMBNAIL_LANDSCAPE_COLUMN = 9;
    private static final int NUM_SPACING = 9; // px

    @Override
    public void binding() {
        mBinding = ActivitySelectPhotoBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<AndroidViewModel> getViewModelClass() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.label_select_photo);

        var dimension = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dimension);
        int width = dimension.widthPixels;

        String extra = getIntent().getStringExtra(EXTRA_CHANGE_PHOTO);

        var images = getAllPhotoFromExternal();

        if (images != null) {
            var spanCount = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
                    ? NUM_THUMBNAIL_PORTRAIT_COLUMN : NUM_THUMBNAIL_LANDSCAPE_COLUMN; // columns
            var adapter = new ImageThumbnailAdapter(this, width, spanCount, NUM_SPACING);
            adapter.submitList(images);
            adapter.setOnItemClick(imageThumbnail -> {
                Logging.show("Thumbnail is clicked, Uri = " + imageThumbnail.getContentUri());
                if (imageThumbnail.getSize() != 0 && imageThumbnail.getContentUri() != null) {
                    var intent = new Intent(this, PreviewEditPhotoActivity.class);
                    intent.putExtra(EXTRA_CHANGE_PHOTO, extra);
                    intent.putExtra(EXTRA_IMAGE_THUMBNAIL, imageThumbnail);
                    startActivity(intent);
                }
            });

            mBinding.recyclerViewPhotos.setAdapter(adapter);
            mBinding.recyclerViewPhotos.setLayoutManager(new GridLayoutManager(this, spanCount));
            mBinding.recyclerViewPhotos.addItemDecoration(new GridSpacingItemDecoration(spanCount, NUM_SPACING, false));
        }
    }

    @Override
    public void setupObserver() {
        // default implementation method
    }

    @RequiresApi(29)
    private List<ImageThumbnail> getImagesShownInApi29() {
        List<ImageThumbnail> images = null;
        var uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        var projection = new String[]{
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_TAKEN,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.RELATIVE_PATH,
                MediaStore.MediaColumns.DATA
        };
        String imageDateSort = MediaStore.MediaColumns.DATE_TAKEN + " DESC";

        var cursor = getContentResolver().query(uri, projection, null, null, imageDateSort);

        if (cursor != null && cursor.getCount() > 0) {
            images = new ArrayList<>();

            var idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
            var nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
            var sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE);
            var dateIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN);
            var typeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);
            var pathIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH);
            var dataIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

            while (cursor.moveToNext()) {
                var id = cursor.getLong(idIndex);
                var name = cursor.getString(nameIndex);
                var size = cursor.getString(sizeIndex);
                var date = cursor.getString(dateIndex);
                var type = cursor.getString(typeIndex);
                var relativePath = cursor.getString(pathIndex);
                var realPath = cursor.getString(dataIndex);

                var contentUri = ContentUris.withAppendedId(uri, id);
                Bitmap thumbnail = null;
                try {
                    thumbnail = getContentResolver().loadThumbnail(contentUri, new Size(480, 480), null);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                LocalDateTime timestamp;

                try {
                    timestamp = LocalDateTime.parse(date);
                } catch (DateTimeParseException e) {
                    timestamp = LocalDateTime.MIN;
                }

                images.add(new ImageThumbnail(
                        id,
                        name,
                        Long.parseLong(size),
                        timestamp,
                        contentUri,
                        thumbnail,
                        type,
                        relativePath,
                        realPath
                ));
            }

            cursor.close();
        }
        return images;
    }

    private List<ImageThumbnail> getImagesShownInApiLower29() {
        List<ImageThumbnail> images = null;
        var uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        var projection = new String[]{
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATA
        };

        var cursor = getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null && cursor.getCount() > 0) {
            images = new ArrayList<>();

            var idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
            var nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
            var sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE);
            var typeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);
            var dataIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

            while (cursor.moveToNext()) {
                var id = cursor.getLong(idIndex);
                var name = cursor.getString(nameIndex);
                var size = cursor.getString(sizeIndex);
                var type = cursor.getString(typeIndex);
                var realPath = cursor.getString(dataIndex);

                var contentUri = ContentUris.withAppendedId(uri, id);
                Bitmap thumbnail = null;
                try {
                    thumbnail = MediaStore.Images.Media.getBitmap(getContentResolver(), contentUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                images.add(new ImageThumbnail(
                        id,
                        name,
                        Long.parseLong(size),
                        null,
                        contentUri,
                        thumbnail,
                        type,
                        "",
                        realPath
                ));
            }

            cursor.close();
        }
        return images;
    }

    private List<ImageThumbnail> getAllPhotoFromExternal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return getImagesShownInApi29();
        } else {
            return getImagesShownInApiLower29();
        }
    }

    public static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spanCount;
        private final int spacing;
        private final boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top Edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }
}
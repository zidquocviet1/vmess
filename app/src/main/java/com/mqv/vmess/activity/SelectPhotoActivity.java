package com.mqv.vmess.activity;

import static com.mqv.vmess.activity.EditProfileActivity.EXTRA_CHANGE_PHOTO;
import static com.mqv.vmess.activity.EditProfileActivity.EXTRA_IMAGE_THUMBNAIL;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mqv.vmess.R;
import com.mqv.vmess.activity.br.FileObserverBroadcastReceiver;
import com.mqv.vmess.activity.service.FileObserverService;
import com.mqv.vmess.databinding.ActivitySelectPhotoBinding;
import com.mqv.vmess.ui.adapter.ImageThumbnailAdapter;
import com.mqv.vmess.ui.data.ImageThumbnail;
import com.mqv.vmess.util.Const;
import com.mqv.vmess.util.Logging;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SelectPhotoActivity extends ToolbarActivity<AndroidViewModel, ActivitySelectPhotoBinding> {
    private String from;
    private static final int NUM_THUMBNAIL_PORTRAIT_COLUMN = 3;
    private static final int NUM_THUMBNAIL_LANDSCAPE_COLUMN = 9;
    private static final int NUM_SPACING = 9; // px
    private static final int CAMERA_POSITION = 0;
    private boolean isPendingStartCamera;
    private ImageThumbnailAdapter adapter;
    private List<ImageThumbnail> images;
    private FileObserverBroadcastReceiver br;
    private Intent mFileObserverServiceIntent;

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

        /*
         * The query to request change photo from is. [Change profile picture, Change cover photo]
         * */
        from = getIntent().getStringExtra(EXTRA_CHANGE_PHOTO);

        setupRecyclerView();

        //Start service to monitor file changes in the external storage
        mFileObserverServiceIntent = new Intent(this, FileObserverService.class);
        startService(mFileObserverServiceIntent);

        //Register FileObserver Broadcast Receiver
        var intentFilter = new IntentFilter(FileObserverBroadcastReceiver.ACTION_FILE_OBSERVER);
        br = new FileObserverBroadcastReceiver(new FileObserverBroadcastReceiver.onImagesChangeListener() {
            @Override
            public void onImageCreated(String path) {
                Logging.show("Image Created " + path);
            }

            @Override
            public void onImageDeleted(String path) {
                Logging.show("Image Deleted " + path);
            }

            @Override
            public void onImageMoved() {

            }
        });
        registerReceiver(br, intentFilter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        var images = getAllPhotoFromExternal(null);
        images.add(CAMERA_POSITION, new ImageThumbnail(Long.MAX_VALUE));

        adapter.submitList(images);
        adapter.notifyItemRangeChanged(0, images.size());
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (isPendingStartCamera && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePhotoIntent();
        } else
            isPendingStartCamera = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(br);
        stopService(mFileObserverServiceIntent);
    }

    @Override
    public void setupObserver() {
        // default implementation method
    }

    private void setupRecyclerView() {
        if (images == null) images = new ArrayList<>();
        /*
         * add the first empty item, because i want to create a texture view in the recycler view at position 0
         * */
        images.add(CAMERA_POSITION, new ImageThumbnail(Long.MAX_VALUE));

        /*
         * Get actual device width and height. Also known as Dimension
         * */
        var dimension = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dimension);
        var actualDeviceWidth = dimension.widthPixels;

        var spanCount = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
                ? NUM_THUMBNAIL_PORTRAIT_COLUMN : NUM_THUMBNAIL_LANDSCAPE_COLUMN; // columns
        adapter = new ImageThumbnailAdapter(this, actualDeviceWidth, spanCount, NUM_SPACING);
        adapter.submitList(images);
        adapter.setOnThumbnailClick(onThumbnailClick());
        adapter.setOnCameraPreviewClick(onCameraPreviewClick());

        mBinding.recyclerViewPhotos.setAdapter(adapter);
        mBinding.recyclerViewPhotos.setLayoutManager(new GridLayoutManager(this, spanCount));
        mBinding.recyclerViewPhotos.addItemDecoration(new GridSpacingItemDecoration(spanCount, NUM_SPACING, false));
    }

    private Consumer<ImageThumbnail> onThumbnailClick() {
        return imageThumbnail -> {
            if (imageThumbnail.getSize() != 0 && imageThumbnail.getContentUri() != null)
                startPreviewPhoto(imageThumbnail);
        };
    }

    private Consumer<Void> onCameraPreviewClick() {
        return unused -> {
            if (ContextCompat.checkSelfPermission(SelectPhotoActivity.this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePhotoIntent();
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    createDialog("Request Permission",
                            "The app will run perfectly when have a camera permission. Do you want to grant it.",
                            "Continue",
                            "Not now",
                            (dialog, which) -> permissionLauncher.launch(Manifest.permission.CAMERA, isGranted -> {
                                if (isGranted) dispatchTakePhotoIntent();
                            }));
                } else {
                    createDialog("Request Camera Permission",
                            "The app will run perfectly when have a camera permission. Go to Settings?",
                            "Go to Settings",
                            "Cancel",
                            (dialog, which) -> {
                                isPendingStartCamera = true;
                                var settingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                settingsIntent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(settingsIntent);
                            });
                }
            }
        };
    }

    private void dispatchTakePhotoIntent() {
        /*
         * The flow according to Android Development Guide
         * link{https://developer.android.com/training/camera/photobasics}
         * This method to get the original bitmap size from camera intent
         * */
        var takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePhotoIntent.resolveActivity(getPackageManager()) != null) {
            var contentUri = createOutputImage();

            // Set the output uri that the image is handled
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
            activityResultLauncher.launch(takePhotoIntent, result -> {
                if (result.getResultCode() == RESULT_OK) {
                    var images = getAllPhotoFromExternal(contentUri);
                    if (images != null && images.size() > 0) {
                        var imageThumbnail = images.get(0);

                        this.images.add(CAMERA_POSITION + 1, imageThumbnail);
                        this.adapter.submitList(this.images, () -> startPreviewPhoto(imageThumbnail));
                        this.adapter.notifyItemInserted(CAMERA_POSITION + 1);
                    }
                } else {
                    getContentResolver().delete(contentUri, null, null);
                }
            });
        }
        isPendingStartCamera = false;
    }

    private Uri createOutputImage() {
        Uri uri;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        var formatter = DateTimeFormatter.ofPattern(Const.IMAGE_FILE_NAME_PATTERN);
        var suffix = LocalDateTime.now().format(formatter);
        var fileName = "TAC_IMG_" + suffix;

        var cv = new ContentValues();
        cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        cv.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            cv.put(MediaStore.MediaColumns.DATE_TAKEN, String.valueOf(System.currentTimeMillis()));

        return getContentResolver().insert(uri, cv);
    }

    private void startPreviewPhoto(ImageThumbnail imageThumbnail) {
        var intent = new Intent(this, PreviewEditPhotoActivity.class);
        intent.putExtra(EXTRA_CHANGE_PHOTO, from);
        intent.putExtra(EXTRA_IMAGE_THUMBNAIL, imageThumbnail);

        activityResultLauncher.launch(intent, result -> {
            if (result.getResultCode() == RESULT_OK) {
                this.finish();
            }
        });
    }

    @Deprecated
    private Uri saveBitmapToExternalStorage(Bitmap bitmap) {
        /*
         * This method is used to save the thumbnail bitmap from the result of camera action image capture
         * According to Android Developer blog. This is good for the icon but not the solution a lot more.
         * */
        Uri uri;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        var formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        var suffix = LocalDateTime.now().format(formatter);
        var fileName = "TAC_IMG_" + suffix;

        var cv = new ContentValues();
        cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        cv.put(MediaStore.MediaColumns.HEIGHT, bitmap.getHeight());
        cv.put(MediaStore.MediaColumns.SIZE, bitmap.getByteCount());
        cv.put(MediaStore.MediaColumns.WIDTH, bitmap.getWidth());
        cv.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            cv.put(MediaStore.MediaColumns.DATE_TAKEN, String.valueOf(System.currentTimeMillis()));

        var contentUri = getContentResolver().insert(uri, cv);

        if (contentUri != null) {
            try {
                var os = getContentResolver().openOutputStream(contentUri);
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)) {
                    Toast.makeText(SelectPhotoActivity.this, "Couldn't save the image", Toast.LENGTH_SHORT).show();
                    return null;
                } else
                    return contentUri;
            } catch (FileNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    private void createDialog(String title, String message, String positive, String negative,
                              DialogInterface.OnClickListener positiveCallback) {
        new MaterialAlertDialogBuilder(SelectPhotoActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positive, positiveCallback)
                .setNegativeButton(negative, null)
                .create()
                .show();
    }

    @RequiresApi(29)
    private List<ImageThumbnail> getImagesShownInApi29(@Nullable Uri specificUri) {
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
        var imageDateSort = MediaStore.MediaColumns.DATE_TAKEN + " DESC";

        var cursor = getContentResolver().query(specificUri == null ? uri : specificUri,
                projection,
                null,
                null,
                imageDateSort);

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
                        size == null ? 0L : Long.parseLong(size),
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
        return images == null ? new ArrayList<>() : images;
    }

    private List<ImageThumbnail> getImagesShownInApiLower29(@Nullable Uri specificUri) {
        List<ImageThumbnail> images = null;

        var uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        var projection = new String[]{
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATA
        };

        var cursor = getContentResolver().query(specificUri == null ? uri : specificUri,
                projection,
                null,
                null,
                null);

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
                        size == null ? 0 : Long.parseLong(size),
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
        return images == null ? new ArrayList<>() : images;
    }

    private List<ImageThumbnail> getAllPhotoFromExternal(@Nullable Uri specificUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return getImagesShownInApi29(specificUri);
        } else {
            return getImagesShownInApiLower29(specificUri);
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
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, RecyclerView parent, @NonNull RecyclerView.State state) {
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
package com.mqv.vmess.activity;

import static com.mqv.vmess.activity.EditProfileActivity.EXTRA_CHANGE_PHOTO;
import static com.mqv.vmess.activity.EditProfileActivity.EXTRA_COVER_PHOTO;
import static com.mqv.vmess.activity.EditProfileActivity.EXTRA_IMAGE_THUMBNAIL;
import static com.mqv.vmess.activity.EditProfileActivity.EXTRA_PROFILE_PICTURE;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.mqv.vmess.R;
import com.mqv.vmess.activity.viewmodel.PreviewEditPhotoViewModel;
import com.mqv.vmess.databinding.ActivityPreviewEditPhotoBinding;
import com.mqv.vmess.manager.LoggedInUserManager;
import com.mqv.vmess.ui.data.ImageThumbnail;
import com.mqv.vmess.util.ExifUtils;
import com.mqv.vmess.util.NetworkStatus;
import com.mqv.vmess.util.Picture;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreviewEditPhotoActivity extends ToolbarActivity<PreviewEditPhotoViewModel, ActivityPreviewEditPhotoBinding> {
    private float my;
    private AlertDialog uploadingDialog;

    @Override
    public void binding() {
        mBinding = ActivityPreviewEditPhotoBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<PreviewEditPhotoViewModel> getViewModelClass() {
        return PreviewEditPhotoViewModel.class;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        var from = getIntent().getStringExtra(EXTRA_CHANGE_PHOTO);
        var image = (ImageThumbnail) getIntent().getParcelableExtra(EXTRA_IMAGE_THUMBNAIL);
        var bitmap = ExifUtils.getRotatedBitmap(getContentResolver(), image.getContentUri());

        if (from.equals(EXTRA_PROFILE_PICTURE)) {
            updateActionBarTitle(R.string.label_preview_profile_picture);

            mBinding.layoutProfilePhoto.setVisibility(View.VISIBLE);
            mBinding.layoutCoverPhoto.setVisibility(View.GONE);

            mBinding.imageProfilePhoto.setImageBitmap(bitmap);
            mBinding.imageProfilePhotoReal.setImageBitmap(bitmap);

            // TODO: crop the image here
            mBinding.buttonCrop.setOnClickListener(null);
        } else if (from.equals(EXTRA_COVER_PHOTO)) {
            updateActionBarTitle(R.string.label_preview_cover_photo);

            mBinding.layoutProfilePhoto.setVisibility(View.GONE);
            mBinding.layoutCoverPhoto.setVisibility(View.VISIBLE);

            mBinding.imageCoverPhoto.setImageBitmap(bitmap);

            var user = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser());

            var uri = user.getPhotoUrl();
            var url = uri == null ? null : uri.toString();

            Picture.loadUserAvatar(this, url).into(mBinding.imageProfileInCover);

            mBinding.textDisplayName.setText(user.getDisplayName());

            // TODO: not complete here
            mBinding.imageCoverPhoto.setOnTouchListener(imageCoverListener());
        }

        enableSaveButton(v -> {
            if (from.equals(EXTRA_PROFILE_PICTURE)) {
                mViewModel.updateProfilePicture(image.getRealPath());
            } else {
                mViewModel.updateCoverPhoto();
            }
        });

        registerFirebaseUserChange(firebaseUser -> {
            var photoUrl = firebaseUser.getPhotoUrl() == null ? "" : firebaseUser.getPhotoUrl().toString();
            var loggedInUser = LoggedInUserManager.getInstance().getLoggedInUser();

            if (loggedInUser != null){
                loggedInUser.setPhotoUrl(photoUrl);
                updateLoggedInUser(loggedInUser);
            }
            mViewModel.updateHistoryUserPhotoUrl(firebaseUser, photoUrl);
        });
    }

    @Override
    public void setupObserver() {
        mViewModel.getUploadPhotoResult().observe(this, uploadPhotoResult -> {
            if (uploadPhotoResult == null)
                return;

            if (uploadPhotoResult.getStatus() == NetworkStatus.LOADING) {
                showLoadingUi(true);
            } else if (uploadPhotoResult.getStatus() == NetworkStatus.SUCCESS) {
                reloadFirebaseUser();

                showLoadingUi(false);

                Toast.makeText(this, uploadPhotoResult.getSuccess(), Toast.LENGTH_SHORT).show();

                setResult(RESULT_OK);
                finish();
            } else if (uploadPhotoResult.getStatus() == NetworkStatus.ERROR) {
                showLoadingUi(false);

                Toast.makeText(this, uploadPhotoResult.getError(), Toast.LENGTH_SHORT).show();

                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    private void showLoadingUi(boolean isLoading) {
        makeButtonEnable(!isLoading);
        if (isLoading) {
            startUploadingDialog();
        } else {
            finishUploading();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private View.OnTouchListener imageCoverListener() {
        return (v, event) -> {
            var imageView = (ImageView) v;
            var f = new float[9];
            var matrix = imageView.getImageMatrix();
            matrix.getValues(f);

            var maxHeight = imageView.getDrawable().getIntrinsicHeight();
            float curY, dy;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: // the action to start event
                    my = event.getY(); // interval (0.0, view.getHeight())
                    break;
                case MotionEvent.ACTION_MOVE:
                    curY = event.getY();
                    dy = curY;

//                            if (curY - maxHeight > 0){
//                                dy = maxHeight;
//                            }

                    if (curY < 0) {
                        dy = 0;
                    }

                    if (curY > maxHeight) {
                        dy = maxHeight;
                    }

                    mBinding.imageCoverPhoto.scrollBy(0, (int) (my - dy)); // x equals to 0, that mean disable horizontal drag
                    break;
                case MotionEvent.ACTION_UP: // the action to finish event
                    break;
            }
            mBinding.imageCoverPhoto.setImageMatrix(matrix);
            return true;
        };
    }

    private void startUploadingDialog() {
        var builder = new MaterialAlertDialogBuilder(this);
        var view = getLayoutInflater().inflate(R.layout.dialog_loading_with_text, null, false);
        var textUploading = (TextView) view.findViewById(R.id.text_uploading);
        var animBlink = AnimationUtils.loadAnimation(this, R.anim.blink);
        textUploading.startAnimation(animBlink);
        builder.setView(view);
        uploadingDialog = builder.create();
        uploadingDialog.setCancelable(true);
        uploadingDialog.setCanceledOnTouchOutside(true);
        uploadingDialog.setOnCancelListener(dialog -> {
            Toast.makeText(this, R.string.msg_cancel_uploading_photo, Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
        });
        uploadingDialog.show();
    }

    private void finishUploading() {
        if (uploadingDialog != null)
            uploadingDialog.dismiss();
    }
}
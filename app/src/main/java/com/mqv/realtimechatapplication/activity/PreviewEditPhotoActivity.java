package com.mqv.realtimechatapplication.activity;

import static com.mqv.realtimechatapplication.activity.EditProfileActivity.EXTRA_CHANGE_PHOTO;
import static com.mqv.realtimechatapplication.activity.EditProfileActivity.EXTRA_COVER_PHOTO;
import static com.mqv.realtimechatapplication.activity.EditProfileActivity.EXTRA_IMAGE_THUMBNAIL;
import static com.mqv.realtimechatapplication.activity.EditProfileActivity.EXTRA_PROFILE_PICTURE;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.signature.ObjectKey;
import com.google.firebase.auth.FirebaseAuth;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.PreviewEditPhotoViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityPreviewEditPhotoBinding;
import com.mqv.realtimechatapplication.di.GlideApp;
import com.mqv.realtimechatapplication.ui.data.ImageThumbnail;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.ExifUtils;
import com.mqv.realtimechatapplication.util.NetworkStatus;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreviewEditPhotoActivity extends ToolbarActivity<PreviewEditPhotoViewModel, ActivityPreviewEditPhotoBinding> {
    private float my;

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
            if (uri != null) {
                var url = uri.toString().replace("localhost", Const.BASE_IP);

                var placeHolder = new CircularProgressDrawable(this);
                placeHolder.setStrokeWidth(5f);
                placeHolder.setCenterRadius(30f);
                placeHolder.start();

                GlideApp.with(this)
                        .load(url)
                        .centerCrop()
                        .placeholder(placeHolder)
                        .error(R.drawable.ic_round_account)
                        .signature(new ObjectKey(url))
                        .into(mBinding.imageProfileInCover);
            }

            mBinding.textDisplayName.setText(user.getDisplayName());

            // TODO: not complete here
            mBinding.imageCoverPhoto.setOnTouchListener(imageCoverListener());
        }

        mBinding.buttonSave.setOnClickListener(v -> {
            if (from.equals(EXTRA_PROFILE_PICTURE)) {
                mViewModel.updateProfilePicture(image.getRealPath());
            }else{
                mViewModel.updateCoverPhoto();
            }
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
                showLoadingUi(false);

                Toast.makeText(this, uploadPhotoResult.getSuccess(), Toast.LENGTH_SHORT).show();

                reloadFirebaseUser();

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
        mBinding.buttonSave.setEnabled(!isLoading);
        mBinding.progressBarLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private View.OnTouchListener imageCoverListener() {
        return new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                var imageView = (ImageView) v;
                var f = new float[9];
                var matrix = imageView.getImageMatrix();
                matrix.getValues(f);

                var maxHeight = imageView.getDrawable().getIntrinsicHeight();
                float curX, curY, dy;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: // the action to start event
                        my = event.getY(); // interval (0.0, view.getHeight())
                        break;
                    case MotionEvent.ACTION_MOVE:
                        var scaleY = f[Matrix.MSCALE_Y];

                        var originalHeight = imageView.getDrawable().getIntrinsicHeight();
                        var actualHeight = Math.round(scaleY * originalHeight);

                        var matrixY = f[Matrix.MTRANS_Y];
                        var matrixX = f[Matrix.MTRANS_X];

                        dy = event.getY() - my;

                        //if image will go outside top bound
                        if (matrixY + dy < 0) {
                            dy = -matrixY;
                        }

                        //if image will go outside bottom bound
                        if (matrixY + dy + actualHeight > v.getHeight()) {
                            dy = v.getHeight() - matrixY - actualHeight;
                        }
                        matrix.postTranslate(matrixX, dy);

//                            curY = event.getY();
//                            Logging.show(String.format("Start Y = %.2f, Current Y = %.2f", my, curY));
//                            dy = curY;
//
////                            if (curY - maxHeight > 0){
////                                dy = maxHeight;
////                            }
//
//                            if (curY < 0){
//                                dy = 0;
//                            }
//
//                            if (curY > maxHeight){
//                                dy = maxHeight;
//                            }
//
//                            mBinding.imageCoverPhoto.scrollBy(0, (int) (my - dy)); // x equals to 0, that mean disable horizontal drag
                        break;
                    case MotionEvent.ACTION_UP: // the action to finish event
                        break;
                }
                mBinding.imageCoverPhoto.setImageMatrix(matrix);
                return true;
            }
        };
    }
}
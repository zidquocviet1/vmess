package com.mqv.realtimechatapplication.activity;

import static com.mqv.realtimechatapplication.activity.EditProfileActivity.EXTRA_CHANGE_PHOTO;
import static com.mqv.realtimechatapplication.activity.EditProfileActivity.EXTRA_COVER_PHOTO;
import static com.mqv.realtimechatapplication.activity.EditProfileActivity.EXTRA_IMAGE_THUMBNAIL;
import static com.mqv.realtimechatapplication.activity.EditProfileActivity.EXTRA_PROFILE_PICTURE;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.lifecycle.AndroidViewModel;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.google.firebase.auth.FirebaseAuth;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.PreviewEditPhotoViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityPreviewEditPhotoBinding;
import com.mqv.realtimechatapplication.ui.data.ImageThumbnail;
import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.ExifUtils;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreviewEditPhotoActivity extends ToolbarActivity<PreviewEditPhotoViewModel, ActivityPreviewEditPhotoBinding> {

    private float mx, my;

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
        } else if (from.equals(EXTRA_COVER_PHOTO)) {
            updateActionBarTitle(R.string.label_preview_cover_photo);

            mBinding.layoutProfilePhoto.setVisibility(View.GONE);
            mBinding.layoutCoverPhoto.setVisibility(View.VISIBLE);

            mBinding.imageCoverPhoto.setImageBitmap(bitmap);

            var user = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser());

            var uri = user.getPhotoUrl();
            if (uri != null) {
                var url = uri.toString().replace("localhost", Const.BASE_IP);

                Glide.with(getApplicationContext())
                        .load(url)
                        .centerCrop()
                        .error(R.drawable.ic_round_account)
                        .signature(new ObjectKey(url))
                        .into(mBinding.imageProfileInCover);
            }

            mBinding.textDisplayName.setText(user.getDisplayName());

            //TODO: problem here
            mBinding.imageCoverPhoto.setOnTouchListener(new Touch(mBinding.imageCoverPhoto));

//            mBinding.imageCoverPhoto.setOnTouchListener(new View.OnTouchListener() {
//                @Override
//                public boolean onTouch(View v, MotionEvent event) {
//                    var imageView = (ImageView) v;
//                    var matrix = imageView.getImageMatrix();
//                    var maxWidth = imageView.getMaxWidth();
//                    var maxHeight = imageView.getMaxHeight();
//
//                    float curX, curY;
//
//                    switch (event.getAction()) {
//                        case MotionEvent.ACTION_DOWN:
//                            mx = event.getX();
//                            my = event.getY();
//                            break;
//                        case MotionEvent.ACTION_MOVE:
//                            curX = event.getX();
//                            curY = event.getY();
//
//                            mBinding.imageCoverPhoto.scrollBy((int) (mx - curX), (int) (my - curY));
//                            mx = curX;
//                            my = curY;
//                            break;
//                        case MotionEvent.ACTION_UP:
//                            curX = event.getX();
//                            curY = event.getY();
//
//                            mBinding.imageCoverPhoto.scrollBy((int) (mx - curX), (int) (my - curY));
//                            break;
//                    }
//                    return true;
//                }
//            });
        }

        mBinding.buttonSave.setOnClickListener(v -> {
            // TODO: upload image to the spring server
        });
    }

    @Override
    public void setupObserver() {

    }

    public class Touch implements View.OnTouchListener {
        private static final int NONE = 0;
        private static final int DRAG = 1;
        private static final int ZOOM = 2;

        private static final float MIN_ZOOM = 1f;
        private static final float MAX_ZOOM = 5f;

        private Matrix matrix = new Matrix();
        private Matrix savedMatrix = new Matrix();

        private PointF start = new PointF();
        private PointF mid = new PointF();

        private int mode = NONE;
        private float oldDistance = 1f;

        private float dx; // postTranslate X distance
        private float dy; // postTranslate Y distance
        private float[] matrixValues = new float[9];
        float matrixX = 0; // X coordinate of matrix inside the ImageView
        float matrixY = 0; // Y coordinate of matrix inside the ImageView
        float width = 0; // width of drawable
        float height = 0; // height of drawable

        private ImageView imageView;

        public Touch(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            ImageView imageView = (ImageView) view;

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrix.set(matrix);
                    start.set(event.getX(), event.getY());
                    mode = DRAG;
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDistance = spacing(event);
                    if (oldDistance > 10f) {
                        savedMatrix.set(matrix);
                        midPoint(mid, event);
                        mode = ZOOM;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    mode = NONE;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        matrix.set(savedMatrix);

                        matrix.getValues(matrixValues);
                        matrixX = matrixValues[2];
                        matrixY = matrixValues[5];
                        width = matrixValues[0] * (((ImageView) view).getDrawable()
                                .getIntrinsicWidth());
                        height = matrixValues[4] * (((ImageView) view).getDrawable()
                                .getIntrinsicHeight());

                        dx = event.getX() - start.x;
                        dy = event.getY() - start.y;

                        //if image will go outside left bound
                        if (matrixX + dx < 0) {
                            dx = -matrixX;
                        }
                        //if image will go outside right bound
                        if (matrixX + dx + width > view.getWidth()) {
                            dx = view.getWidth() - matrixX - width;
                        }
                        //if image will go oustside top bound
                        if (matrixY + dy < 0) {
                            dy = -matrixY;
                        }
                        //if image will go outside bottom bound
                        if (matrixY + dy + height > view.getHeight()) {
                            dy = view.getHeight() - matrixY - height;
                        }
                        matrix.postTranslate(dx, dy);
                    } else if (mode == ZOOM) {
                        float newDistance = spacing(event);
                        if (newDistance > 10f) {
                            matrix.set(savedMatrix);
                            float scale = newDistance / oldDistance;
                            float[] values = new float[9];
                            matrix.getValues(values);
                            float currentScale = values[Matrix.MSCALE_X];
                            if (scale * currentScale > MAX_ZOOM)
                                scale = MAX_ZOOM / currentScale;
                            else if (scale * currentScale < MIN_ZOOM)
                                scale = MIN_ZOOM / currentScale;
                            matrix.postScale(scale, scale, mid.x, mid.y);
                        }
                    }
                    break;
            }
            this.imageView.setImageMatrix(matrix);
            return true;
        }

        private float spacing(MotionEvent event) {
            float x = event.getX(0) - event.getX(1);
            float y = event.getY(0) - event.getY(1);
            return (float) Math.sqrt(x * x + y * y);
        }

        private void midPoint(PointF point, MotionEvent event) {
            point.set((event.getX(0) + event.getX(1)) / 2, (event.getY(0) + event.getY(1)) / 2);
        }
    }
}
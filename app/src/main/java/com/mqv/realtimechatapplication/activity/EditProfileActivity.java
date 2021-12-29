package com.mqv.realtimechatapplication.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.EditProfileViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityEditProfileBinding;
import com.mqv.realtimechatapplication.network.model.User;
import com.mqv.realtimechatapplication.network.model.UserSocialLink;
import com.mqv.realtimechatapplication.ui.adapter.UserLinkAdapter;
import com.mqv.realtimechatapplication.ui.fragment.preference.UserDetailsPreferenceFragment;
import com.mqv.realtimechatapplication.util.Picture;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EditProfileActivity extends BaseUserActivity<EditProfileViewModel, ActivityEditProfileBinding>
        implements View.OnClickListener {
    public static final String EXTRA_PROFILE_PICTURE = "profile_picture";
    public static final String EXTRA_COVER_PHOTO = "cover_photo";
    public static final String EXTRA_CHANGE_PHOTO = "change_photo";
    public static final String EXTRA_IMAGE_THUMBNAIL = "image_thumbnail";

    private boolean isOpenSelectPhotoPending = false;
    private String extraSelected;

    @Override
    public void binding() {
        mBinding = ActivityEditProfileBinding.inflate(getLayoutInflater());
    }

    @NonNull
    @Override
    public Class<EditProfileViewModel> getViewModelClass() {
        return EditProfileViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.label_edit_profile);

        mBinding.imageAvatar.setOnClickListener(this);
        mBinding.imageCover.setOnClickListener(this);
        mBinding.buttonEditBio.setOnClickListener(this);
        mBinding.buttonEditPicture.setOnClickListener(this);
        mBinding.buttonEditCover.setOnClickListener(this);
        mBinding.buttonEditDetails.setOnClickListener(this);
        mBinding.buttonEditLinks.setOnClickListener(this);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout_preferences_details, new UserDetailsPreferenceFragment())
                .commit();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED && isOpenSelectPhotoPending) {
            startActivity(SelectPhotoActivity.class);
            isOpenSelectPhotoPending = false;
        }
    }

    @Override
    public void setupObserver() {
        mViewModel.getFirebaseUser().observe(this, this::refreshFirebaseUserUi);

        mViewModel.getLoggedInUser().observe(this, this::showLoggedInUserUi);
    }

    @Override
    public void refreshFirebaseUserUi(FirebaseUser user) {
        runOnUiThread(() -> showFirebaseUserUi(user));
    }

    @Override
    public void refreshLoggedInUserUi(User user) {
        runOnUiThread(() -> showLoggedInUserUi(user));
    }

    @Override
    public void onClick(View v) {
        var id = v.getId();

        if (id == mBinding.imageAvatar.getId() || id == mBinding.buttonEditPicture.getId()) {
            isOpenSelectPhotoPending = true;
            extraSelected = EXTRA_PROFILE_PICTURE;
            checkPermission();
        } else if (id == mBinding.imageCover.getId() || id == mBinding.buttonEditCover.getId()) {
            isOpenSelectPhotoPending = true;
            extraSelected = EXTRA_COVER_PHOTO;
            checkPermission();
        } else if (id == mBinding.buttonEditBio.getId()) {
            startActivity(EditProfileBioActivity.class);
        } else if (id == mBinding.buttonEditDetails.getId()) {
            startActivity(EditDetailsActivity.class);
        } else if (id == mBinding.buttonEditLinks.getId()) {
            startActivity(EditProfileLinkActivity.class);
        }
    }

    private void showLoggedInUserUi(User user) {
        if (user == null) return;

        mBinding.textBio.setText(user.getBiographic());

        var adapter = new UserLinkAdapter(this, user.getSocialLinks(), R.layout.item_preference_content, UserLinkAdapter.ACTION.VIEW);
        adapter.submitList(user.getSocialLinks());
        adapter.setOnSocialLinkViewClickListener(this::handleSocialLinkClicked);

        mBinding.recyclerViewLinks.setAdapter(adapter);
        mBinding.recyclerViewLinks.setNestedScrollingEnabled(false);
        mBinding.recyclerViewLinks.setHasFixedSize(false);
        mBinding.recyclerViewLinks.setLayoutManager(new LinearLayoutManager(this));
    }

    private void showFirebaseUserUi(FirebaseUser user) {
        if (user == null) return;

        var uri = user.getPhotoUrl();
        var url = uri != null ? uri.toString() : null;

        Picture.loadUserAvatar(this, url).into(mBinding.imageAvatar);
    }

    private void handleSocialLinkClicked(UserSocialLink socialLink) {
        var type = socialLink.getType();

        final Intent intent = new Intent(Intent.ACTION_VIEW);

        try {
            if (getPackageManager().getPackageInfo(type.getPackageName(), 0) != null) {
                // http://stackoverflow.com/questions/21505941/intent-to-open-instagram-user-profile-on-android
                intent.setData(Uri.parse(type.getUrl() + socialLink.getAccountName()));
                intent.setPackage(type.getPackageName());
                startActivity(intent);
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            intent.setData(Uri.parse(type.getUrl() + socialLink.getAccountName()));
            startActivity(intent);
        }
    }

    private void startActivity(Class<?> target) {
        var intent = new Intent(getApplicationContext(), target);

        if (target == SelectPhotoActivity.class) {
            intent.putExtra(EXTRA_CHANGE_PHOTO, extraSelected);
        }

        startActivity(intent);
    }

    private void checkPermission() {
        /*
         * Following to the recommendation of Google Android Developer about the permission privacy
         * We should use this pattern to request any permissions in this app
         * */
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            startActivity(SelectPhotoActivity.class);
            isOpenSelectPhotoPending = false;
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            /*
             * First time to ask about a permission. If user deny grant the permission, don't show this again
             * Go to the section below
             * */
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Request Permission")
                    .setMessage("Grant a permission to allow this app access a gallery")
                    .setNegativeButton("Not now", null)
                    .setPositiveButton("Continue", (dialog, which) ->
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE, result -> {
                                if (result) {
                                    startActivity(SelectPhotoActivity.class);
                                    isOpenSelectPhotoPending = false;
                                }
                            }))
                    .create().show();
        } else {
            /*
             * In this section, show a dialog to explain why this app need a permission
             * and make the user go to settings to open it
             * */
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Request Permission")
                    .setMessage("The app need a read external storage permission to run smoothly. Go to Settings to grant the permission")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Settings", (dialog, which) -> {
                        var settingIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        settingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        settingIntent.setData(Uri.parse("package:" + this.getPackageName()));
                        startActivity(settingIntent);
                    })
                    .create().show();
        }
    }
}
package com.mqv.vmess.activity;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.vmess.R;
import com.mqv.vmess.activity.viewmodel.UserViewModel;
import com.mqv.vmess.databinding.ActivityUserBinding;
import com.mqv.vmess.ui.fragment.preference.UserPreferencesFragment;
import com.mqv.vmess.util.Picture;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class UserActivity extends ToolbarActivity<UserViewModel, ActivityUserBinding> {
    private UserPreferencesFragment fragment;

    @Override
    public void binding() {
        mBinding = ActivityUserBinding.inflate(getLayoutInflater());
    }

    @NonNull
    @Override
    public Class<UserViewModel> getViewModelClass() {
        return UserViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.label_user_information);

        fragment = new UserPreferencesFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout_preferences, fragment)
                .commit();

        registerFirebaseUserChange(this::showUserUi);
    }

    @Override
    public void setupObserver() {
        mViewModel.getFirebaseUser().observe(this, this::showUserUi);
    }

    @UiThread
    private void showUserUi(FirebaseUser user) {
        runOnUiThread(() -> {
            if (user == null)
                return;

            var uri = user.getPhotoUrl();
            var url = uri == null ? null : uri.toString();

            Picture.loadUserAvatar(this, url)
                   .listener(new RequestListener<>() {
                       @Override
                       public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                           if (url == null) {
                               fragment.setSignInIcon(Picture.getDefaultUserAvatar(UserActivity.this));
                               return false;
                           }

                           fragment.setSignInIcon(Picture.getErrorAvatarLoaded(UserActivity.this));
                           mBinding.imageAvatar.setImageDrawable(Picture.getErrorAvatarLoaded(UserActivity.this));
                           return true;
                       }

                       @Override
                       public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                           fragment.setSignInIcon(resource);
                           return false;
                       }
                   })
                   .into(mBinding.imageAvatar);

            mBinding.textDisplayName.setText(user.getDisplayName());
        });
    }
}
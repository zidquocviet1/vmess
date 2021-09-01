package com.mqv.realtimechatapplication.activity;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.UserViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityUserBinding;
import com.mqv.realtimechatapplication.ui.fragment.preference.UserPreferencesFragment;
import com.mqv.realtimechatapplication.util.Const;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class UserActivity extends ToolbarActivity<UserViewModel, ActivityUserBinding> {
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

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout_preferences, new UserPreferencesFragment())
                .commit();

        registerFirebaseUserChange(this::showUserUi);
    }

    @Override
    public void setupObserver() {
        mViewModel.getFirebaseUser().observe(this, this::showUserUi);
    }

    @UiThread
    private void showUserUi(FirebaseUser user){
        runOnUiThread(() -> {
            if (user == null)
                return;

            //TODO: reformat the url in the develop mode
            var uri = user.getPhotoUrl();
            if (uri != null) {
                var url = uri.toString().replace("localhost", Const.BASE_IP);

                Glide.with(getApplicationContext())
                        .load(url)
                        .centerCrop()
                        .error(R.drawable.ic_round_account)
                        .signature(new ObjectKey(url))
                        .into(mBinding.imageAvatar);
            }
            mBinding.textDisplayName.setText(user.getDisplayName());
        });
    }
}
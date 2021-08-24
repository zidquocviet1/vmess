package com.mqv.realtimechatapplication.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.viewmodel.UserViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityUserBinding;
import com.mqv.realtimechatapplication.ui.fragment.UserPreferencesFragment;
import com.mqv.realtimechatapplication.util.Const;

public class UserActivity extends BaseActivity<UserViewModel, ActivityUserBinding> implements View.OnClickListener {
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

        mBinding.buttonBack.setOnClickListener(this);

        findViewById(R.id.button_edit_profile).setOnClickListener(v -> {
            // TODO: call upload photo to the Spring server if success then call reload user
//            user.reload().addOnCompleteListener(new OnCompleteListener<Void>() {
//                @Override
//                public void onComplete(@NonNull Task<Void> task) {
//                    Toast.makeText(getApplicationContext(), "Reload User", Toast.LENGTH_SHORT).show();
//                }
//            });
        });

//        findViewById(R.id.button_log_out).setOnClickListener(v -> {
//            // TODO: Loi cho nay can phai check lai ben Android Studio Arctic Fox
////            auth.signOut();
//
////            finishAffinity();
//
//            var loginIntent = new Intent(UserActivity.this, LoginActivity.class);
////            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
////            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
////            loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//
//            startActivity(loginIntent);
//        });

        var userPreferences = new UserPreferencesFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout_preferences, userPreferences)
                .commit();
    }

    @Override
    public void setupObserver() {
        mViewModel.getFirebaseUser().observe(this, user -> {
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

    @Override
    public void onClick(View v) {
        var id = v.getId();

        if (id == mBinding.buttonBack.getId()){
            onBackPressed();
        }
    }
}
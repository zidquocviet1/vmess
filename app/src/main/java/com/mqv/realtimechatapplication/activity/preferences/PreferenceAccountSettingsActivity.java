package com.mqv.realtimechatapplication.activity.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.lifecycle.AndroidViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.LoginActivity;
import com.mqv.realtimechatapplication.activity.ToolbarActivity;
import com.mqv.realtimechatapplication.activity.viewmodel.AccountSettingViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityPreferenceAccountSettingsBinding;
import com.mqv.realtimechatapplication.manager.LoggedInUserManager;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferenceAccountSettingsActivity extends ToolbarActivity<AccountSettingViewModel, ActivityPreferenceAccountSettingsBinding> {

    @Override
    public void binding() {
        mBinding = ActivityPreferenceAccountSettingsBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<AccountSettingViewModel> getViewModelClass() {
        return AccountSettingViewModel.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.title_preference_item_account_settings);

        mBinding.buttonLogOut.setOnClickListener(v -> {
            var user = getCurrentUser();
            if (user != null)
                mViewModel.signOut(user.getUid());
        });
    }

    @Override
    public void setupObserver() {
        mViewModel.getSignOutStatus().observe(this, isSignOut -> {
            if (isSignOut){
                LoggedInUserManager.getInstance().signOut();
                FirebaseAuth.getInstance().signOut();

                var loginIntent = new Intent(PreferenceAccountSettingsActivity.this, LoginActivity.class);
                loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(loginIntent);
                finishAffinity();
            }else{
                Toast.makeText(this, "Fail to sign out the current user", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
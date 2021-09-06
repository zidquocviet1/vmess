package com.mqv.realtimechatapplication.activity.preferences;

import android.content.Intent;
import android.os.Bundle;

import androidx.lifecycle.AndroidViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.LoginActivity;
import com.mqv.realtimechatapplication.activity.ToolbarActivity;
import com.mqv.realtimechatapplication.databinding.ActivityPreferenceAccountSettingsBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferenceAccountSettingsActivity extends ToolbarActivity<AndroidViewModel, ActivityPreferenceAccountSettingsBinding> {

    @Override
    public void binding() {
        mBinding = ActivityPreferenceAccountSettingsBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<AndroidViewModel> getViewModelClass() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.title_preference_item_account_settings);

        mBinding.buttonLogOut.setOnClickListener(v -> {
            var user = getCurrentUser();
            if (user != null) {
                FirebaseAuth.getInstance().signOut();

                var loginIntent = new Intent(PreferenceAccountSettingsActivity.this, LoginActivity.class);
                loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(loginIntent);
                finishAffinity();
            }
        });
    }

    @Override
    public void setupObserver() {

    }
}
package com.mqv.realtimechatapplication.activity.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.LoginActivity;
import com.mqv.realtimechatapplication.activity.ToolbarActivity;
import com.mqv.realtimechatapplication.activity.viewmodel.AccountSettingViewModel;
import com.mqv.realtimechatapplication.databinding.ActivityPreferenceAccountSettingsBinding;
import com.mqv.realtimechatapplication.util.LoadingDialog;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferenceAccountSettingsActivity extends ToolbarActivity<AccountSettingViewModel, ActivityPreferenceAccountSettingsBinding> {
    private boolean isLoading;

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
                mViewModel.signOut(user);
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isLoading){
            LoadingDialog.finishLoadingDialog();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (isLoading){
            LoadingDialog.startLoadingDialog(this, getLayoutInflater(), R.string.action_loading);
        }
    }

    @Override
    public void setupObserver() {
        mViewModel.getSignOutStatus().observe(this, result -> {
            if (result == null)
                return;

            var status = result.getStatus();

            switch (status){
                case LOADING:
                    isLoading = true;

                    LoadingDialog.startLoadingDialog(this, getLayoutInflater(), R.string.action_loading);
                    break;
                case ERROR:
                    isLoading = false;

                    LoadingDialog.finishLoadingDialog();

                    Toast.makeText(this, "Fail to sign out the current user, error: " + result.getError(),
                            Toast.LENGTH_SHORT).show();
                    break;
                case SUCCESS:
                    isLoading = false;

                    LoadingDialog.finishLoadingDialog();

                    var loginIntent = new Intent(PreferenceAccountSettingsActivity.this, LoginActivity.class);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(loginIntent);
                    finishAffinity();
                    break;
            }
        });
    }
}
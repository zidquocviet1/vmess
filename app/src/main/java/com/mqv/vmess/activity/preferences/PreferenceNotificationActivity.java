package com.mqv.vmess.activity.preferences;

import android.os.Bundle;

import androidx.lifecycle.AndroidViewModel;

import com.mqv.vmess.R;
import com.mqv.vmess.activity.ToolbarActivity;
import com.mqv.vmess.databinding.ActivityPreferenceNotificationBinding;
import com.mqv.vmess.dependencies.AppDependencies;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferenceNotificationActivity extends ToolbarActivity<AndroidViewModel, ActivityPreferenceNotificationBinding> {

    @Override
    public void binding() {
        mBinding = ActivityPreferenceNotificationBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<AndroidViewModel> getViewModelClass() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.title_preference_item_notification_and_sounds);

        mBinding.switchNotification.setChecked(!AppDependencies.getAppPreferences().getNotificationStatus());
        mBinding.switchNotification.setOnClickListener(v -> AppDependencies.getAppPreferences().setNotificationStatus(!mBinding.switchNotification.isChecked()));
    }

    @Override
    public void setupObserver() {

    }
}
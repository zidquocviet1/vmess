package com.mqv.vmess.activity.preferences;

import androidx.lifecycle.AndroidViewModel;

import android.os.Bundle;

import com.mqv.vmess.R;
import com.mqv.vmess.activity.ToolbarActivity;
import com.mqv.vmess.databinding.ActivityPreferenceNotificationBinding;

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
    }

    @Override
    public void setupObserver() {

    }
}
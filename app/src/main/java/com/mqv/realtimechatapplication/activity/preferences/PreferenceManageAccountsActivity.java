package com.mqv.realtimechatapplication.activity.preferences;

import android.os.Bundle;

import androidx.lifecycle.AndroidViewModel;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.ToolbarActivity;
import com.mqv.realtimechatapplication.databinding.ActivityPreferenceManageAccountsBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferenceManageAccountsActivity extends
        ToolbarActivity<AndroidViewModel, ActivityPreferenceManageAccountsBinding> {
    @Override
    public void binding() {
        mBinding = ActivityPreferenceManageAccountsBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<AndroidViewModel> getViewModelClass() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference_manage_accounts);

        setupToolbar();

        updateActionBarTitle(R.string.label_switch_accounts);
    }

    @Override
    public void setupObserver() {

    }
}
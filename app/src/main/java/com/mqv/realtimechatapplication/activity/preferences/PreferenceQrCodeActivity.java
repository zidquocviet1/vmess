package com.mqv.realtimechatapplication.activity.preferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.AndroidViewModel;

import android.os.Bundle;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.ToolbarActivity;
import com.mqv.realtimechatapplication.databinding.ActivityPreferenceQrCodeBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferenceQrCodeActivity extends ToolbarActivity<AndroidViewModel, ActivityPreferenceQrCodeBinding> {

    @Override
    public void binding() {
        mBinding = ActivityPreferenceQrCodeBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<AndroidViewModel> getViewModelClass() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.title_preference_item_qr_code);
    }

    @Override
    public void setupObserver() {

    }
}
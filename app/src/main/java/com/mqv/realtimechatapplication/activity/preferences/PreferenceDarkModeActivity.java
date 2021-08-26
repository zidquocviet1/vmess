package com.mqv.realtimechatapplication.activity.preferences;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.databinding.ActivityPreferenceDarkModeBinding;

public class PreferenceDarkModeActivity extends AppCompatActivity {
    private ActivityPreferenceDarkModeBinding mBinding;
    private DARK_MODE dMode = DARK_MODE.SYSTEM;

    enum DARK_MODE {
        ON, OFF, SYSTEM
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityPreferenceDarkModeBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.includedAppbar.toolbar.setTitle(getString(R.string.title_preference_item_dark_mode));
        mBinding.includedAppbar.buttonBack.setOnClickListener(v -> onBackPressed());
    }
}
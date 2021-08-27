package com.mqv.realtimechatapplication.activity.preferences;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.AndroidViewModel;

import com.mqv.realtimechatapplication.R;
import com.mqv.realtimechatapplication.activity.ToolbarActivity;
import com.mqv.realtimechatapplication.databinding.ActivityPreferenceDarkModeBinding;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreferenceDarkModeActivity extends ToolbarActivity<AndroidViewModel,
        ActivityPreferenceDarkModeBinding> {
    @Inject
    AppPreferences mPreferences;

    public void binding() {
        mBinding = ActivityPreferenceDarkModeBinding.inflate(getLayoutInflater());
    }

    @Override
    public Class<AndroidViewModel> getViewModelClass() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupToolbar();

        updateActionBarTitle(R.string.label_dark_mode);

        var mode = mPreferences.getDarkModeTheme();

        switch(mode){
            case ON:
                mBinding.radioOn.setChecked(true);
                break;
            case OFF:
                mBinding.radioOff.setChecked(true);
                break;
            case SYSTEM:
                mBinding.radioSystem.setChecked(true);
                break;
        }

        mBinding.radioGroupMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_on) {
                mPreferences.setDarkModeTheme(DarkMode.ON);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else if (checkedId == R.id.radio_off) {
                mPreferences.setDarkModeTheme(DarkMode.OFF);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                mPreferences.setDarkModeTheme(DarkMode.SYSTEM);
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
        });
    }

    @Override
    public void setupObserver() {

    }
}
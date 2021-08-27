package com.mqv.realtimechatapplication;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.mqv.realtimechatapplication.activity.preferences.AppPreferences;
import com.mqv.realtimechatapplication.activity.preferences.DarkMode;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MainApplication extends Application {
    @Inject
    AppPreferences mPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        setAppTheme(mPreferences.getDarkModeTheme());
    }

    private void setAppTheme(DarkMode mode){
        switch(mode){
            case ON:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case OFF:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case SYSTEM:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}

package com.mqv.realtimechatapplication;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;

import com.mqv.realtimechatapplication.activity.preferences.AppPreferences;
import com.mqv.realtimechatapplication.activity.preferences.DarkMode;
import com.mqv.realtimechatapplication.util.Logging;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MainApplication extends Application implements Configuration.Provider{
    @Inject
    AppPreferences mPreferences;

    @Inject
    HiltWorkerFactory workerFactory;

    private Activity activeActivity;

    @Override
    public void onCreate() {
        super.onCreate();
        setAppTheme(mPreferences.getDarkModeTheme());
        setupActivityListener();
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

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build();
    }

    private void setupActivityListener() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                activeActivity = activity;
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                activeActivity = null;
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                Logging.show("onActivityDestroyed: " + activity.getLocalClassName());
            }
        });
    }

    public Activity getActiveActivity() {
        return activeActivity;
    }
}

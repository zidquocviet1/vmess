package com.mqv.vmess;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;

import com.google.gson.Gson;
import com.mqv.vmess.activity.br.AlarmSleepTimer;
import com.mqv.vmess.activity.preferences.AppPreferences;
import com.mqv.vmess.activity.preferences.DarkMode;
import com.mqv.vmess.data.MyDatabase;
import com.mqv.vmess.dependencies.AppDependencies;
import com.mqv.vmess.dependencies.AppDependencyProvider;
import com.mqv.vmess.util.Logging;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

@HiltAndroidApp
public class MainApplication extends Application implements Configuration.Provider{
    @Inject AppPreferences    mPreferences;
    @Inject HiltWorkerFactory workerFactory;
    @Inject OkHttpClient      okHttpClient;
    @Inject Gson              gson;
    @Inject MyDatabase        database;
    @Inject Retrofit          retrofit;

    private Activity activeActivity;

    @Override
    public void onCreate() {
        super.onCreate();
        initializeAppDependencies();
        setAppTheme(mPreferences.getDarkModeTheme());
        setupActivityListener();
        setupObserveIncomingMessage();
        initializeAlarmSleepTimer();
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

    private void initializeAppDependencies() {
        AppDependencies.init(new AppDependencyProvider(this, database, retrofit, okHttpClient, gson));
    }

    private void setupObserveIncomingMessage() {
//        AppDependencies.getIncomingMessageObserver();
    }

    private void initializeAlarmSleepTimer() {
        new AlarmSleepTimer(this).setRepeatingAlarm();
    }
}

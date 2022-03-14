package com.mqv.vmess.di;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.mqv.vmess.activity.preferences.AppPreferences;
import com.mqv.vmess.activity.preferences.AppPreferencesImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class PreferencesModule {
    @Provides
    @Singleton
    public SharedPreferences provideSharedPreferences(@ApplicationContext Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Provides
    @Singleton
    public AppPreferences provideAppPreferences(@ApplicationContext Context context,
                                                SharedPreferences sharedPreferences) {
        return new AppPreferencesImpl(context, sharedPreferences);
    }
}

package com.mqv.realtimechatapplication.activity.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.mqv.realtimechatapplication.util.Const;
import com.mqv.realtimechatapplication.util.Logging;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.Nullable;

public class AppPreferencesImpl implements AppPreferences{
    public static final String PREF_DARK_THEME = "dark_theme_mode";

    private final Context mContext;
    private final SharedPreferences mPreferences;
    private final ListenerRegistry mListeners;

    static class ListenerRegistry implements SharedPreferences.OnSharedPreferenceChangeListener{
        private final AppPreferences appPreferences;
        private final Set<Listener> listeners;

        ListenerRegistry(AppPreferences appPreferences){
            this.appPreferences = appPreferences;
            listeners = new CopyOnWriteArraySet<>();
        }

        void add(@Nullable final Listener listener){
            if (listener != null){
                listeners.add(listener);
            }
        }

        void remove(@Nullable final Listener listener){
            if (listener != null){
                listeners.remove(listener);
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (PREF_DARK_THEME.equals(key)){
                var darkMode = appPreferences.getDarkModeTheme();
                 for (var l : listeners){
                     l.onDarkThemeModeChanged(darkMode);
                 }
            }
        }
    }

    public AppPreferencesImpl(Context context, SharedPreferences sharedPreferences) {
        mContext = context;
        mPreferences = sharedPreferences;
        mListeners = new ListenerRegistry(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(mListeners);
        Logging.show("App Preferences is created!");
    }

    @Override
    public void addListener(@androidx.annotation.Nullable Listener listener) {
        mListeners.add(listener);
    }

    @Override
    public void removeListener(@androidx.annotation.Nullable Listener listener) {
        mListeners.remove(listener);
    }

    @Override
    public void setDarkModeTheme(DarkMode mode) {
        mPreferences.edit().putString(PREF_DARK_THEME, mode.name()).apply();
    }

    @Override
    public DarkMode getDarkModeTheme() {
        try{
            return DarkMode.valueOf(mPreferences.getString(PREF_DARK_THEME, DarkMode.SYSTEM.name()));
        }catch(ClassCastException e){
            mPreferences.edit().putString(PREF_DARK_THEME, DarkMode.SYSTEM.name()).apply();
            return DarkMode.SYSTEM;
        }
    }

    @Override
    public void setFcmToken(String token) {
        mPreferences.edit().putString(Const.KEY_PREF_FCM_TOKEN, token).apply();
    }

    @Override
    public Optional<String> getFcmToken() {
        var token = mPreferences.getString(Const.KEY_PREF_FCM_TOKEN, null);
        return Optional.ofNullable(token);
    }

    @Override
    public void setNotificationStatus(Boolean isTurnOn) {
        mPreferences.edit().putBoolean(Const.KEY_PREF_NOTIFICATION_STATUS, isTurnOn).apply();
    }

    @NonNull
    @Override
    public Boolean getNotificationStatus() {
        return mPreferences.getBoolean(Const.KEY_PREF_NOTIFICATION_STATUS, true);
    }
}

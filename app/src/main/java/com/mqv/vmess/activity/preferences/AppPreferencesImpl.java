package com.mqv.vmess.activity.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.mqv.vmess.util.Const;
import com.mqv.vmess.util.Logging;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.Nullable;

public class AppPreferencesImpl implements AppPreferences {
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

    @Override
    public void setUserAuthToken(String token) {
        mPreferences.edit().putString(Const.KEY_PREF_AUTH_USER_TOKEN, token).apply();
    }

    @Override
    public Optional<String> getUserAuthToken() {
        String authToken = mPreferences.getString(Const.KEY_PREF_AUTH_USER_TOKEN, null);
        return Optional.ofNullable(authToken);
    }

    @Override
    public void setUserAuthTokenExpiresTime(Long time) {
        mPreferences.edit().putLong(Const.KEY_PREF_AUTH_USER_TOKEN_EXPIRES_TIME, time).apply();
    }

    @Override
    public Long getUserAuthTokenExpiresTime() {
        return mPreferences.getLong(Const.KEY_PREF_AUTH_USER_TOKEN_EXPIRES_TIME, 0L);
    }

    @Override
    public void setMessageMediaSort(MessageMediaSort sort) {
        mPreferences.edit().putString(Const.KEY_PREF_MESSAGE_MEDIA_SORT, sort.name()).apply();
    }

    @Override
    public MessageMediaSort getMessageMediaSort() {
        try {
            String sortType = mPreferences.getString(Const.KEY_PREF_MESSAGE_MEDIA_SORT, MessageMediaSort.LATEST.name());
            return MessageMediaSort.valueOf(sortType);
        } catch (ClassCastException ignore) {
            setMessageMediaSort(MessageMediaSort.LATEST);
            return MessageMediaSort.LATEST;
        }
    }

    @Override
    public void setAccountId(String id) {
        mPreferences.edit().putString(Const.KEY_PREF_ACCOUNT_ID, id).apply();
    }

    @Override
    public Optional<String> getAccountId() {
        return Optional.ofNullable(mPreferences.getString(Const.KEY_PREF_ACCOUNT_ID, null));
    }

    @Override
    public void setRegistrationId(Integer id) {
        mPreferences.edit().putInt(Const.KEY_PREF_REGISTRATION_ID, id).apply();
    }

    @Override
    public Integer getRegistrationId() {
        return mPreferences.getInt(Const.KEY_PREF_REGISTRATION_ID, 0);
    }

    @Override
    public void setIdentityKey(String identityKey) {
        mPreferences.edit().putString(Const.KEY_PREF_IDENTITY_KEY, identityKey).apply();
    }

    @Override
    public Optional<String> getIdentityKey() {
        return Optional.ofNullable(mPreferences.getString(Const.KEY_PREF_IDENTITY_KEY, null));
    }

    @Override
    public Boolean isContainIdentityKey() {
        return getIdentityKey().isPresent();
    }

    @Override
    public void setIdentityPrivateKey(String key) {
        mPreferences.edit().putString(Const.KEY_PREF_PRIVATE_IDENTITY_KEY, key).apply();
    }

    @Override
    public Optional<String> getIdentityPrivateKey() {
        return Optional.ofNullable(mPreferences.getString(Const.KEY_PREF_PRIVATE_IDENTITY_KEY, null));
    }

    @Override
    public void setNextSignedPreKeyId(Integer value) {
        mPreferences.edit().putInt(Const.KEY_PREF_NEXT_SIGNED_PRE_KEY_ID, value).apply();
    }

    @Override
    public Integer getNextSignedPreKeyId() {
        return mPreferences.getInt(Const.KEY_PREF_NEXT_SIGNED_PRE_KEY_ID, new SecureRandom().nextInt(0xFFFFFF));
    }

    @Override
    public void setActiveSignedPreKeyId(Integer value) {
        mPreferences.edit().putInt(Const.KEY_PREF_ACTIVE_SIGNED_PRE_KEY_ID, value).apply();
    }

    @Override
    public Integer getActiveSignedPreKeyId() {
        return mPreferences.getInt(Const.KEY_PREF_ACTIVE_SIGNED_PRE_KEY_ID, -1);
    }

    @Override
    public void setSignedPreKeyRegistered(Boolean value) {
        mPreferences.edit().putBoolean(Const.KEY_PREF_SIGNED_PRE_KEY_REGISTERED, value).apply();
    }

    @Override
    public Boolean getSignedPreKeyRegistered() {
        return mPreferences.getBoolean(Const.KEY_PREF_SIGNED_PRE_KEY_REGISTERED, false);
    }

    @Override
    public void setSignedPreKeyFailureCount(Integer value) {
        mPreferences.edit().putInt(Const.KEY_PREF_SIGNED_PRE_KEY_FAILURE_COUNT, value).apply();
    }

    @Override
    public Integer getSignedPreKeyFailureCount() {
        return mPreferences.getInt(Const.KEY_PREF_SIGNED_PRE_KEY_FAILURE_COUNT, 0);
    }

    @Override
    public void setNextOneTimePreKeyId(Integer value) {
        mPreferences.edit().putInt(Const.KEY_PREF_NEXT_ONE_TIME_PRE_KEY_ID, value).apply();
    }

    @Override
    public Integer getNextOneTimePreKeyId() {
        return mPreferences.getInt(Const.KEY_PREF_NEXT_ONE_TIME_PRE_KEY_ID, new SecureRandom().nextInt(0xFFFFFF));
    }
}

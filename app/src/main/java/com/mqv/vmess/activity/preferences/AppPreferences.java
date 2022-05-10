package com.mqv.vmess.activity.preferences;

import java.util.Optional;

import javax.annotation.Nullable;

/**
 * This interface provides single point of entry for access to all application
 * preferences and allows clients to subscribe for specific configuration
 * changes.
 */
public interface AppPreferences {
    /**
     * Preferences listener. Callbacks should be invoked on main thread.
     *
     * Maintainers should extend this interface with callbacks for specific
     * events.
     */
    interface Listener{
        default void onDarkThemeModeChanged(DarkMode mode){

        }
    }

    void addListener(@Nullable Listener listener);

    void removeListener(@Nullable Listener listener);

    void setDarkModeTheme(DarkMode mode);

    DarkMode getDarkModeTheme();

    void setFcmToken(String token);

    Optional<String> getFcmToken();

    void setNotificationStatus(Boolean isTurnOn);

    Boolean getNotificationStatus();

    void setUserAuthToken(String token);

    Optional<String> getUserAuthToken();

    void setUserAuthTokenExpiresTime(Long time);

    Long getUserAuthTokenExpiresTime();

    void setMessageMediaSort(MessageMediaSort sort);

    MessageMediaSort getMessageMediaSort();
}

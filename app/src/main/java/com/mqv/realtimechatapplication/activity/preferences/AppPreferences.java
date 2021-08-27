package com.mqv.realtimechatapplication.activity.preferences;

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
}

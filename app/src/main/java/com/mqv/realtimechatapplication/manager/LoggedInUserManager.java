package com.mqv.realtimechatapplication.manager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseUser;
import com.mqv.realtimechatapplication.network.model.User;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LoggedInUserManager {
    private static final Object LOCK = new Object();
    private static LoggedInUserManager instance;
    @Nullable
    private User user;
    private final List<LoggedInUserListener> loggedInUserUpdatedListeners;

    public static LoggedInUserManager getInstance() {
        synchronized (LOCK) {
            if (instance == null) {
                instance = new LoggedInUserManager();
            }
            return instance;
        }
    }

    private LoggedInUserManager() {
        loggedInUserUpdatedListeners = new CopyOnWriteArrayList<>();
    }

    public interface LoggedInUserListener {
        void onLoggedInUserChanged(User user);
    }

    public void addListener(LoggedInUserListener listener) {
        loggedInUserUpdatedListeners.add(listener);
    }

    public void removeListener(LoggedInUserListener listener) {
        loggedInUserUpdatedListeners.remove(listener);
    }

    public void notifyUserChanged(User user) {
        for (var listener : loggedInUserUpdatedListeners) {
            listener.onLoggedInUserChanged(user);
        }
    }

    public void setLoggedInUser(@Nullable User user) {
        this.user = user;

        notifyUserChanged(user);
    }

    public User getLoggedInUser() {
        return user;
    }

    public void signOut() {
        this.user = null;
    }

    public User parseFirebaseUser(@NonNull FirebaseUser user) {
        return new User.Builder()
                       .setUid(user.getUid())
                       .setPhotoUrl(user.getPhotoUrl() == null ? null : user.getPhotoUrl().toString())
                       .setDisplayName(user.getDisplayName())
                       .create();
    }
}

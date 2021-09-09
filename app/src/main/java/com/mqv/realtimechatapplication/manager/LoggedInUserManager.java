package com.mqv.realtimechatapplication.manager;

import androidx.annotation.Nullable;

import com.mqv.realtimechatapplication.network.model.User;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LoggedInUserManager {
    private static final Object LOCK = new Object();
    private static LoggedInUserManager instance;
    @Nullable
    private User user;
    private final List<LoggedInUserUpdatedListener> loggedInUserUpdatedListeners;

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

    public interface LoggedInUserUpdatedListener {
        void onLoggedInUserUpdated(User user);
    }

    public void addListener(LoggedInUserUpdatedListener listener) {
        loggedInUserUpdatedListeners.add(listener);
    }

    public void removeListener(LoggedInUserUpdatedListener listener) {
        loggedInUserUpdatedListeners.remove(listener);
    }

    public void notifyUserUpdated(User user) {
        for (var listener : loggedInUserUpdatedListeners){
            listener.onLoggedInUserUpdated(user);
        }
    }

    public void setLoggedInUser(@Nullable User user){
        this.user = user;
    }

    public User getLoggedInUser(){
        return user;
    }
}

package com.mqv.realtimechatapplication.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
@Entity(tableName = "logged_in_user")
public class LoggedInUser {
    @PrimaryKey
    @NonNull
    private String userId;
    @ColumnInfo(name = "display_name")
    private String displayName;

    public LoggedInUser(@NonNull String userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
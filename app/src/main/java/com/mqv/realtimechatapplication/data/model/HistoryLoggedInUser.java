package com.mqv.realtimechatapplication.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history_logged_in_user")
public class HistoryLoggedInUser {
    @PrimaryKey
    @NonNull
    private String uid;

    @ColumnInfo(name = "display_name")
    private String displayName;

    @ColumnInfo(name = "photo_url")
    private String photoUrl;
    private SignInProvider provider;

    @Nullable
    private String email;

    @Nullable
    @ColumnInfo(name = "phone_number")
    private String phoneNumber;

    @ColumnInfo(name = "is_login")
    private Boolean isLogin;

    public HistoryLoggedInUser(@NonNull String uid, String displayName,
                               String photoUrl, SignInProvider provider,
                               @Nullable String email, @Nullable String phoneNumber, Boolean isLogin) {
        this.uid = uid;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.provider = provider;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.isLogin = isLogin;
    }

    @NonNull
    public String getUid() {
        return uid;
    }

    public void setUid(@NonNull String uid) {
        this.uid = uid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public SignInProvider getProvider() {
        return provider;
    }

    public void setProvider(SignInProvider provider) {
        this.provider = provider;
    }

    public Boolean getLogin() {
        return isLogin;
    }

    public void setLogin(Boolean login) {
        isLogin = login;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    public void setEmail(@Nullable String email) {
        this.email = email;
    }

    @Nullable
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(@Nullable String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public static class Builder{
        private String uid;
        private String displayName;
        private String photoUrl;
        private SignInProvider provider;
        private String email;
        private String phoneNumber;
        private Boolean isLogin;

        public Builder(){
        }

        public Builder setUid(String uid) {
            this.uid = uid;
            return this;
        }

        public Builder setDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder setPhotoUrl(String photoUrl) {
            this.photoUrl = photoUrl;
            return this;
        }

        public Builder setProvider(SignInProvider provider) {
            this.provider = provider;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder setLogin(Boolean login) {
            isLogin = login;
            return this;
        }

        public HistoryLoggedInUser build(){
            return new HistoryLoggedInUser(
                    uid,
                    displayName,
                    photoUrl,
                    provider,
                    email,
                    phoneNumber,
                    isLogin
            );
        }
    }
}

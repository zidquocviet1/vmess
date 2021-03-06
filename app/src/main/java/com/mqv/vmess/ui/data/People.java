package com.mqv.vmess.ui.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;
import com.mqv.vmess.network.model.User;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity(tableName = "people")
public class People implements Parcelable {
    @PrimaryKey
    @NonNull
    private String uid;
    @SerializedName(value = "display_name")
    private String displayName;
    private String biographic;
    @SerializedName(value = "photo_url")
    private String photoUrl;
    @SerializedName(value = "user_connect_name")
    private String username;
    @ColumnInfo(name = "is_friend")
    private Boolean isFriend;
    @SerializedName(value = "accessed_date")
    private LocalDateTime accessedDate;

    public static final People NOT_FOUND = new People("-1", "", "Application User", null, "", false, null);

    @Ignore
    public People(@NonNull String uid) {
        this.uid = uid;
    }

    public People(@NonNull String uid, String biographic, String displayName, String photoUrl, String username, Boolean isFriend, LocalDateTime accessedDate) {
        this.uid = uid;
        this.displayName = displayName;
        this.biographic = biographic;
        this.photoUrl = photoUrl;
        this.username = username;
        this.isFriend = isFriend;
        this.accessedDate = accessedDate;
    }

    public static People mapFromUser(User user, Boolean isFriend) {
        return new People(user.getUid(), user.getBiographic(), user.getDisplayName(), user.getPhotoUrl(), user.getUsername(), isFriend, user.getAccessedDate());
    }

    protected People(Parcel in) {
        uid = in.readString();
        displayName = in.readString();
        photoUrl = in.readString();
        username = in.readString();
        accessedDate = (LocalDateTime) in.readSerializable();
    }

    public static final Creator<People> CREATOR = new Creator<People>() {
        @Override
        public People createFromParcel(Parcel in) {
            return new People(in);
        }

        @Override
        public People[] newArray(int size) {
            return new People[size];
        }
    };

    public void copy(People that) {
        this.displayName = that.displayName;
        this.photoUrl = that.photoUrl;
        this.username = that.username;
        this.accessedDate = that.accessedDate;
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

    public String getBiographic() {
        return biographic;
    }

    public void setBiographic(String biographic) {
        this.biographic = biographic;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Boolean getFriend() {
        return isFriend;
    }

    public void setFriend(Boolean friend) {
        isFriend = friend;
    }

    public LocalDateTime getAccessedDate() {
        return accessedDate;
    }

    public void setAccessedDate(LocalDateTime accessedDate) {
        this.accessedDate = accessedDate;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeString(displayName);
        dest.writeString(photoUrl);
        dest.writeString(username);
        dest.writeSerializable(accessedDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        var that = (People) o;

        return Objects.equals(uid, that.uid) &&
                Objects.equals(photoUrl, that.photoUrl) &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(username, that.username) &&
                Objects.equals(accessedDate, that.accessedDate);
    }
}

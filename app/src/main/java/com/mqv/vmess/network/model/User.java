package com.mqv.vmess.network.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;
import com.mqv.vmess.network.model.type.Gender;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity(tableName = "user")
public class User implements Parcelable {
    @PrimaryKey
    @NonNull
    private String uid;
    private String biographic;

    @SerializedName(value = "display_name")
    @ColumnInfo(name = "display_name")
    private String displayName;

    @SerializedName(value = "photo_url")
    @ColumnInfo(name = "photo_url")
    private String photoUrl;
    private Gender gender;
    private LocalDateTime birthday;
    @SerializedName("created_date")
    private LocalDateTime createdDate;
    @SerializedName("modified_date")
    private LocalDateTime modifiedDate;
    @SerializedName("accessed_date")
    private LocalDateTime accessedDate;
    @SerializedName("social_links")
    private List<UserSocialLink> socialLinks;
    @ColumnInfo(name = "user_connect_name", defaultValue = "")
    @SerializedName(value = "user_connect_name")
    private String username;

    public User(@NonNull String uid, String biographic, String displayName, String photoUrl, Gender gender,
                LocalDateTime birthday, LocalDateTime createdDate,
                LocalDateTime modifiedDate, LocalDateTime accessedDate,
                List<UserSocialLink> socialLinks, String username) {
        this.uid = uid;
        this.biographic = biographic;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.gender = gender;
        this.birthday = birthday;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.accessedDate = accessedDate;
        this.socialLinks = socialLinks;
        this.username = username;
    }

    @Ignore
    public User(String id) {
        this.uid = id;
    }

    // This constructor will create a new instance with the same value for update user request body
    @Ignore
    public User(User another){
        this.uid = another.getUid();
        this.biographic = another.getBiographic();
        this.displayName = another.getDisplayName();
        this.photoUrl = another.getPhotoUrl();
        this.gender = another.getGender();
        this.birthday = another.getBirthday();
        this.createdDate = another.getCreatedDate();
        this.modifiedDate = another.getModifiedDate();
        this.accessedDate = another.getAccessedDate();
        this.socialLinks = another.getSocialLinks();
        this.username = another.getUsername();
    }

    protected User(Parcel in) {
        uid = in.readString();
        biographic = in.readString();
        displayName = in.readString();
        photoUrl = in.readString();
        gender = (Gender) in.readSerializable();
        birthday = (LocalDateTime) in.readSerializable();
        createdDate = (LocalDateTime) in.readSerializable();
        modifiedDate = (LocalDateTime) in.readSerializable();
        accessedDate = (LocalDateTime) in.readSerializable();
        socialLinks = in.createTypedArrayList(UserSocialLink.CREATOR);
        username = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @NonNull
    public String getUid() {
        return uid;
    }

    public void setUid(@NonNull String uid) {
        this.uid = uid;
    }

    public String getBiographic() {
        return biographic;
    }

    public void setBiographic(String biographic) {
        this.biographic = biographic;
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

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public LocalDateTime getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDateTime birthday) {
        this.birthday = birthday;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(LocalDateTime modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public LocalDateTime getAccessedDate() {
        return accessedDate;
    }

    public void setAccessedDate(LocalDateTime accessedDate) {
        this.accessedDate = accessedDate;
    }

    public List<UserSocialLink> getSocialLinks() {
        return socialLinks;
    }

    public void setSocialLinks(List<UserSocialLink> socialLinks) {
        this.socialLinks = socialLinks;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeString(biographic);
        dest.writeString(displayName);
        dest.writeString(photoUrl);
        dest.writeSerializable(gender);
        dest.writeSerializable(birthday);
        dest.writeSerializable(createdDate);
        dest.writeSerializable(modifiedDate);
        dest.writeSerializable(accessedDate);
        dest.writeTypedList(socialLinks);
        dest.writeString(username);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return uid.equals(user.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid);
    }

    public static class Builder {
        private String uid;
        private String biographic;
        private String displayName;
        private String photoUrl;
        private String username;
        private Gender gender;
        private LocalDateTime birthday;
        private LocalDateTime createdDate;
        private LocalDateTime modifiedDate;
        private LocalDateTime accessedDate;
        private List<UserSocialLink> socialLinks;

        public Builder() {
        }

        public Builder setUid(String uid) {
            this.uid = uid;
            return this;
        }

        public Builder setBiographic(String biographic) {
            this.biographic = biographic;
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

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setGender(Gender gender) {
            this.gender = gender;
            return this;
        }

        public Builder setBirthday(LocalDateTime birthday) {
            this.birthday = birthday;
            return this;
        }

        public Builder setCreatedDate(LocalDateTime createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public Builder setModifiedDate(LocalDateTime modifiedDate) {
            this.modifiedDate = modifiedDate;
            return this;
        }

        public Builder setAccessedDate(LocalDateTime accessedDate) {
            this.accessedDate = accessedDate;
            return this;
        }

        public Builder setSocialLinks(List<UserSocialLink> socialLinks) {
            this.socialLinks = socialLinks;
            return this;
        }

        public User create() {
            return new User(uid,
                            biographic,
                            displayName,
                            photoUrl,
                            gender,
                            birthday,
                            createdDate,
                            modifiedDate,
                            accessedDate,
                            socialLinks,
                            username);
        }
    }
}

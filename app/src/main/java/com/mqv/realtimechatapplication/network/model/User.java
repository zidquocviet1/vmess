package com.mqv.realtimechatapplication.network.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;
import java.util.List;

@Entity(tableName = "user")
public class User {
    @PrimaryKey
    @NonNull
    private String uid;
    private String biographic;
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

    public User(@NonNull String uid, String biographic, Gender gender,
                LocalDateTime birthday, LocalDateTime createdDate,
                LocalDateTime modifiedDate, LocalDateTime accessedDate, List<UserSocialLink> socialLinks) {
        this.uid = uid;
        this.biographic = biographic;
        this.gender = gender;
        this.birthday = birthday;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.accessedDate = accessedDate;
        this.socialLinks = socialLinks;
    }

    @Ignore
    public User(@NonNull String uid, String biographic, Gender gender, LocalDateTime birthday) {
        this.uid = uid;
        this.biographic = biographic;
        this.gender = gender;
        this.birthday = birthday;
    }

    // This constructor will create a new instance with the same value for update user request body
    @Ignore
    public User(User another){
        this.uid = another.getUid();
        this.biographic = another.getBiographic();
        this.gender = another.getGender();
        this.birthday = another.getBirthday();
        this.createdDate = another.getCreatedDate();
        this.modifiedDate = another.getModifiedDate();
        this.accessedDate = another.getAccessedDate();
        this.socialLinks = another.getSocialLinks();
    }

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
}

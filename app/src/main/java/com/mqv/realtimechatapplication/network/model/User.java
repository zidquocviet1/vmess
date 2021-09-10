package com.mqv.realtimechatapplication.network.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.LocalDateTime;

@Entity(tableName = "user")
public class User {
    @PrimaryKey
    @NonNull
    private String uid;
    private String biographic;
    private Gender gender;
    private LocalDateTime birthday;

    public User(@NonNull String uid, String biographic, Gender gender, LocalDateTime birthday) {
        this.uid = uid;
        this.biographic = biographic;
        this.gender = gender;
        this.birthday = birthday;
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
}

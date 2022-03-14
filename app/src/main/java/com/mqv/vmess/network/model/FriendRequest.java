package com.mqv.vmess.network.model;

import com.google.gson.annotations.SerializedName;
import com.mqv.vmess.network.model.type.FriendRequestStatus;

import java.time.LocalDateTime;
import java.util.Objects;

public class FriendRequest {
    private Long id;
    @SerializedName("sender_id")
    private String senderId;
    @SerializedName("receiver_id")
    private String receiverId;
    @SerializedName("photo_url")
    private String photoUrl;
    @SerializedName("display_name")
    private String displayName;
    private FriendRequestStatus status;
    @SerializedName("created_date")
    private LocalDateTime createdDate;

    public FriendRequest(Long id, String senderId, String receiverId, String photoUrl, String displayName, FriendRequestStatus status) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.photoUrl = photoUrl;
        this.displayName = displayName;
        this.status = status;
        this.createdDate = LocalDateTime.of(2021, 9, 25, 1, 10, 30);
    }

    public FriendRequest(String senderId, String receiverId, FriendRequestStatus status) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.status = status;
        this.createdDate = LocalDateTime.now();
    }

    public FriendRequest(String senderId, String receiverId) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.createdDate = LocalDateTime.now();
    }

    public FriendRequest(FriendRequest that) {
        this.id = that.getId();
        this.senderId = that.getSenderId();
        this.receiverId = that.getReceiverId();
        this.photoUrl = that.getPhotoUrl();
        this.displayName = that.getDisplayName();
        this.status = that.getStatus();
        this.createdDate = that.getCreatedDate();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public FriendRequestStatus getStatus() {
        return status;
    }

    public void setStatus(FriendRequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        FriendRequest that = (FriendRequest) o;

        return Objects.equals(id, that.id) &&
                Objects.equals(senderId, that.senderId) &&
                Objects.equals(receiverId, that.receiverId) &&
                Objects.equals(photoUrl, that.photoUrl) &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(createdDate, that.createdDate) &&
                status == that.status;
    }
}

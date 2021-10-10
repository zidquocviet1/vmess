package com.mqv.realtimechatapplication.network.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;
import com.mqv.realtimechatapplication.network.model.type.NotificationType;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class Notification implements Parcelable {
    @PrimaryKey
    private Long id;
    private String messageId;
    private String title;
    private String body;
    private NotificationType type;
    private Boolean hasRead;
    private String ownerId;
    private String agentId;
    private String agentImageUrl;
    @SerializedName("pushSent")
    private Boolean isPushSent;
    private LocalDateTime createdDate;
    private LocalDateTime accessedDate;

    public Notification(Long id, String messageId, String title,
                        String body, NotificationType type, Boolean hasRead,
                        String ownerId, String agentId, String agentImageUrl,
                        Boolean isPushSent, LocalDateTime createdDate, LocalDateTime accessedDate) {
        this.id = id;
        this.messageId = messageId;
        this.title = title;
        this.body = body;
        this.type = type;
        this.hasRead = hasRead;
        this.ownerId = ownerId;
        this.agentId = agentId;
        this.agentImageUrl = agentImageUrl;
        this.isPushSent = isPushSent;
        this.createdDate = createdDate;
        this.accessedDate = accessedDate;
    }

    protected Notification(Parcel in) {
        if (in.readByte() == 0) {
            id = null;
        } else {
            id = in.readLong();
        }
        messageId = in.readString();
        title = in.readString();
        body = in.readString();
        byte tmpHasRead = in.readByte();
        hasRead = tmpHasRead == 0 ? null : tmpHasRead == 1;
        ownerId = in.readString();
        agentId = in.readString();
        agentImageUrl = in.readString();
        byte tmpIsPushSent = in.readByte();
        isPushSent = tmpIsPushSent == 0 ? null : tmpIsPushSent == 1;
    }

    public static final Creator<Notification> CREATOR = new Creator<Notification>() {
        @Override
        public Notification createFromParcel(Parcel in) {
            return new Notification(in);
        }

        @Override
        public Notification[] newArray(int size) {
            return new Notification[size];
        }
    };

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public Boolean getHasRead() {
        return hasRead;
    }

    public void setHasRead(Boolean hasRead) {
        this.hasRead = hasRead;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentImageUrl() {
        return agentImageUrl;
    }

    public void setAgentImageUrl(String agentImageUrl) {
        this.agentImageUrl = agentImageUrl;
    }

    public Boolean getPushSent() {
        return isPushSent;
    }

    public void setPushSent(Boolean pushSent) {
        isPushSent = pushSent;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getAccessedDate() {
        return accessedDate;
    }

    public void setAccessedDate(LocalDateTime accessedDate) {
        this.accessedDate = accessedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(messageId, that.messageId) &&
                Objects.equals(title, that.title) &&
                Objects.equals(body, that.body) &&
                type == that.type &&
                Objects.equals(hasRead, that.hasRead) &&
                Objects.equals(ownerId, that.ownerId) &&
                Objects.equals(agentId, that.agentId) &&
                Objects.equals(agentImageUrl, that.agentImageUrl) &&
                Objects.equals(isPushSent, that.isPushSent) &&
                Objects.equals(createdDate, that.createdDate);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (id == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(id);
        }
        dest.writeString(messageId);
        dest.writeString(title);
        dest.writeString(body);
        dest.writeByte((byte) (hasRead == null ? 0 : hasRead ? 1 : 2));
        dest.writeString(ownerId);
        dest.writeString(agentId);
        dest.writeString(agentImageUrl);
        dest.writeByte((byte) (isPushSent == null ? 0 : isPushSent ? 1 : 2));
    }
}

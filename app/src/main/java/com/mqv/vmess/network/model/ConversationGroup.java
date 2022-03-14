package com.mqv.vmess.network.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

import java.time.LocalDateTime;
import java.util.Objects;

public class ConversationGroup implements Parcelable {
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    @ColumnInfo(name = "creator_id")
    private String creatorId;
    @ColumnInfo(name = "group_conversation_id")
    private String conversationId;
    @ColumnInfo(name = "admin_id")
    private String adminId;
    private String thumbnail;
    @ColumnInfo(name = "creation_timestamp")
    private LocalDateTime creationTimestamp;
    @ColumnInfo(name = "last_modified_by")
    private String lastModifiedBy;
    @ColumnInfo(name = "last_modified")
    private LocalDateTime lastModified;

    public ConversationGroup(@NonNull String id,
                             String name,
                             String creatorId,
                             String adminId,
                             String thumbnail,
                             LocalDateTime creationTimestamp,
                             String modifiedBy,
                             LocalDateTime lastModified) {
        this.id = id;
        this.name = name;
        this.creatorId = creatorId;
        this.adminId = adminId;
        this.thumbnail = thumbnail;
        this.creationTimestamp = creationTimestamp;
        this.lastModifiedBy = modifiedBy;
        this.lastModified = lastModified;
    }

    protected ConversationGroup(Parcel in) {
        id = in.readString();
        name = in.readString();
        creatorId = in.readString();
        conversationId = in.readString();
        adminId = in.readString();
        thumbnail = in.readString();
        lastModifiedBy = in.readString();
    }

    public static final Creator<ConversationGroup> CREATOR = new Creator<ConversationGroup>() {
        @Override
        public ConversationGroup createFromParcel(Parcel in) {
            return new ConversationGroup(in);
        }

        @Override
        public ConversationGroup[] newArray(int size) {
            return new ConversationGroup[size];
        }
    };

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public LocalDateTime getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(LocalDateTime creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationGroup that = (ConversationGroup) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(name);
        parcel.writeString(creatorId);
        parcel.writeString(conversationId);
        parcel.writeString(adminId);
        parcel.writeString(thumbnail);
        parcel.writeString(lastModifiedBy);
    }
}

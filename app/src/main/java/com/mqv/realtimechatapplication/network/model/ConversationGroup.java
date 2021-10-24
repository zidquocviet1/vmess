package com.mqv.realtimechatapplication.network.model;

import java.time.LocalDateTime;

public class ConversationGroup {
    private String id;
    private String name;
    private String creatorId;
    private String adminId;
    private String thumbnail;
    private LocalDateTime creationTimestamp;
    private String modifiedBy;
    private LocalDateTime lastModified;

    public ConversationGroup(String id,
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
        this.modifiedBy = modifiedBy;
        this.lastModified = lastModified;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }
}

package com.mqv.realtimechatapplication.network.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;
import com.mqv.realtimechatapplication.network.model.type.NotificationType;

import java.time.LocalDateTime;

@Entity
public class Notification {
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
}

package com.mqv.realtimechatapplication.network.model;

import androidx.annotation.Nullable;

import com.mqv.realtimechatapplication.util.MessageStatus;

import java.time.LocalDateTime;

public class Conversation {
    private Long id;
    private String title;
    private String lastMessage;
    private LocalDateTime createdAt;
    private MessageStatus status;
    private Integer type; // group or personal

    public Conversation(Long id, String title, String lastMessage, LocalDateTime createdAt, MessageStatus status) {
        this.id = id;
        this.title = title;
        this.lastMessage = lastMessage;
        this.createdAt = createdAt;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null || obj.getClass() != Conversation.class)
            return false;

        var o2 = (Conversation)obj;
        return o2.getId().equals(this.getId()) &&
                o2.getTitle().equals(this.getTitle()) &&
                o2.getLastMessage().equals(this.getLastMessage()) &&
                o2.getCreatedAt().equals(this.getCreatedAt()) &&
                o2.getStatus().equals(this.getStatus());
    }
}

package com.mqv.realtimechatapplication.network.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Ignore;

import com.google.firebase.firestore.PropertyName;
import com.google.gson.annotations.SerializedName;
import com.mqv.realtimechatapplication.network.model.type.MessageStatus;
import com.mqv.realtimechatapplication.network.model.type.MessageType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Chat implements Parcelable {
    private String id;

    @PropertyName("sender")
    @SerializedName("sender")
    private String senderId;

    @SerializedName("conversation")
    @PropertyName("conversation")
    private String conversationId;

    private String content;
    private LocalDateTime timestamp;

    @Ignore
    private Share share;

    @Ignore
    private List<Photo> photos;

    @Ignore
    private List<File> files;

    private List<String> seenBy;
    private MessageStatus status;
    private MessageType type;

    @SerializedName("unsent")
    @PropertyName("unsent")
    private Boolean isUnsent;

    public Chat() {
        // Default no-arguments constructor for Firestore
    }

    public Chat(String id,
                String senderId,
                String content,
                String conversationId,
                LocalDateTime timestamp,
                Share share,
                List<Photo> photos,
                List<File> files,
                List<String> seenBy,
                MessageStatus status,
                MessageType messageType,
                Boolean isUnsent) {
        this.id = id;
        this.senderId = senderId;
        this.content = content;
        this.conversationId = conversationId;
        this.timestamp = timestamp;
        this.share = share;
        this.photos = photos;
        this.files = files;
        this.seenBy = seenBy;
        this.status = status;
        this.type = messageType;
        this.isUnsent = isUnsent;
    }

    public Chat(String id,
                String senderId,
                String content,
                String conversationId,
                MessageType type) {
        this.id = id;
        this.senderId = senderId;
        this.content = content;
        this.conversationId = conversationId;
        this.timestamp = LocalDateTime.now();
        this.status = MessageStatus.SENDING;
        this.type = type;
        this.isUnsent = Boolean.FALSE;
    }

    protected Chat(Parcel in) {
        id = in.readString();
        senderId = in.readString();
        conversationId = in.readString();
        content = in.readString();
        timestamp = (LocalDateTime) in.readSerializable();
        if (seenBy == null)
            seenBy = new ArrayList<>();
        in.readStringList(seenBy);
        status = (MessageStatus) in.readSerializable();
        type = (MessageType) in.readSerializable();
        byte tmpIsUnsent = in.readByte();
        isUnsent = tmpIsUnsent == 0 ? null : tmpIsUnsent == 1;
    }

    public static final Creator<Chat> CREATOR = new Creator<Chat>() {
        @Override
        public Chat createFromParcel(Parcel in) {
            return new Chat(in);
        }

        @Override
        public Chat[] newArray(int size) {
            return new Chat[size];
        }
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean isUnsent() {
        return isUnsent;
    }

    public void setUnsent(Boolean unsent) {
        isUnsent = unsent;
    }

    public Share getShare() {
        return share;
    }

    public void setShare(Share share) {
        this.share = share;
    }

    public List<Photo> getPhotos() {
        return photos;
    }

    public void setPhotos(List<Photo> photos) {
        this.photos = photos;
    }

    public List<File> getFiles() {
        return files;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }

    public List<String> getSeenBy() {
        return seenBy;
    }

    public void setSeenBy(List<String> seenBy) {
        this.seenBy = seenBy;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(senderId);
        dest.writeString(conversationId);
        dest.writeString(content);
        dest.writeSerializable(timestamp);
        dest.writeStringList(seenBy);
        dest.writeSerializable(status);
        dest.writeSerializable(type);
        dest.writeByte((byte) (isUnsent == null ? 0 : isUnsent ? 1 : 2));
    }

    public static class Photo {
        private String uri;
        private LocalDateTime creationTimestamp;

        public Photo(String uri, LocalDateTime creationTimestamp) {
            this.uri = uri;
            this.creationTimestamp = creationTimestamp;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public LocalDateTime getCreationTimestamp() {
            return creationTimestamp;
        }

        public void setCreationTimestamp(LocalDateTime creationTimestamp) {
            this.creationTimestamp = creationTimestamp;
        }
    }

    public static class File {
        private String uri;
        private LocalDateTime creationTimestamp;

        public File(String uri, LocalDateTime creationTimestamp) {
            this.uri = uri;
            this.creationTimestamp = creationTimestamp;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public LocalDateTime getCreationTimestamp() {
            return creationTimestamp;
        }

        public void setCreationTimestamp(LocalDateTime creationTimestamp) {
            this.creationTimestamp = creationTimestamp;
        }
    }

    public static class Share {
        private String link;

        public Share(String link) {
            this.link = link;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }
    }
}

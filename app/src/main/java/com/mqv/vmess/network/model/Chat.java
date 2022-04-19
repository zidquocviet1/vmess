package com.mqv.vmess.network.model;

import static androidx.room.ForeignKey.CASCADE;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;
import com.mqv.vmess.network.model.type.MessageStatus;
import com.mqv.vmess.network.model.type.MessageType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity(tableName = "chat",
        foreignKeys = { @ForeignKey(entity = Conversation.class,
                                    parentColumns = "conversation_id",
                                    childColumns = "chat_conversation_id",
                                    onDelete = CASCADE)})
public class Chat implements Parcelable {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "chat_id")
    private String id;

    @SerializedName("sender")
    @ColumnInfo(name = "chat_sender_id")
    private String senderId;

    @SerializedName("conversation")
    @ColumnInfo(index = true, name = "chat_conversation_id")
    private String conversationId;

    @ColumnInfo(name = "chat_content")
    private String content;

    @ColumnInfo(name = "chat_timestamp")
    private LocalDateTime timestamp;

    private Share share;
    private List<Photo> photos;
    private List<File> files;
    private List<Video> videos;
    private List<String> seenBy;

    @ColumnInfo(name = "chat_status")
    private MessageStatus status;

    @ColumnInfo(name = "chat_type")
    private MessageType type;

    @ColumnInfo(name = "chat_is_unsent")
    @SerializedName("unsent")
    private Boolean isUnsent;

    @Ignore
    public Chat() {
        // Default no-arguments constructor for Firestore
        this.id = UUID.randomUUID().toString();
    }

    @Ignore
    public Chat(@NonNull String id,
                String senderId,
                String content,
                String conversationId,
                LocalDateTime timestamp,
                Share share,
                List<Photo> photos,
                List<File> files,
                List<Video> videos,
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
        this.videos = videos;
        this.seenBy = seenBy;
        this.status = status;
        this.type = messageType;
        this.isUnsent = isUnsent;
    }

    public Chat(@NonNull String id,
                String senderId,
                String conversationId,
                String content,
                LocalDateTime timestamp,
                List<String> seenBy,
                MessageStatus status,
                MessageType type,
                Boolean isUnsent) {
        this.id = id;
        this.senderId = senderId;
        this.conversationId = conversationId;
        this.content = content;
        this.timestamp = timestamp;
        this.seenBy = seenBy;
        this.status = status;
        this.type = type;
        this.isUnsent = isUnsent;
    }

    @Ignore
    public Chat(@NonNull String id,
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
        this.seenBy = new ArrayList<>();
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
        share = in.readParcelable(Share.class.getClassLoader());
        if (photos == null)
            photos = new ArrayList<>();
        in.readList(photos, Photo.class.getClassLoader());
        if (videos == null)
            videos = new ArrayList<>();
        in.readList(videos, Video.class.getClassLoader());
        if (files == null)
            files = new ArrayList<>();
        in.readList(files, File.class.getClassLoader());
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

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
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

    public List<Video> getVideos() {
        return videos;
    }

    public void setVideos(List<Video> videos) {
        this.videos = videos;
    }

    public List<String> getSeenBy() {
        return seenBy == null ? new ArrayList<>() : seenBy;
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
        dest.writeParcelable(share, flags);
        dest.writeList(photos);
        dest.writeList(videos);
        dest.writeList(files);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chat chat = (Chat) o;
        return id.equals(chat.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static class Media implements Parcelable {
        private String uri;
        private LocalDateTime creationTimestamp;

        public Media(String uri) {
            this.uri = uri;
            this.creationTimestamp = LocalDateTime.now();
        }

        protected Media(Parcel in) {
            uri = in.readString();
            creationTimestamp = (LocalDateTime) in.readSerializable();
        }

        public static final Creator<Media> CREATOR = new Creator<Media>() {
            @Override
            public Media createFromParcel(Parcel in) {
                return new Media(in);
            }

            @Override
            public Media[] newArray(int size) {
                return new Media[size];
            }
        };

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

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(uri);
            dest.writeSerializable(creationTimestamp);
        }
    }

    public static class Photo extends Media implements Parcelable {
        public Photo(String uri) {
            super(uri);
        }
    }

    public static class File extends Media implements Parcelable {
        public File(String uri) {
            super(uri);
        }
    }

    public static class Share implements Parcelable {
        private String link;

        public Share(String link) {
            this.link = link;
        }

        protected Share(Parcel in) {
            link = in.readString();
        }

        public static final Creator<Share> CREATOR = new Creator<Share>() {
            @Override
            public Share createFromParcel(Parcel in) {
                return new Share(in);
            }

            @Override
            public Share[] newArray(int size) {
                return new Share[size];
            }
        };

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(link);
        }
    }

    public static class Video extends Media implements Parcelable {
        private String thumbnail;

        public Video(String uri, String thumbnail) {
            super(uri);
            this.thumbnail = thumbnail;
        }

        protected Video(Parcel in) {
            super(in);
            thumbnail = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(thumbnail);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<Video> CREATOR = new Creator<Video>() {
            @Override
            public Video createFromParcel(Parcel in) {
                return new Video(in);
            }

            @Override
            public Video[] newArray(int size) {
                return new Video[size];
            }
        };

        public String getThumbnail() {
            return thumbnail;
        }

        public void setThumbnail(String thumbnail) {
            this.thumbnail = thumbnail;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String senderId;
        private String conversationId;
        private String content;
        private Share share;
        private MessageType type;
        private List<Photo> photos;
        private List<File> files;
        private List<Video> videos;

        private Builder() {
        }

        public Builder setSenderId(String senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder setConversationId(String conversationId) {
            this.conversationId = conversationId;
            return this;
        }

        public Builder setContent(String content) {
            this.content = content;
            return this;
        }

        public Builder withShare(Share share) {
            this.share = share;
            return this;
        }

        public Builder setType(MessageType type) {
            this.type = type;
            return this;
        }

        public Builder withPhoto(List<Photo> photos) {
            this.photos = photos;
            return this;
        }

        public Builder withFile(List<File> files) {
            this.files = files;
            return this;
        }

        public Builder withVideos(List<Video> videos) {
            this.videos = videos;
            return this;
        }

        public Chat create() {
            return new Chat(UUID.randomUUID().toString(),
                            senderId,
                            content,
                            conversationId,
                            LocalDateTime.now(),
                            share,
                            photos,
                            files,
                            videos,
                            Collections.emptyList(),
                            MessageStatus.SENDING,
                            type,
                            false);
        }
    }
}

package com.mqv.realtimechatapplication.network.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;
import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType;
import com.mqv.realtimechatapplication.network.model.type.ConversationType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity(tableName = "conversation")
public class Conversation implements Parcelable {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "conversation_id")
    private String id;
    @ColumnInfo(name = "conversation_participants_id")
    private List<User> participants;
    @Ignore
    private List<Chat> chats;
    @Ignore
    private ConversationGroup group;
    @ColumnInfo(name = "conversation_type")
    private ConversationType type;
    @ColumnInfo(name = "conversation_status")
    private ConversationStatusType status;
    @ColumnInfo(name = "conversation_creation_time")
    private LocalDateTime creationTime;
    @SerializedName("last_chat")
    @Ignore
    private Chat lastChat;

    public Conversation(@NonNull String id,
                        List<User> participants,
                        ConversationType type,
                        ConversationStatusType status,
                        LocalDateTime creationTime) {
        this.id = id;
        this.participants = participants;
        this.type = type;
        this.status = status;
        this.creationTime = creationTime;
    }

    @Ignore
    public Conversation(@NonNull String id,
                        List<User> participants,
                        List<Chat> chats,
                        ConversationType type,
                        ConversationStatusType status,
                        LocalDateTime creationTime) {
        this.id = id;
        this.participants = participants;
        this.chats = chats;
        this.type = type;
        this.status = status;
        this.creationTime = creationTime;
    }

    @Ignore
    public Conversation(@NonNull String id,
                        List<User> participants,
                        List<Chat> chats,
                        ConversationGroup group,
                        ConversationType type,
                        ConversationStatusType status,
                        LocalDateTime creationTime,
                        Chat lastChat) {
        this.id = id;
        this.participants = participants;
        this.chats = chats;
        this.group = group;
        this.type = type;
        this.status = status;
        this.creationTime = creationTime;
        this.lastChat = lastChat;
    }

    protected Conversation(Parcel in) {
        id = in.readString();
        participants = in.createTypedArrayList(User.CREATOR);
        chats = in.createTypedArrayList(Chat.CREATOR);
        type = (ConversationType) in.readSerializable();
        status = (ConversationStatusType) in.readSerializable();
        creationTime = (LocalDateTime) in.readSerializable();
        lastChat = in.readParcelable(Chat.class.getClassLoader());
    }

    public static final Creator<Conversation> CREATOR = new Creator<>() {
        @Override
        public Conversation createFromParcel(Parcel in) {
            return new Conversation(in);
        }

        @Override
        public Conversation[] newArray(int size) {
            return new Conversation[size];
        }
    };

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public List<User> getParticipants() {
        return participants;
    }

    public void setParticipants(List<User> participants) {
        this.participants = participants;
    }

    public List<Chat> getChats() {
        return chats;
    }

    public void setChats(List<Chat> chats) {
        this.chats = chats;
    }

    public ConversationGroup getGroup() {
        return group;
    }

    public void setGroup(ConversationGroup group) {
        this.group = group;
    }

    public ConversationType getType() {
        return type;
    }

    public void setType(ConversationType type) {
        this.type = type;
    }

    public ConversationStatusType getStatus() {
        return status;
    }

    public void setStatus(ConversationStatusType status) {
        this.status = status;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public Chat getLastChat() {
        return chats.get(chats.size() - 1);
    }

    public void setLastChat(Chat lastChat) {
        this.lastChat = lastChat;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeTypedList(participants);
        dest.writeTypedList(chats);
        dest.writeSerializable(type);
        dest.writeSerializable(status);
        dest.writeSerializable(creationTime);
        dest.writeParcelable(lastChat, flags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conversation that = (Conversation) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

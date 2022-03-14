package com.mqv.vmess.data.converter;

import androidx.room.TypeConverter;

import com.mqv.vmess.network.model.type.MessageType;

public class MessageTypeConverter {
    @TypeConverter
    public MessageType toMessageType(String value) {
        return value.equals("") ? null : Enum.valueOf(MessageType.class, value);
    }

    @TypeConverter
    public String fromMessageType(MessageType type) {
        return type == null ? "" : type.name();
    }
}

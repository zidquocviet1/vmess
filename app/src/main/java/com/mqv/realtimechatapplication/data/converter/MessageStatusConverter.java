package com.mqv.realtimechatapplication.data.converter;

import androidx.room.TypeConverter;

import com.mqv.realtimechatapplication.network.model.type.MessageStatus;

public class MessageStatusConverter {
    @TypeConverter
    public MessageStatus toMessageStatus(String value) {
        return value.equals("") ? null : Enum.valueOf(MessageStatus.class, value);
    }

    @TypeConverter
    public String fromMessageStatus(MessageStatus type) {
        return type == null ? "" : type.name();
    }
}

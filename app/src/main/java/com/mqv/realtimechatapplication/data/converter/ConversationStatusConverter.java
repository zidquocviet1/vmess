package com.mqv.realtimechatapplication.data.converter;

import androidx.room.TypeConverter;

import com.mqv.realtimechatapplication.network.model.type.ConversationStatusType;

public class ConversationStatusConverter {
    @TypeConverter
    public ConversationStatusType toConversationStatus(String value) {
        return Enum.valueOf(ConversationStatusType.class, value);
    }

    @TypeConverter
    public String fromConversationStatus(ConversationStatusType type) {
        return type.name();
    }
}

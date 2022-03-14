package com.mqv.vmess.data.converter;

import androidx.room.TypeConverter;

import com.mqv.vmess.network.model.type.ConversationType;

public class ConversationTypeConverter {
    @TypeConverter
    public ConversationType toConversationType(String value) {
        return Enum.valueOf(ConversationType.class, value);
    }

    @TypeConverter
    public String fromConversationType(ConversationType type) {
        return type.name();
    }
}

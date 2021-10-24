package com.mqv.realtimechatapplication.data.converter;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mqv.realtimechatapplication.network.adapter.LocalDateTimeAdapter;
import com.mqv.realtimechatapplication.network.model.Chat;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;

public class ConversationChatConverter {
    @TypeConverter
    public List<Chat> toChats(String value) {
        Type type = new TypeToken<List<Chat>>(){}.getType();

        Gson gson = getGson();

        return value == null ? null : gson.fromJson(value, type);
    }

    @TypeConverter
    public String fromChats(List<Chat> chats) {
        Type type = new TypeToken<List<Chat>>(){}.getType();

        Gson gson = getGson();

        return chats == null ? "" : gson.toJson(chats, type);
    }

    public Gson getGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }
}

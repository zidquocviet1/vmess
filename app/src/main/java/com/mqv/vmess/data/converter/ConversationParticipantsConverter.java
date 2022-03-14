package com.mqv.vmess.data.converter;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mqv.vmess.network.adapter.LocalDateTimeAdapter;
import com.mqv.vmess.network.model.User;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;

public class ConversationParticipantsConverter {
    @TypeConverter
    public List<User> toParticipants(String value) {
        Type type = new TypeToken<List<User>>(){}.getType();

        Gson gson = getGson();

        return value == null ? null : gson.fromJson(value, type);
    }

    @TypeConverter
    public String fromParticipants(List<User> participants) {
        Type type = new TypeToken<List<User>>(){}.getType();

        Gson gson = getGson();

        return participants == null ? "" : gson.toJson(participants, type);
    }

    public Gson getGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }
}

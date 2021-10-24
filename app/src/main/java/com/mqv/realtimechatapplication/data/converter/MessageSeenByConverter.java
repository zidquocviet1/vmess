package com.mqv.realtimechatapplication.data.converter;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class MessageSeenByConverter {
    @TypeConverter
    public List<String> toMessageSeenBy(String value) {
        var typeList = new TypeToken<List<String>>(){}.getType();

        return value.equals("") ? null : new Gson().fromJson(value, typeList);
    }

    @TypeConverter
    public String fromMessageSeenBy(List<String> links) {
        var typeList = new TypeToken<List<String>>(){}.getType();

        return links == null ? "" : new Gson().toJson(links, typeList);
    }
}

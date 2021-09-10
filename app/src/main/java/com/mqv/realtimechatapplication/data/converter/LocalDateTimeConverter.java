package com.mqv.realtimechatapplication.data.converter;

import androidx.room.TypeConverter;

import java.time.LocalDateTime;

public class LocalDateTimeConverter {
    @TypeConverter
    public LocalDateTime fromTimestamp(String value) {
        return value == null ? null : LocalDateTime.parse(value);
    }

    @TypeConverter
    public String dateToTimestamp(LocalDateTime date) {
        if (date == null) {
            return null;
        } else {
            return date.toString();
        }
    }
}

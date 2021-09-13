package com.mqv.realtimechatapplication.data.converter;

import androidx.room.TypeConverter;

import com.mqv.realtimechatapplication.network.model.Gender;

public class GenderConverter {
    @TypeConverter
    public int fromGender(Gender gender){
        return gender == null ? -1 : gender.getKey();
    }

    @TypeConverter
    public Gender toGender(int key){
        return key == -1 ? null : Gender.getGenderByKey(key);
    }
}

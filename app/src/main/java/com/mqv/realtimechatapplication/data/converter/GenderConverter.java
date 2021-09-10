package com.mqv.realtimechatapplication.data.converter;

import androidx.room.TypeConverter;

import com.mqv.realtimechatapplication.network.model.Gender;

public class GenderConverter {
    @TypeConverter
    public int fromGender(Gender gender){
        return gender.getKey();
    }

    @TypeConverter
    public Gender toGender(int key){
        return Gender.getGenderByKey(key);
    }
}

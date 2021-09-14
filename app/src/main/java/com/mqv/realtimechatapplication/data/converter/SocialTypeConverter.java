package com.mqv.realtimechatapplication.data.converter;

import androidx.room.TypeConverter;

import com.mqv.realtimechatapplication.network.model.SocialType;

public class SocialTypeConverter {
    @TypeConverter
    public SocialType toSocialType(int key) {
        return key == -1 ? null : SocialType.getGenderByKey(key);
    }

    @TypeConverter
    public int fromSocialType(SocialType type){
        return type == null ? -1 : type.getKey();
    }
}

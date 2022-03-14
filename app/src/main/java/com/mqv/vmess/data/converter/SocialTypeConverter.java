package com.mqv.vmess.data.converter;

import androidx.room.TypeConverter;

import com.mqv.vmess.network.model.type.SocialType;

public class SocialTypeConverter {
    @TypeConverter
    public SocialType toSocialType(int key) {
        return key == -1 ? null : SocialType.getSocialTypeByKey(key);
    }

    @TypeConverter
    public int fromSocialType(SocialType type){
        return type == null ? -1 : type.getKey();
    }
}

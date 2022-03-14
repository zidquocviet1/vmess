package com.mqv.vmess.data.converter;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mqv.vmess.network.model.UserSocialLink;

import java.util.List;

public class SocialLinksListConverter {
    @TypeConverter
    public List<UserSocialLink> toSocialLinkList(String value) {
        var typeList = new TypeToken<List<UserSocialLink>>(){}.getType();

        return value.equals("") ? null : new Gson().fromJson(value, typeList);
    }

    @TypeConverter
    public String fromSocialLinkList(List<UserSocialLink> links) {
        var typeList = new TypeToken<List<UserSocialLink>>(){}.getType();

        return links == null ? "" : new Gson().toJson(links, typeList);
    }
}

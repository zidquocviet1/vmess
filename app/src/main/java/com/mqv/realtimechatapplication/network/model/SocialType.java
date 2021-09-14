package com.mqv.realtimechatapplication.network.model;

import android.util.SparseArray;

import com.google.gson.annotations.SerializedName;

public enum SocialType {
    @SerializedName("instagram")
    INSTAGRAM("instagram", 1),
    @SerializedName("facebook")
    FACEBOOK("facebook", 2),
    @SerializedName("github")
    GITHUB("github", 3),
    @SerializedName("linkedin")
    LINKEDIN("linkedin", 4),
    @SerializedName("pinterest")
    PINTEREST("pinterest", 5),
    @SerializedName("soundcloud")
    SOUNDCLOUD("soundcloud", 6),
    @SerializedName("tiktok")
    TIKTOK("tiktok", 7),
    @SerializedName("tumblr")
    TUMBLR("tumblr", 8),
    @SerializedName("twitter")
    TWITTER("twitter", 9),
    @SerializedName("vine")
    VINE("vine", 10),
    @SerializedName("vk")
    VK("vk", 11);

    private final String value;
    private final int key;
    private static final SparseArray<SocialType> array = new SparseArray<>();
    static {
        array.put(1, INSTAGRAM);
        array.put(2, FACEBOOK);
        array.put(3, GITHUB);
        array.put(4, LINKEDIN);
        array.put(5, PINTEREST);
        array.put(6, SOUNDCLOUD);
        array.put(7, TIKTOK);
        array.put(8, TUMBLR);
        array.put(9, TWITTER);
        array.put(10, VINE);
        array.put(11, VK);
    }

    SocialType(String value, int key) {
        this.value = value;
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public int getKey(){
        return key;
    }

    public static SocialType getGenderByKey(int key){
        return array.get(key);
    }
}

package com.mqv.realtimechatapplication.network.model.type;

import android.util.SparseArray;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public enum SocialType {
    @SerializedName("instagram")
    INSTAGRAM("instagram", 1, "com.instagram.android", "https://instagram.com/_u/"),
    @SerializedName("facebook")
    FACEBOOK("facebook", 2, "com.facebook.katana", "https://www.facebook.com/"),
    @SerializedName("github")
    GITHUB("github", 3, "", "https://github.com/"),
    @SerializedName("linkedin")
    LINKEDIN("linkedin", 4, "com.linkedin.android", "https://www.linkedin.com/in/"),
    @SerializedName("pinterest")
    PINTEREST("pinterest", 5, "", "https://www.pinterest.com/"),
    @SerializedName("soundcloud")
    SOUNDCLOUD("soundcloud", 6, "", "https://soundcloud.com/"),
    @SerializedName("tiktok")
    TIKTOK("tiktok", 7, "com.zhiliaoapp.musically", "https://www.tiktok.com/"),
    @SerializedName("tumblr")
    TUMBLR("tumblr", 8, "", "https://www.tumblr.com/"),
    @SerializedName("twitter")
    TWITTER("twitter", 9, "com.twitter.android", "https://twitter.com/"),
    @SerializedName("vine")
    VINE("vine", 10, "", "https://vine.co/"),
    @SerializedName("vk")
    VK("vk", 11, "", "https://vk.com/");

    private final String value;
    private final String packageName;
    private final String url;
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

    SocialType(String value, int key, String packageName, String url) {
        this.value = value;
        this.key = key;
        this.packageName = packageName;
        this.url = url;
    }

    public String getValue() {
        return value;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getUrl() {
        return url;
    }

    public int getKey() {
        return key;
    }

    public static SocialType getSocialTypeByKey(int key) {
        return array.get(key);
    }

    public static ArrayList<SocialType> getSocialTypeAsArray() {
        var result = new ArrayList<SocialType>();

        for (int i = 1; i <= array.size(); i++) {
            result.add(array.get(i));
        }

        return result;
    }
}

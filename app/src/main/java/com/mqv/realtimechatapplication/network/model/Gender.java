package com.mqv.realtimechatapplication.network.model;

import com.google.gson.annotations.SerializedName;

public enum Gender {
    @SerializedName("male")
    MALE("Male", 1),
    @SerializedName("female")
    FEMALE("Female", 2),
    @SerializedName("nonBinary")
    NON_BINARY("Non-binary", 3),
    @SerializedName("transgender")
    TRANSGENDER("Transgender", 4),
    @SerializedName("intersex")
    INTERSEX("Intersex", 5),
    @SerializedName("preferNotToSay")
    PREFER_NOT_TO_SAY("Prefer not to say", 6);

    private final String value;
    private final int key;

    Gender(String value, int key) {
        this.value = value;
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public int getKey(){
        return key;
    }
}

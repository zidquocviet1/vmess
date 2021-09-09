package com.mqv.realtimechatapplication.network.model;

import com.google.gson.annotations.SerializedName;

public enum Gender {
    @SerializedName("male")
    MALE("Male"),
    @SerializedName("female")
    FEMALE("Female"),
    @SerializedName("nonBinary")
    NON_BINARY("Non-binary"),
    @SerializedName("transgender")
    TRANSGENDER("Transgender"),
    @SerializedName("intersex")
    INTERSEX("Intersex"),
    @SerializedName("preferNotToSay")
    PREFER_NOT_TO_SAY("Prefer not to say");

    private final String value;

    Gender(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

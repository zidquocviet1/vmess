package com.mqv.realtimechatapplication.network.model;

import com.google.gson.annotations.SerializedName;

public enum Gender {
    @SerializedName("male")
    MALE,
    @SerializedName("female")
    FEMALE,
    @SerializedName("nonBinary")
    NON_BINARY,
    @SerializedName("transgender")
    TRANSGENDER,
    @SerializedName("intersex")
    INTERSEX,
    @SerializedName("preferNotToSay")
    PREFER_NOT_TO_SAY
}
